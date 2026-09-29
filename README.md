# 🔥 Flint

### One spark. Endless stories.

[![Live Web App](https://img.shields.io/badge/🌐_Live_Web_App-Try_Flint_Online-6366f1?style=for-the-badge)](https://shubham230523.github.io/Flint/)

> **🌐 Live Web App**: [https://shubham230523.github.io/Flint/](https://shubham230523.github.io/Flint/)

Flint is a **Kotlin Multiplatform (KMP) & Compose Multiplatform** AI Content Operating System designed for creators. It transforms a single source idea or document into coordinated, platform-tailored content across LinkedIn, X (Twitter), Newsletters, YouTube, and Instagram Carousels.

```text
               Source Idea / Content
                         │
                         ▼
             [ Brand DNA Voice Engine ]
                         │
                         ▼
        [ AiTaskRouter (Gemini / OpenRouter) ]
                         │
                         ▼
  ┌──────────────────────┼──────────────────────┐
  ↓                      ↓                      ↓
LinkedIn Post        X Thread              Newsletter
  │                      │                      │
  └──────────────────────┴──────────────────────┘
                         │
                         ▼
        [ Cloud Firestore Dual-Sync Engine ]
                         │
                         ▼
        [ Content Library & Campaign Engine ]
```

---

## ✨ Live Core Features

### ⚡ Spark Workspace (Campaign Generator)
Turn any raw text, document summary, or topic idea into an entire multi-channel content campaign.
* **Target Channels**: LinkedIn Posts, X (Twitter) Threads, Newsletters, YouTube Scripts, and Instagram Carousels.
* **Tone Overrides**: Quickly override campaign tone on-the-fly (*Conversational*, *Punchy*, *Authoritative*, *Storytelling*, *Witty*).
* **AI Provider Routing**: Powered by `AiTaskRouter` with clean prompt formatting.

### 🧬 Brand DNA Studio (Creator DNA)
Train Flint AI on your exact brand voice so generated posts sound authentically like you instead of robotic AI.
* **Voice Controls**: *Preferred Tone*, *Writing Style*, *Target Audience*, *Content Niche*, *CTA Style*, and *Humor Level (1-5)*.
* **Firestore Persistence**: Persists user brand profiles under `users/{userId}/creator_dna/profile`.
* **Prompt Injection**: Injects saved Brand DNA voice tokens into all content generation requests.

### 📚 Content Library & Real-Time Editor
* **Filter & Search**: Categorize assets by platform (*LinkedIn*, *X*, *Newsletter*, *YouTube*, *Carousel*) and status (*Draft*, *Scheduled*, *Published*).
* **Live In-Place Editing**: Edit asset title, body, and status via modal dialog.
* **Firestore Dual-Sync**: Updating an asset automatically synchronizes both the standalone `content` collection and its parent embedded `campaigns` collection in Cloud Firestore.

### 📊 Campaign Engine & Analytics
* **Campaign Overview**: Displays source sparks and all associated multi-channel assets.
* **Capacity Quota Tracking**: Tracks AI generations and campaign limits via `QuotaCalculator` and `FreePlanLimits`.

### 🔑 Authentication & Theme Customization
* **Authentication**: Email/Password Sign-In/Up with show/hide password toggle.
* **Design System**: Material 3 Compose Multiplatform styling with toggleable ☀️ Light and 🌙 Dark themes.

---

## 🤖 AI Gateway Architecture

Flint is **provider-agnostic**, using a unified `AiRepository` interface:

```text
                  AiTaskRouter
                       │
       ┌───────────────┼───────────────┐
       ↓               ↓               ↓
 GeminiProvider   OpenRouterProvider  OllamaCloudProvider
(Gemini 2.5/1.5)  (Claude, Llama, etc)  (Local / Cloud)
```

* **Google Gemini Provider**: Direct API integration supporting Server-Sent Events (SSE) streaming.
* **OpenRouter Provider**: Gateway supporting Claude 3.5 Sonnet, Llama 3.1, Nemotron, and free tier models.
* **Ollama Cloud Provider**: REST endpoint provider for open-source AI models.

---

## ☁️ Firebase Architecture & KMP Storage Engine

Flint uses a hybrid architecture to support both mobile (Android/iOS) and Desktop JVM environments seamlessly:

```text
                        AuthRepository / Data Layer
                                     │
                 ┌───────────────────┴───────────────────┐
                 ↓                                       ↓
        Native Firebase SDKs                   Ktor REST API Engine
    (Android Play Services / iOS)           (Desktop JVM / Cross-Platform)
                 │                                       │
                 └───────────────────┬───────────────────┘
                                     │
                                     ▼
                            Firebase Services
               ┌─────────────────────┼─────────────────────┐
               ↓                     ↓                     ↓
       Firebase Auth          Cloud Firestore          Google Cloud
      (REST / Native)       (Bearer Token Auth)      (Security Rules)
```

| Service | Subcollection / Path | Description |
| :--- | :--- | :--- |
| **Firebase Auth** | `/identitytoolkit` | Manages user accounts and retrieves signed JWT Bearer ID Tokens. |
| **Campaigns** | `users/{userId}/campaigns/{id}` | Persists full multi-channel campaign records and embedded assets. |
| **Content Assets** | `users/{userId}/content/{id}` | Persists individual platform assets for the Content Library. |
| **Brand DNA** | `users/{userId}/creator_dna/profile` | Stores user-specific brand voice controls and tone preferences. |

---

## 🏗️ Technical Stack

| Area | Technology |
| :--- | :--- |
| **UI Framework** | Compose Multiplatform (Material 3) |
| **Shared Logic** | Kotlin Multiplatform (KMP) |
| **Architecture** | Clean Architecture + MVVM + Repository Pattern |
| **Target Platforms** | Desktop JVM (`:desktopApp`), Android (`:androidApp`), Web (`:webApp`) |
| **Asynchronous Engine** | Kotlin Coroutines + StateFlow / Flow |
| **Networking** | Ktor HTTP Client (Engine CIO) |
| **Serialization** | `kotlinx.serialization` |
| **AI Providers** | Google Gemini, OpenRouter, Ollama Cloud |
| **Backend Storage** | Firebase Cloud Firestore (Native + REST with Bearer Auth) |
| **Authentication** | Firebase Authentication (Native + REST Engine) |

---

## ⚙️ Local Configuration & Setup

### 1. Configure `local.properties` (Ignored by Git)
Create or update `local.properties` in the project root:

```properties
openrouter.api.key=your_openrouter_api_key
openrouter.model.name=anthropic/claude-3.5-sonnet
gemini.api.key=your_gemini_api_key

# Optional: Pre-fill test credentials for local testing
flint.test.email=your_email@example.com
flint.test.password=your_password
```

### 2. Configure Cloud Firestore Security Rules
In your [Firebase Console](https://console.firebase.google.com) → **Firestore Database** → **Rules**, publish the following rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null || true;
    }
  }
}
```

### 3. Run Application Targets
* **Desktop App**:
  ```bash
  ./gradlew :desktopApp:run
  ```
* **Android App**:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```

---

## 🔥 Vision & Philosophy

```text
One Spark ──► Brand DNA Alignment ──► Multi-Channel Generation ──► Real-Time Firestore Sync ──► Impact
```

### Flint
**One spark. Endless stories.**
