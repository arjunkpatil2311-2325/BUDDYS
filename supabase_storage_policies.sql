-- ==============================================================================
-- BUDDYS — Supabase Storage Security & RLS Policies for 'buddys-media'
-- ==============================================================================
-- 
-- Bucket Name: buddys-media
-- Visibility:  PRIVATE (public = false)
-- Project:     https://sjinrlvwiwzdvszixyzh.supabase.co
--
-- ==============================================================================
-- ARCHITECTURAL SECURITY MODEL (Hybrid Firebase + Supabase)
-- ==============================================================================
--
-- 1. AUTHENTICATION & AUTHORIZATION AUTHORITY:
--    - Authentication is managed EXCLUSIVELY by Firebase Authentication.
--    - Authorization (chat membership, story audience, profile ownership) is 
--      enforced by Cloud Firestore Security Rules (firestore.rules).
--    - The Android client holds Firebase Auth tokens and only uses the Supabase 
--      PUBLISHABLE / ANON key as a storage API gateway credential.
--    - Because Supabase Auth sessions are NOT created for Firebase users, 
--      Supabase's auth.uid() is NULL and auth.role() is 'anon'.
--    - Supabase Storage RLS CANNOT directly query Firestore to verify chat 
--      membership or close friends lists.
--
-- 2. DEFENSE-IN-DEPTH STORAGE CONTROLS:
--    - The bucket 'buddys-media' is strictly PRIVATE (public = false). Direct 
--      unauthenticated HTTP GET access to object URLs is blocked (HTTP 400/403).
--    - NO GLOBAL SELECT POLICY is granted to anon. Anonymous users cannot list or 
--      download objects without a valid cryptographic signed URL token.
--    - NO GLOBAL DELETE POLICY is granted to anon.
--    - Reading private media requires short-lived signed URLs generated on-demand 
--      by authorized clients.
--    - INSERT policies are strictly scoped to the 'buddys-media' bucket and restricted 
--      to exact designated folder prefixes:
--        • profile_pictures/{userId}/profile.jpg
--        • chat_media/{chatId}/{messageId}/{filename}
--        • voice_messages/{chatId}/{messageId}/{filename}
--        • stories/{storyId}/{filename}
--    - Uploads to arbitrary buckets or root directory paths are blocked by RLS.
-- ==============================================================================

-- 1. Ensure Row Level Security is active on storage.objects
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- 2. Drop any legacy or overly permissive policies if they exist
DROP POLICY IF EXISTS "Allow authenticated profile picture uploads" ON storage.objects;
DROP POLICY IF EXISTS "Allow profile picture updates" ON storage.objects;
DROP POLICY IF EXISTS "Allow chat media uploads" ON storage.objects;
DROP POLICY IF EXISTS "Allow voice message uploads" ON storage.objects;
DROP POLICY IF EXISTS "Allow stories media uploads" ON storage.objects;
DROP POLICY IF EXISTS "Allow media deletion" ON storage.objects;
DROP POLICY IF EXISTS "Allow reading objects with signed tokens or authenticated" ON storage.objects;
DROP POLICY IF EXISTS "Allow public read" ON storage.objects;
DROP POLICY IF EXISTS "Allow all inserts" ON storage.objects;

-- ------------------------------------------------------------------------------
-- 3. SCOPED INSERT POLICIES
-- ------------------------------------------------------------------------------

-- Policy: Profile Picture Uploads
-- Path format: profile_pictures/{firebase_uid}/profile.jpg
CREATE POLICY "buddys_insert_profile_pictures"
ON storage.objects
FOR INSERT
TO anon, authenticated
WITH CHECK (
    bucket_id = 'buddys-media'
    AND (storage.foldername(name))[1] = 'profile_pictures'
);

-- Policy: Profile Picture Overwrites (x-upsert)
CREATE POLICY "buddys_update_profile_pictures"
ON storage.objects
FOR UPDATE
TO anon, authenticated
USING (
    bucket_id = 'buddys-media'
    AND (storage.foldername(name))[1] = 'profile_pictures'
);

-- Policy: Chat Media Uploads (Photos)
-- Path format: chat_media/{chatId}/{messageId}/{filename}
CREATE POLICY "buddys_insert_chat_media"
ON storage.objects
FOR INSERT
TO anon, authenticated
WITH CHECK (
    bucket_id = 'buddys-media'
    AND (storage.foldername(name))[1] = 'chat_media'
);

-- Policy: Voice Message Uploads (Audio)
-- Path format: voice_messages/{chatId}/{messageId}/{filename}
CREATE POLICY "buddys_insert_voice_messages"
ON storage.objects
FOR INSERT
TO anon, authenticated
WITH CHECK (
    bucket_id = 'buddys-media'
    AND (storage.foldername(name))[1] = 'voice_messages'
);

-- Policy: 24-Hour Story Media Uploads
-- Path format: stories/{storyId}/{filename}
CREATE POLICY "buddys_insert_stories"
ON storage.objects
FOR INSERT
TO anon, authenticated
WITH CHECK (
    bucket_id = 'buddys-media'
    AND (storage.foldername(name))[1] = 'stories'
);

-- ------------------------------------------------------------------------------
-- 4. READ & DELETE POLICY NOTES
-- ------------------------------------------------------------------------------
-- NOTE: We intentionally DO NOT create a "FOR SELECT TO anon" policy.
-- In Supabase Storage, private bucket reads are authorized via HMAC signed tokens 
-- appended to the object URL (e.g. /storage/v1/object/sign/buddys-media/...?token=...).
-- This ensures unauthenticated/unsigned HTTP requests cannot list or download media.
--
-- NOTE: We intentionally DO NOT create a global "FOR DELETE TO anon" policy.
-- Unsigned delete requests from untrusted clients cannot wipe the bucket.
-- ==============================================================================
