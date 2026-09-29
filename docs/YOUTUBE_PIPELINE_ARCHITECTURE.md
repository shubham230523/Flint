# Flint — YouTube → Instagram Pipeline Architecture Note

## Overview
Stage 1 of the YouTube → Instagram pipeline enables Flint to ingest a YouTube URL, parse metadata and transcripts, perform AI-driven content analysis, discover Instagram content opportunities, and generate platform-optimized assets (Reels, Carousels, Stories, Quote Posts, Captions) preserved with Creator DNA.

## Architecture & Module Structure
- **KMP Multiplatform Domain Layer**: `shared/src/commonMain/kotlin/com/shubhamthorat/flint/domain/`
  - **Models**: `SourceItem`, `ContentAsset`, `YouTubeVideoData`, `VideoTranscript`, `YouTubeContentAnalysis`, `InstagramContentOpportunity`, `CreatorDNA`.
  - **Repositories**: `SourceRepository`, `ContentRepository`, `AiRepository` (`AiTaskRouter`).
  - **Use Cases**: `CreateYouTubeSourceUseCase`, `ProcessYouTubeSourceUseCase`, `AnalyzeYouTubeContentUseCase`, `GenerateInstagramOpportunitiesUseCase`, `GenerateInstagramReelUseCase`, `GenerateInstagramCarouselUseCase`, `GenerateInstagramStoriesUseCase`, `GenerateInstagramQuotePostsUseCase`, `GenerateInstagramCaptionUseCase`.
- **Data & Persistence**: `shared/src/commonMain/kotlin/com/shubhamthorat/flint/data/`
  - `FirestoreSourceRepository`, `FirestoreContentRepository` with `InMemory` fallback implementations.
- **Presentation Layer**: `shared/src/commonMain/kotlin/com/shubhamthorat/flint/presentation/`
  - Navigation: `FlintScreen.YouTubeWorkspace` managed by `NavigationManager`.
  - Screen: `YouTubeWorkspaceScreen` built with Flint Design System (`FlintTheme`, `FlintButton`, `FlintCard`, `FlintChip`, `FlintTextField`).
