# 🔥 Flint

### One spark. Endless stories.

Flint is a **cross-platform AI Content Operating System** that helps creators turn one idea or source into multiple pieces of platform-ready content.

Instead of separately creating content for YouTube, Instagram, LinkedIn, X, newsletters, blogs, and other platforms, Flint uses AI to understand the original content and transform it into different formats.

```text
Idea / Content
      ↓
AI Understanding
      ↓
Content Strategy
      ↓
Generate
      ↓
Review & Edit
      ↓
Schedule / Publish
      ↓
Analytics
      ↓
Learn
```

---

## ✨ What Can Flint Do?

### 🎥 Multi-Source Content

Creators can start with:

* Video
* Audio / Podcast
* PDF / Document
* Blog / Article
* Presentation
* URL
* GitHub repository
* Existing content
* A simple idea

Flint analyzes the source and identifies topics, key points, quotes, hooks, and content opportunities.

### ✍️ Multi-Platform Generation

One source can become:

* YouTube scripts and descriptions
* Shorts / Reels scripts
* LinkedIn posts
* X threads
* Instagram captions and carousels
* Newsletters
* Blogs
* Community posts
* Quote cards
* Content ideas

Content is adapted for each platform instead of simply copying the same text everywhere.

### 🧬 Creator DNA

Flint can learn a creator's preferred:

* Tone
* Writing style
* Vocabulary
* Audience
* Niche
* Technical depth
* Hook style
* CTA style
* Content length
* Language

This helps generated content remain consistent with the creator's style.

### 🔥 Campaigns

A single idea can become an entire campaign.

For example:

```text
Product Launch
 ├── YouTube Video
 ├── 3 Short Videos
 ├── LinkedIn Post
 ├── X Thread
 ├── Instagram Carousel
 ├── Newsletter
 └── Blog
```

Creators can organize, edit, and schedule these assets together.

### 📅 Calendar & Publishing

Flint provides a content calendar for planning campaigns and individual posts.

A publishing abstraction allows different social platforms to be integrated without coupling the application to a specific platform.

### 📊 Analytics & Learning

Flint tracks available content performance metrics and helps creators understand patterns across their content.

Historical performance can provide additional context for future content generation.

### ♻️ Content Recycling

Older evergreen content can be refreshed and transformed into new formats instead of being forgotten.

### 🎬 AI Video Assistant

For long-form video, Flint can help identify potential highlights, topic changes, silence, repeated sections, and short-form opportunities.

Heavy video processing is handled through backend infrastructure and FFmpeg.

---

## 🤖 AI Architecture

Flint is **provider-agnostic** and supports multiple AI providers:

```text
                 AI Gateway
                     │
          ┌──────────┼──────────┐
          ↓          ↓          ↓
       Gemini    OpenRouter   Ollama Cloud
```

The application communicates with a common `AiProvider` abstraction, allowing AI providers and models to be changed without rewriting the core application.

AI tasks can include:

* Source analysis
* Content generation
* Rewriting
* Summarization
* Translation
* Campaign generation
* Content improvement
* Performance analysis

---

## 🏗️ Architecture

Flint is built using **Kotlin Multiplatform and Compose Multiplatform**.

```text
Compose Multiplatform
          ↓
   Shared KMP Layer
          ↓
 ┌────────┼────────┐
 ↓        ↓        ↓
Domain   Data   Presentation
 ↓        ↓        ↓
Use     Repos   ViewModels
Cases
          ↓
       Services
          ↓
 ┌────────┼───────────┐
 ↓        ↓           ↓
Firebase AI Gateway  Media
                    FFmpeg
```

The project follows:

* Clean Architecture
* MVVM
* Repository Pattern
* Kotlin Coroutines
* Kotlin Flow
* TDD

---

## ☁️ Firebase Backend

Firebase is used as Flint's backend platform.

| Service                  | Purpose                                                    |
| ------------------------ | ---------------------------------------------------------- |
| Firebase Authentication  | User accounts and sessions                                 |
| Cloud Firestore          | Application data                                           |
| Firebase Storage         | Videos, audio, documents and generated assets              |
| Cloud Functions          | Secure server-side operations and processing orchestration |
| Firebase Analytics       | Product analytics                                          |
| Firebase Cloud Messaging | Notifications                                              |

Heavy media processing is handled by backend workers using **FFmpeg** rather than inside the client.

---

## 💳 Membership & Ads

Flint uses a **capacity-based monetization model**.

The free tier can access the core product, but has limited resources such as:

* AI generations
* Media-processing minutes
* Projects
* Campaigns
* Analytics history

Membership provides higher limits and can include reduced or removed advertising.

The goal is:

> **Free users can experience Flint. Members get more capacity.**

Ads are implemented carefully and should never interfere with critical workflows such as editing, AI generation, authentication, publishing confirmation, or payment.

Usage limits and membership status are validated server-side.

---

## 🎨 Design

Flint's visual identity is inspired by:

**Spark → Flame → Stories**

The design uses a creative but professional palette built around:

* Warm amber/orange
* Deep violet
* Coral accents
* Warm neutral light surfaces
* Near-black dark surfaces

Flint supports:

* ☀️ Light mode
* 🌙 Dark mode
* 🖥️ System theme

The UI is designed to feel like a **creative workspace rather than a generic SaaS dashboard**, while remaining comfortable for long editing sessions.

---

## 📱 Platforms

Flint targets:

* Android
* iOS
* Desktop
* Web

Layouts are responsive and adapt to the platform.

Mobile focuses on quick creation and focused workflows, while desktop and web provide larger workspaces for content editing, campaigns, calendars, and analytics.

---

## 🧪 Testing

Flint follows a **Test-Driven Development** approach:

```text
Write Test
    ↓
Test Fails
    ↓
Implement
    ↓
Test Passes
    ↓
Refactor
    ↓
Repeat
```

Testing covers:

* Domain logic
* Use cases
* Repositories
* ViewModels
* AI routing
* Content generation
* Usage limits
* Membership
* Ads
* Campaigns
* Publishing
* Media processing
* Error handling

The target is **80%+ meaningful unit-test coverage**, along with platform and UI testing where practical.

---

## 🛠️ Tech Stack

| Area          | Technology                       |
| ------------- | -------------------------------- |
| UI            | Compose Multiplatform            |
| Shared Logic  | Kotlin Multiplatform             |
| Platforms     | Android, iOS, Desktop, Web       |
| Architecture  | Clean Architecture + MVVM        |
| Backend       | Firebase                         |
| Database      | Cloud Firestore                  |
| Storage       | Firebase Storage                 |
| Networking    | Ktor                             |
| AI            | Gemini, OpenRouter, Ollama Cloud |
| Media         | FFmpeg                           |
| Async         | Kotlin Coroutines + Flow         |
| Notifications | Firebase Cloud Messaging         |
| Testing       | Kotlin Multiplatform Testing     |

---

## 🔥 Vision

Flint is designed to become a creator's central workspace for turning ideas into an ongoing content engine.

```text
One Spark
    ↓
Create
    ↓
Transform
    ↓
Publish
    ↓
Analyze
    ↓
Learn
    ↓
Next Spark
```

### Flint

**One spark. Endless stories.**
