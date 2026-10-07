# Synapse Social Platform & Admin Control Panel

Synapse is a next-generation online social-media ecosystem featuring two distinct applications connected to the same Firebase backend:

1. **Synapse User App (User APK)**: A mobile-first social network with news feed, stories, rich multimedia posts, comments, likes, shares, direct messaging, real-time presence, user search, profile customization, and notifications.
2. **Synapse Admin Control Panel (Admin APK)**: A dedicated administrative and moderation suite with real-time analytics, user lifecycle controls (activate, suspend, ban, promote/demote), post moderation, comment inspection, report resolution workflows, system broadcast announcements, immutable audit logs, and global platform switches.

Both applications communicate securely with the same Firebase project, utilizing Google Sign-In via Jetpack Credential Manager and zero-trust Firestore Security Rules.

---

## Architecture & Project Structure

The project is structured with Clean Architecture and modular separation of concerns:

```
├── app/                                 # Primary dual-launcher Android project (compiled for AI Studio preview)
│   ├── src/main/java/com/example/
│   │   ├── common/                      # Shared domain models, Firestore repositories, and M3 design system
│   │   │   ├── models/Models.kt         # UserProfile, Post, Comment, Story, Conversation, Report, AdminLog
│   │   │   ├── data/
│   │   │   │   ├── FirestoreProvider.kt # Custom database ID resolver
│   │   │   │   ├── AuthRepository.kt    # Credential Manager Google Sign-In & role resolution
│   │   │   │   ├── PostRepository.kt    # Feed, reactions, and comments
│   │   │   │   ├── ChatRepository.kt    # Direct messaging & conversation threads
│   │   │   │   ├── SocialRepository.kt  # Stories, notifications, and search
│   │   │   │   └── AdminRepository.kt   # System statistics, logs, and moderation operations
│   │   │   └── ui/components/           # Reusable UI components (Avatar, Badges, PostCard, Dialogs)
│   │   ├── user/                        # Synapse User App
│   │   │   ├── auth/                    # Google Sign-In and onboarding
│   │   │   ├── feed/                    # News feed with stories, pull-to-refresh, creation sheets
│   │   │   ├── stories/                 # Ephemeral stories bar and fullscreen viewer
│   │   │   ├── chat/                    # Real-time one-to-one messaging and conversations
│   │   │   ├── explore/                 # User search and suggested creators
│   │   │   ├── notifications/           # Activity feed and system announcements
│   │   │   ├── profile/                 # Profile details, custom covers, user posts
│   │   │   └── settings/                # Account settings, privacy, and preferences
│   │   ├── admin/                       # Synapse Admin Control Panel
│   │   │   ├── auth/                    # Role verification gate (rejects normal users)
│   │   │   ├── dashboard/               # Metrics cards, activity charts, and audit summary
│   │   │   ├── users/                   # User search, suspension, ban, and role modification
│   │   │   ├── posts/                   # Content inspection, hide, restore, and delete
│   │   │   ├── reports/                 # Community moderation reports & resolution notes
│   │   │   ├── notifications/           # System broadcasts to all or selected users
│   │   │   ├── logs/                    # Immutable administrative audit logs
│   │   │   └── settings/                # Global switches (maintenance mode, posting, messaging)
│   │   ├── MainActivity.kt              # Entry point for Synapse User App
│   │   └── AdminActivity.kt             # Standalone entry point for Synapse Admin Control Panel
│   └── src/main/res/                    # Custom adaptive icons, drawables, and strings
├── UserApp/                             # Standalone independent project for User APK
├── AdminApp/                            # Standalone independent project for Admin Control Panel APK
├── functions/                           # Firebase Cloud Functions for notifications & role claims
├── firestore.rules                      # Hardened zero-trust Firestore Security Rules
├── storage.rules                        # Firebase Storage security rules
├── firestore.indexes.json               # Firestore compound query indexes
└── firebase-blueprint.json              # Intermediate Representation schema specification
```

---

## 1. Firebase Configuration

