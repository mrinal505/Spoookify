# Implementation Plan - Spoookify Music App

Create a Spotify-like Android music application that streams audio-only content from YouTube.

## User Review Required

> [!IMPORTANT]
> **YouTube Data API Key**: To fetch search results and metadata, you will need a YouTube Data API v3 key from the [Google Cloud Console](https://console.cloud.google.com/).
>
> **Streaming Logic**: The YouTube Data API provides metadata but not direct audio stream URLs. This plan assumes the use of a metadata-driven approach. For actual playback of YouTube streams without video, we will implement a strategy using `Media3` and an extractor approach (e.g., integrating a library like `yt-dlp` wrapper or similar open-source extractors).

> [!WARNING]
> **Background Playback**: Playing YouTube audio in the background without a video view may technically violate YouTube's Terms of Service for standard API usage. This app is for educational/personal use.

## Proposed Changes

### 1. Project Infrastructure & Configuration

#### [MODIFY] [build.gradle.kts (app)](file:///E:/Spoookify/app/build.gradle.kts)
- Add dependencies for:
    - **Jetpack Compose** (Material 3)
    - **Media3** (ExoPlayer, Session, UI)
    - **Hilt** (Dependency Injection)
    - **Retrofit/OkHttp** (API calls)
    - **Coil** (Image loading)
    - **Navigation Compose**

#### [NEW] [NetworkModule.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/di/NetworkModule.kt)
- Hilt module for Retrofit and YouTube API service.

#### [NEW] [PlaybackModule.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/di/PlaybackModule.kt)
- Hilt module for Media3 ExoPlayer and MediaSession.

---

### 2. Media Playback Service (Core)

#### [NEW] [MusicService.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/service/MusicService.kt)
- Extend `MediaSessionService`.
- Initialize `ExoPlayer` and `MediaSession`.
- Handle background playback and notification management.

#### [NEW] [MusicController.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/playback/MusicController.kt)
- Wrapper around `MediaController` to provide a clean API for the ViewModels to interact with the service.

---

### 3. Data Layer (YouTube Integration)

#### [NEW] [YoutubeApiService.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/data/remote/YoutubeApiService.kt)
- Retrofit interface for YouTube Data API v3 (Search, Videos).

#### [NEW] [MusicRepository.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/data/repository/MusicRepository.kt)
- Fetch search results and convert them into internal `Track` models.

---

### 4. UI - Design System & Navigation

#### [NEW] [Theme.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/theme/Theme.kt)
- Define a "Spotify Dark" color palette (Black, Dark Grey, Spotify Green).

#### [NEW] [NavGraph.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/navigation/NavGraph.kt)
- Define destinations: Home, Search, Library, Player.

---

### 5. UI - Features

#### [NEW] [HomeViewModel.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/home/HomeViewModel.kt) & [HomeScreen.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/home/HomeScreen.kt)
- Display categories and trending tracks (fetched from YouTube).

#### [NEW] [SearchViewModel.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/search/SearchViewModel.kt) & [SearchScreen.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/search/SearchScreen.kt)
- Real-time search for songs.

#### [NEW] [PlayerViewModel.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/player/PlayerViewModel.kt) & [PlayerBar.kt](file:///E:/Spoookify/app/src/main/java/com/example/spoookify/ui/player/PlayerBar.kt)
- Persistent player bar visible at the bottom of all screens.
- Full-screen player view with controls and seek bar.

## Verification Plan

### Automated Tests
- Unit tests for `MusicRepository` to verify API parsing.
- Unit tests for `MusicController` state transitions.

### Manual Verification
- Deploy to device/emulator.
- Verify search functionality returns results.
- Verify playback starts upon selecting a track.
- Verify playback continues when the app is in the background.
- UI Check: Ensure the theme and layout match the Spotify aesthetic.
