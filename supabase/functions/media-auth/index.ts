// ==============================================================================
// BUDDYS — Supabase Edge Function: media-auth (ZERO-TRUST SECURITY ARCHITECTURE)
// ==============================================================================

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import * as jose from "https://esm.sh/jose@5.2.0";

const FIREBASE_PROJECT_ID = "glass-chat-13f27";
const GOOGLE_JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const JWKS = jose.createRemoteJWKSet(new URL(GOOGLE_JWKS_URL));

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface MediaAuthRequest {
  firebaseToken: string;
  action: "upload" | "sign";
  path: string;
  contentType?: string;
  expiresIn?: number;
}

// ------------------------------------------------------------------------------
// SERVER-SIDE FIRESTORE AUTHORIZATION HELPERS
// ------------------------------------------------------------------------------

/**
 * Verifies caller is in chats/{chatId}.participants.
 * If the message document already exists, also verifies message.senderId === firebaseUid.
 */
async function verifyChatAuthorization(
  projectId: string,
  chatId: string,
  messageId: string,
  firebaseUid: string,
  firebaseToken: string,
  action: "upload" | "sign"
): Promise<{ authorized: boolean; reason?: string }> {
  try {
    // 1. Verify chat conversation membership
    const chatUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/chats/${encodeURIComponent(chatId)}`;
    const chatResp = await fetch(chatUrl, {
      method: "GET",
      headers: { Authorization: `Bearer ${firebaseToken}` },
    });

    if (chatResp.status === 403 || chatResp.status === 404) {
      return { authorized: false, reason: "Chat does not exist or user is not a participant." };
    }
    if (!chatResp.ok) {
      return { authorized: false, reason: `Firestore chat lookup failed with status ${chatResp.status}` };
    }

    const chatDoc = await chatResp.json();
    const rawParticipants = chatDoc?.fields?.participants?.arrayValue?.values || [];
    const participants: string[] = rawParticipants.map((v: any) => v.stringValue || "");

    if (!participants.includes(firebaseUid)) {
      return { authorized: false, reason: "User UID is not listed in chat participants." };
    }

    // 2. If uploading and message document exists, verify senderId
    if (action === "upload") {
      const msgUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}`;
      const msgResp = await fetch(msgUrl, {
        method: "GET",
        headers: { Authorization: `Bearer ${firebaseToken}` },
      });

      if (msgResp.ok) {
        const msgDoc = await msgResp.json();
        const existingSenderId = msgDoc?.fields?.senderId?.stringValue;
        if (existingSenderId && existingSenderId !== firebaseUid) {
          return { authorized: false, reason: "Forbidden: Cannot upload media to another user's message." };
        }
      }
    }

    return { authorized: true };
  } catch (err: any) {
    return { authorized: false, reason: `Chat authorization error: ${err.message}` };
  }
}

/**
 * Verifies story access rules:
 * - For upload: If story doc exists, story.userId === firebaseUid.
 * - For sign: Author has access; viewers have access only if story is not expired AND audience permits.
 */