Both applications connect to the same Firebase project:
- **Project ID**: `gen-lang-client-0092162695`
- **Firestore Database ID**: `ai-studio-android-nexussoc-8eca487d-5d07-4694-9ea4-1a43b5bae4d2`
- **Config file**: `google-services.json` placed in `/app/`, `/UserApp/app/`, and `/AdminApp/app/`

---

## 2. Firestore Database Schema

### `users/{uid}`
| Field | Type | Description |
|---|---|---|
| `uid` | string | Firebase Auth UID |
| `displayName` | string | Full display name |
| `username` | string | Unique handle |
| `email` | string | Registered Google account email |
| `photoURL` | string | Profile photo URL |
| `coverURL` | string | Profile cover photo URL |
| `bio` | string | Profile biography |
| `role` | string | `"user"`, `"moderator"`, or `"admin"` |
| `status` | string | `"active"`, `"suspended"`, or `"banned"` |
| `isOnline` | boolean | Real-time presence flag |
| `lastSeen` | timestamp | Timestamp of last activity |
| `createdAt` | timestamp | Account registration date |
| `updatedAt` | timestamp | Last profile update date |

### `posts/{postId}`
| Field | Type | Description |
|---|---|---|
| `postId` | string | Document identifier |
| `authorId` | string | Author UID |
| `authorName` | string | Author display name |
| `authorUsername`| string | Author handle |
| `authorPhotoURL`| string | Author avatar |
| `content` | string | Text body of the post |
| `mediaUrls` | string | Attachment image/media URL |
| `mediaType` | string | `"none"`, `"image"`, or `"video"` |
| `visibility` | string | `"public"`, `"followers"`, or `"hidden"` |
| `likeCount` | integer | Number of likes |
| `commentCount` | integer | Number of comments |
| `shareCount` | integer | Number of shares |
| `createdAt` | timestamp | Publication timestamp |
| `updatedAt` | timestamp | Last edit timestamp |

### `posts/{postId}/likes/{uid}`
- `uid`: string (Liking user UID)
- `createdAt`: timestamp

### `posts/{postId}/comments/{commentId}`
- `commentId`: string
- `authorId`: string
- `authorName`: string
- `authorPhotoURL`: string
- `content`: string
- `createdAt`: timestamp

### `conversations/{conversationId}`
- `conversationId`: string
- `participantIds`: array of strings
- `participantNames`: map
- `participantPhotos`: map
- `lastMessage`: string
- `lastSenderId`: string
- `updatedAt`: timestamp

### `conversations/{conversationId}/messages/{messageId}`
- `messageId`: string
- `senderId`: string
- `senderName`: string
- `text`: string
- `mediaUrl`: string
- `isRead`: boolean
- `timestamp`: timestamp

### `notifications/{notificationId}`
- `notificationId`: string
- `recipientId`: string
- `senderId`: string
- `senderName`: string
- `type`: `"like"`, `"comment"`, `"follow"`, `"message"`, `"announcement"`
- `targetId`: string
- `title`: string
- `message`: string
- `isRead`: boolean
- `createdAt`: timestamp

### `stories/{storyId}`
- `storyId`: string
- `authorId`: string
- `authorName`: string
- `authorPhotoURL`: string
- `mediaUrl`: string
- `caption`: string
- `createdAt`: timestamp
- `expiresAt`: timestamp (24 hours after creation)

### `reports/{reportId}`
- `reportId`: string
- `reporterId`: string
- `reporterName`: string
- `targetType`: `"user"`, `"post"`, `"comment"`
- `targetId`: string
- `targetSummary`: string
- `reason`: string
- `status`: `"pending"`, `"reviewed"`, `"resolved"`, `"rejected"`
- `moderatorNotes`: string
- `createdAt`: timestamp
- `updatedAt`: timestamp

### `adminLogs/{logId}`
- `logId`: string
- `adminUid`: string
- `adminName`: string
- `action`: string (e.g. `"BAN_USER"`, `"HIDE_POST"`, `"ROLE_CHANGED_TO_MODERATOR"`)
- `targetId`: string
- `targetType`: string
- `details`: string
- `timestamp`: timestamp

