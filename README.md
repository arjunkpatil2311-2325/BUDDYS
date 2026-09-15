# Buddies — Native Android + Firebase + Dark Glassmorphism

A native Android private messaging application built with **Kotlin**, **Jetpack Compose**, and **Firebase**, featuring a **dark glassmorphism design system** and a **15-minute temporary friend-pairing code system**.

---

## 🌟 Key Features

1. **Dark Glassmorphism Interface**
   - Deep-space dark canvas (`#0B0E14`) with ambient glowing nebula orbs (Cyan, Purple, Aurora Teal).
   - Frosted translucent glass cards (`GlassCard`, `GlassSurface`) with soft borders and inner glows.
   - Dynamic message bubbles (Vibrant gradient glass for sent messages, translucent deep glass for received messages).
   - Smooth animations and keyboard-friendly Compose layouts.

2. **Temporary Friend-Pairing Code System**
   - Mutual friendship established via 6-character cryptographically random codes (`AURA-XXXXXX`).
   - Codes expire automatically after **15 minutes** and are **single-use**.
   - Atomic Firestore transactions create mutual friendship records and the deterministic chat thread simultaneously.
   - Self-pairing and reused/expired codes are strictly prevented.

3. **Deterministic 1-to-1 Chat Channels**
   - Chat IDs are deterministically calculated using `chat_{sortedUidA}_{sortedUidB}`.
   - Guarantees zero duplicate conversations between the same two users.

4. **Real-Time Messaging & Scalable Read Receipts**
   - Real-time snapshot listeners for live conversation updates and messages.
   - Per-user `lastReadAt: { uidA: timestamp, uidB: timestamp }` tracking for scalable unread calculations.
   - Lifecycle-aware online presence tracking (`isOnline` & `lastSeen`).

5. **Strict Security Isolation & Rules**
   - `firestore.rules` enforces participant-only access to `/chats/{chatId}` and `/chats/{chatId}/messages/{messageId}`.
   - Spoofing `senderId` or altering another user's profile/friends is blocked at the database engine level.
   - Cloud Functions backend (`functions/index.js`) provided for server-validated code generation and redemption.

---

## 📂 Project Architecture

```
glass-chat-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aura/glasschat/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/           # User, Friend, PairingCode, Chat, Message
│   │   │   │   │   └── repository/      # AuthRepository, UserRepository, PairingRepository, ChatRepository
│   │   │   │   ├── lifecycle/           # AppPresenceManager (ProcessLifecycleOwner)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/      # GlassCard, GlassButton, GlassTextField, AvatarView, GlassTopBar
│   │   │   │   │   ├── navigation/      # Sealed Screen routes (Auth, Home, AddFriend, Chat, Profile)
│   │   │   │   │   ├── screens/         # AuthScreen, HomeScreen, AddFriendScreen, ChatScreen, ProfileScreen
│   │   │   │   │   ├── theme/           # Color, Type, Theme, GlassModifiers
│   │   │   │   │   └── viewmodel/       # AuthViewModel, HomeViewModel, PairingViewModel, ChatViewModel, ProfileViewModel
│   │   │   │   ├── util/                # ChatUtils (Deterministic ID, Code generation, Time formatting)
│   │   │   │   ├── GlassChatApplication.kt
│   │   │   │   └── MainActivity.kt
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/aura/glasschat/
│   │       ├── model/                   # PairingLogicTest, ReadReceiptTest
│   │       └── util/                    # ChatUtilsTest
│   ├── build.gradle.kts
│   └── google-services.json
├── functions/                           # Cloud Functions backend (index.js, package.json)
├── firestore.rules                      # Production Firestore Security Rules
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 🚀 Setup & Firebase Configuration

### Step 1: Firebase Project Setup
1. Go to [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an **Android App** with package name: `com.aura.glasschat`.
3. Download `google-services.json` and place it in the `app/` folder.
4. Enable **Authentication** (Email/Password provider).
5. Enable **Cloud Firestore** and deploy `firestore.rules`.
6. (Optional) Deploy Cloud Functions:
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

### Step 2: Build and Run
Open `glass-chat-android` in Android Studio or run via Gradle:
```bash
./gradlew assembleDebug
```

---

## 🛡️ Security Rules Verification Matrix

| Scenario | Rule Check | Expected Behavior |
| :--- | :--- | :--- |
| **User A reads their own profile** | `isOwner(userId)` | ✅ Allowed |
| **User A reads User B's profile** | `isAuthenticated()` | ✅ Public profile readable |
| **User A edits User B's profile** | `request.auth.uid == userId` | ❌ Denied (Permission Denied) |
| **User A & B chat** | `request.auth.uid in participants` | ✅ Allowed |
| **User C reads A & B's chat** | `request.auth.uid in participants` | ❌ Denied (Permission Denied) |
| **User C writes message to A & B's chat** | `isChatParticipant(chatId)` | ❌ Denied (Permission Denied) |
| **User A spoofs `senderId = "User B"`** | `request.resource.data.senderId == request.auth.uid` | ❌ Denied |
| **Reusing an expired/used pairing code** | `resource.data.used == false && expiresAt > request.time` | ❌ Denied |