async function verifyStoryAuthorization(
  projectId: string,
  storyId: string,
  firebaseUid: string,
  firebaseToken: string,
  action: "upload" | "sign"
): Promise<{ authorized: boolean; reason?: string }> {
  try {
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/stories/${encodeURIComponent(storyId)}`;
    const resp = await fetch(url, {
      method: "GET",
      headers: { Authorization: `Bearer ${firebaseToken}` },
    });

    if (action === "upload") {
      if (resp.ok) {
        const doc = await resp.json();
        const existingAuthor = doc?.fields?.userId?.stringValue;
        if (existingAuthor && existingAuthor !== firebaseUid) {
          return { authorized: false, reason: "Forbidden: Cannot upload media to another user's story ID." };
        }
      }
      return { authorized: true };
    }

    // action === "sign"
    if (resp.status === 403 || resp.status === 404) {
      return { authorized: false, reason: "Story does not exist or access is forbidden." };
    }
    if (!resp.ok) {
      return { authorized: false, reason: `Firestore returned status ${resp.status}` };
    }

    const doc = await resp.json();
    const authorUid = doc?.fields?.userId?.stringValue || "";

    // Author always has access
    if (authorUid === firebaseUid) {
      return { authorized: true };
    }

    // Check expiration
    const expiresAtStr = doc?.fields?.expiresAt?.timestampValue;
    if (expiresAtStr) {
      const expiresAt = new Date(expiresAtStr).getTime();
      if (expiresAt < Date.now()) {
        return { authorized: false, reason: "Story has expired." };
      }
    }

    // Check audience rules
    const audience = doc?.fields?.audience?.stringValue || "EVERYONE";
    if (audience === "CLOSE_FRIENDS") {
      const rawCloseFriends = doc?.fields?.closeFriends?.arrayValue?.values || [];
      const closeFriends: string[] = rawCloseFriends.map((v: any) => v.stringValue || "");
      if (!closeFriends.includes(firebaseUid)) {
        return { authorized: false, reason: "Forbidden: User is not in Close Friends audience." };
      }
    }

    return { authorized: true };
  } catch (err: any) {
    return { authorized: false, reason: `Story authorization error: ${err.message}` };
  }
}

// ------------------------------------------------------------------------------
// MAIN HANDLER
// ------------------------------------------------------------------------------

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey);

    const body: MediaAuthRequest = await req.json();
    const { firebaseToken, action, path, contentType = "image/jpeg", expiresIn = 604800 } = body;

    if (!firebaseToken || !action || !path) {
      return new Response(
        JSON.stringify({ error: "Missing required parameters: firebaseToken, action, path" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (action !== "upload" && action !== "sign") {
      return new Response(
        JSON.stringify({ error: `Unsupported action: ${action}` }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Path Sanitization & Traversal Prevention
    const cleanPath = path.trim().replace(/^\/+/, "");
    const pathParts = cleanPath.split("/");

    if (
      pathParts.length < 2 ||
      pathParts.length > 4 ||
      pathParts.some((part) => !part || part === ".." || part === "." || /[^a-zA-Z0-9_\-\.]/.test(part))
    ) {
      return new Response(
        JSON.stringify({ error: "Invalid path format or illegal path characters." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Cryptographic Token Verification (Google JWKS)
    let firebaseUid: string;
    try {
      const { payload } = await jose.jwtVerify(firebaseToken, JWKS, {
        issuer: `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`,
        audience: FIREBASE_PROJECT_ID,
      });
      firebaseUid = payload.sub as string;
      if (!firebaseUid || typeof firebaseUid !== "string") {
        throw new Error("Missing valid subject (UID) in token payload");
      }
    } catch (tokenErr: any) {
      return new Response(
        JSON.stringify({ error: `Invalid or expired Firebase token: ${tokenErr.message}` }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Domain-Level Authorization Verification
    const rootFolder = pathParts[0];

    // A. Profile Pictures: profile_pictures/{targetUid}/profile.jpg
    if (rootFolder === "profile_pictures") {
      if (pathParts.length !== 3 || pathParts[2] !== "profile.jpg") {
        return new Response(
          JSON.stringify({ error: "Invalid profile picture path. Expected: profile_pictures/{uid}/profile.jpg" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const targetUid = pathParts[1];
      if (action === "upload") {
        if (targetUid !== firebaseUid) {
          return new Response(
            JSON.stringify({ error: "Forbidden: You can only upload your own profile picture." }),
            { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
      } else if (action === "sign") {
        if (!targetUid || targetUid.length < 5) {
          return new Response(
            JSON.stringify({ error: "Invalid target user UID." }),
            { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
      }
    }

    // B. Chat Media & Voice: chat_media/{chatId}/{messageId}/{filename}, voice_messages/{chatId}/{messageId}/{filename}
    else if (rootFolder === "chat_media" || rootFolder === "voice_messages") {
      if (pathParts.length !== 4) {
        return new Response(
          JSON.stringify({ error: `Invalid ${rootFolder} path. Expected: ${rootFolder}/{chatId}/{messageId}/{filename}` }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const chatId = pathParts[1];
      const messageId = pathParts[2];
      const filename = pathParts[3];

      // Server-side verification of conversation membership and message ownership
      const chatAuth = await verifyChatAuthorization(FIREBASE_PROJECT_ID, chatId, messageId, firebaseUid, firebaseToken, action);
      if (!chatAuth.authorized) {
        return new Response(
          JSON.stringify({ error: `Forbidden: ${chatAuth.reason}` }),
          { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      // For uploads: filename must also start with caller's verified UID
      if (action === "upload") {
        if (!filename.startsWith(`${firebaseUid}_`)) {
          return new Response(
            JSON.stringify({ error: "Forbidden: Media upload filename must start with your Firebase UID." }),
            { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
      }
    }

    // C. 24-Hour Stories: stories/{storyId}/{filename}
    else if (rootFolder === "stories") {
      if (pathParts.length !== 3) {
        return new Response(
          JSON.stringify({ error: "Invalid story path. Expected: stories/{storyId}/{filename}" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const storyId = pathParts[1];
      const filename = pathParts[2];

      // Server-side verification of story ownership / audience rules
      const storyAuth = await verifyStoryAuthorization(FIREBASE_PROJECT_ID, storyId, firebaseUid, firebaseToken, action);
      if (!storyAuth.authorized) {
        return new Response(
          JSON.stringify({ error: `Forbidden: ${storyAuth.reason}` }),
          { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (action === "upload") {
        if (!filename.startsWith(`${firebaseUid}_`)) {
          return new Response(
            JSON.stringify({ error: "Forbidden: Story filename must start with your Firebase UID." }),
            { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
      }
    }

    // D. Unknown Root Folder
    else {
      return new Response(
        JSON.stringify({ error: `Forbidden: Unsupported root folder '${rootFolder}'.` }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Perform Supabase Storage Operation with Private 'buddys-media' Bucket
    const bucket = supabase.storage.from("buddys-media");

    if (action === "upload") {
      const { data, error } = await bucket.createSignedUploadUrl(cleanPath, { upsert: true });
      if (error) {
        return new Response(
          JSON.stringify({ error: `Storage error: ${error.message}` }),
          { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      return new Response(
        JSON.stringify({
          uploadUrl: data.signedUrl,
          token: data.token,
          path: cleanPath,
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    } else {
      // action === "sign"
      const maxExpiry = rootFolder === "stories" ? 86400 : 604800;
      const effectiveExpiry = Math.min(Math.max(Number(expiresIn) || 3600, 60), maxExpiry);

      const { data, error } = await bucket.createSignedUrl(cleanPath, effectiveExpiry);
      if (error) {
        return new Response(
          JSON.stringify({ error: `Storage error: ${error.message}` }),
          { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      return new Response(
        JSON.stringify({
          signedUrl: data.signedUrl,
          path: cleanPath,
          expiresIn: effectiveExpiry,
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }
  } catch (err: any) {
    return new Response(
      JSON.stringify({ error: `Internal Server Error: ${err.message}` }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