### `appSettings/{settingId}` (Document: `global`)
- `settingId`: `"global"`
- `maintenanceMode`: boolean
- `registrationEnabled`: boolean
- `postingEnabled`: boolean
- `messagingEnabled`: boolean
- `moderationEnabled`: boolean
- `updatedAt`: timestamp

---

## 3. Firestore Security Rules

Deployed in `firestore.rules`:
- Authenticated access required for all document reads and writes.
- Users can update only their own profile, and cannot alter their own `role` or `status`.
- Non-admin callers cannot create documents with `role: "admin"` or `role: "moderator"`.
- Suspended and banned accounts are rejected from creating posts, comments, stories, and messages.
- Hidden posts are accessible only to their author and moderators/admins.
- Only administrators can modify roles and global application settings.
- Only moderators and administrators can read/update reports and view audit logs.

---

## 4. Firebase Storage Rules

Deployed in `storage.rules`:
- Validates file content types (`image/*`, `video/*`).
- Enforces max upload sizes: 10MB for avatars, 15MB for cover photos, 50MB for posts.
- Restricts writes under `/profilePhotos/{uid}/` and `/posts/{uid}/` to the document owner.

---

## 5. Firebase Cloud Functions

Located in `/functions/index.js`:
- `onNewMessage`: Dispatches push notification and data payload via FCM to the conversation recipient.
- `onCommentCreated`: Sends in-app and push notification to the post author.
- `onReportCreated`: Logs new community reports for moderator queues.
- `setUserRole`: Callable Cloud Function with caller claim verification (`context.auth.token.role === 'admin'`) that sets Firebase Custom User Claims and updates Firestore.

---

## 6. Authentication Setup & Admin Verification

Both applications use **Google Sign-In via Jetpack Credential Manager** (`GetSignInWithGoogleOption`) with the project's OAuth Web Client ID (`1001795134980-vtub83vglohbjjppq49g936kb3dqujqa.apps.googleusercontent.com`).

### Zero-Trust Admin Role Verification:
1. When a user authenticates in `AdminActivity`, the app queries `/users/{uid}` in Firestore.
2. If `role` is not `"admin"` and not `"moderator"`, access is immediately rejected.
3. The user is presented with an "Access Denied" screen with account details and a sign-out button.
4. Client-side self-promotion is blocked by Firestore Security Rules.
5. In development/bootstrap mode, the first registered user is provisioned as an initial Super Admin.

---

## 7. Build Instructions

### Prerequisites
- JDK 17 or higher
- Android SDK Platform 36 (minSdk 24, targetSdk 36)
- Gradle 8.11+ / AGP 9.1+

### Build Debug APKs
To build the User App APK:
```bash
gradle :app:assembleDebug
# Output APK: app/build/outputs/apk/debug/app-debug.apk
```

To build standalone UserApp:
```bash
cd UserApp
gradle :app:assembleDebug
```

To build standalone AdminApp:
```bash
cd AdminApp
gradle :app:assembleDebug
```

### Build Release APKs
```bash
gradle :app:assembleRelease
# Output APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Build Signed Release AAB (Android App Bundle for Google Play)
```bash
gradle :app:bundleRelease
# Output AAB: app/build/outputs/bundle/release/app-release.aab
```

Sign using `apksigner`:
```bash
apksigner sign --ks upload-keystore.jks --ks-key-alias upload app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 8. Firebase Deployment

To deploy security rules, indexes, and Cloud Functions to your Firebase project:
```bash
firebase use gen-lang-client-0092162695
firebase deploy --only firestore:rules,firestore:indexes,storage,functions
```

---

## 9. Seed Data & Test Accounts

To test both applications:
1. **User Role**: Sign in with any Google account on the User App to explore the feed, create posts, share stories, and message other creators.
2. **Admin Role**: Sign in on the Admin Control Panel (`Synapse Admin`). The initial administrative account can immediately manage user accounts, review pending reports, toggle maintenance mode, and broadcast system notifications.
3. Use the integrated drawer switcher to switch between the User experience and Admin moderation controls within the streaming emulator.
