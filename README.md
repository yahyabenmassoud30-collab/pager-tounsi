# 📟 Pager Tounsi

A real-time group chat app for Android, built with **Kotlin** and **Jetpack Compose**.
Join a group with a code, chat live, and share photos, videos and voice messages, with push notifications when the app is closed.

> **Status:** v1.2.0 (build 7) · Personal project · Built March 2026

---

## ✨ Features

| Area | What it does |
|------|--------------|
| **Accounts** | Sign up with email, username and password; 6-digit email verification with a 60 s resend cooldown; persistent login |
| **Groups** | Join any group by code, list of joined groups, leave a group |
| **Real-time chat** | Live messaging over Socket.IO, message history loaded from the server, duplicate-message protection |
| **Media** | Photo upload, video upload with in-app player, recorded voice messages with a playback bubble |
| **Presence** | Group members dialog showing who is online and when they joined; "last seen" tracking |
| **Notifications** | Firebase Cloud Messaging push, a foreground service that listens for messages, and auto-restart after reboot |
| **In-app updates** | Checks a server-hosted `app-version.json` and offers the new APK |
| **Patch notes** | "What's New" dialog with the full version history |

## 🧱 Tech stack

- **Language / UI:** Kotlin, Jetpack Compose, Material 3, Navigation Compose
- **Networking:** Retrofit 2 + Gson, OkHttp 4, Socket.IO client 2.1
- **Local storage:** Jetpack DataStore (Preferences)
- **Images:** Coil
- **Push:** Firebase Cloud Messaging + Analytics
- **Media:** `MediaRecorder` / `MediaPlayer` (voice), `VideoView` (video)
- **Build:** Gradle (Groovy), AGP 8.x, minSdk 24, target/compile SDK 35, R8 minification on release

## 🗺️ Architecture

```
┌──────────────────────────────┐        REST (Retrofit)        ┌────────────────────┐
│  Android app (Compose UI)    │ ────────────────────────────► │  Backend server    │
│                              │                               │  (Node.js, port    │
│  Screens: Login · Signup ·   │ ◄──────────────────────────── │   3000, Socket.IO) │
│  Verify · Groups · Chat      │      Socket.IO (real time)    └─────────┬──────────┘
│                              │                                         │
│  GroupManagementService ◄────┼───────── FCM push ◄─────────────────────┘
└──────────────────────────────┘          (Firebase)
```

**Source layout** (`app/src/main/java/com/example/pagertounsi/`)

| File | Responsibility |
|------|----------------|
| `MainActivity.kt` | Entry point, navigation graph, auto-login, starts the socket and service |
| `LoginScreen`, `SignupScreen`, `VerificationScreen` | Auth flow |
| `groupScreen.kt` | Join / list / leave groups, update check |
| `ChatScreen.kt` | Chat UI, message list, uploads, voice recording |
| `VoiceMessageBubble`, `VideoMessageBubble` | Media playback in the chat |
| `GroupMembersDialog.kt` | Member list with online status |
| `ApiClient`, `ApiService`, `ApiModels` | Retrofit client, endpoints and data models |
| `SocketManager.kt` | Shared Socket.IO connection |
| `GroupManagementService.kt`, `BootReceiver.kt` | Background message listening and restart on boot |
| `FirebaseMessagingService.kt`, `NotificationService.kt` | Push and local notifications |
| `VersionChecker`, `UpdateDownloader`, `SimpleUpdateDialog` | In-app update flow |
| `PatchNotes.kt` | "What's New" dialog |

## 🔌 Backend contract

The server is **not** part of this repository. The app expects:

**REST endpoints:** `POST /signup`, `/login`, `/sendVerification`, `/verifyCode`, `/resendVerification`, `/checkVerification`, `/createGroup`, `/joinGroup`, `/leaveGroup`, `/getUserGroups`, `/getGroupMembers`, `/uploadPhoto`, `/uploadVoice`, `/uploadVideo`, `/updateFcmToken`, `/sendFcmMessage`; `GET /messages/{groupCode}`; `GET /app-version.json`

**Socket.IO events:** client emits `userOnline`, `joinGroup`, `sendMessage`, `updateLastSeen`, `leaveGroup`; server emits `newMessage`.

The server address is hardcoded in `ApiClient.kt`, `SocketManager.kt` and `VersionChecker.kt`. Change it to your own host before running.

## 🚀 Getting started

1. **Clone** the repo and open it in Android Studio.
2. **Add Firebase config:** create a Firebase project, register the package `com.example.pagertounsi`, and download `google-services.json` into `app/`. See `app/google-services.json.example` for the shape. This file is git-ignored.
3. **Point the app at your server** by updating the base URL in the three files above.
4. **Run** on a device or emulator (API 24+).

**Release signing (optional):** create `keystore.properties` in the project root (git-ignored):

```properties
storeFile=path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## 📜 Changelog

- **v1.2.0**: video sending and player
- **v1.1.0**: voice messages, group members and online status
- **v1.0**: improved notification handling, last-seen protocol
- **v0.9.9.8**: patch notes dialog
- **v0.9.9**: fixed message duplication, stability and error handling
- **v0.9.0**: push notifications, group list, leave group
- **v0.7**: photo upload, Socket.IO real-time chat

## 🛣️ Roadmap / known limitations

Things I plan to improve next:

- [ ] Move to HTTPS/WSS and token-based authentication (currently plain HTTP, username-only identification)
- [ ] Mask password fields and add password rules
- [ ] Turn off verbose HTTP body logging in release builds
- [ ] Add a "Create group" button (the endpoint already exists)
- [ ] Show patch notes only once per version
- [ ] Make the background service reliable on Android 12+ and avoid duplicate notifications
- [ ] Install updates in-app instead of opening the browser
- [ ] Add unit and UI tests; move server URLs into `BuildConfig`
- [ ] Document and open-source the backend

## 🔐 Security note

Never commit `google-services.json`, `keystore.properties`, keystores, or Firebase **service-account** keys. They are listed in `.gitignore`.
