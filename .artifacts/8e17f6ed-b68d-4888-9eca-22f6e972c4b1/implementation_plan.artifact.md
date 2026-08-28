# Spoookify Redesign & Feature Expansion Plan

This plan outlines the transformation of Spoookify into a high-end music player with AI-driven intelligence and professional-grade audio controls.

## User Review Required

> [!IMPORTANT]
> **Crossfade Support**: Media3/ExoPlayer doesn't support crossfade natively across `MediaItem` transitions out-of-the-box in a simple way (it requires custom `AudioSink` or dual players). I will implement a "Fade-out/Fade-in" transition first as a reliable alternative.
> **AI Metadata**: Since the app currently relies on YouTube extraction, "Mood" and "Energy" metadata is not always available. The AI DJ will use search keywords and user listening patterns to approximate these values.

## Proposed Changes

### [Audio Engine] (Phase 1)
Upgrade the playback core to handle professional audio effects.

#### [MODIFY] [MusicController.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/playback/MusicController.kt)
- Add support for `AudioProcessor` (Equalizer, DynamicsProcessing).
- Implement volume normalization logic.
- Add fade-in/fade-out on play/pause/track change.
- Implement sleep timer logic.

#### [NEW] [AudioProfileManager.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/playback/AudioProfileManager.kt)
- Manage EQ presets (Classic, Rock, Pop, etc.).
- Store per-song and per-artist EQ profiles in a local database.

---

### [Smart Intelligence] (Phase 2)
Implement the data layer for taste learning and AI recommendations.

#### [NEW] [UserAnalyticsRepository.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/data/repository/UserAnalyticsRepository.kt)
- Track listening events: `onSkip`, `onFinish`, `onRepeat`, `duration`.
- Build a "Taste Profile" based on tags and artists.

#### [MODIFY] [MusicRepository.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/data/repository/MusicRepository.kt)
- Add `getRelatedTracks(trackId)` method.
- Add `searchByMood(mood, energy)` method.

#### [NEW] [SmartQueueManager.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/playback/SmartQueueManager.kt)
- The "AI DJ" logic: Predicts the next 20-50 songs.
- Implements the "Why this song?" metadata generator.

---

### [UI Redesign] (Phase 3)
Modernize the interface and add controls for new features.

#### [MODIFY] [PlayerScreen.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/ui/player/PlayerScreen.kt)
- Redesign the main playback interface for a "Premium" feel.
- Add Energy Slider and Mood Selection.
- Add "Why this song?" info panel.

#### [NEW] [EqualizerScreen.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/ui/settings/EqualizerScreen.kt)
- Visual 10/20/31-band EQ (dynamic depending on device capabilities).
- Controls for Mono/Stereo, Balance, and ReplayGain.

#### [MODIFY] [SettingsScreen.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/ui/settings/SettingsScreen.kt)
- Configure skip intervals, fade duration, and Auto Audio Mode settings.

---

### [Automation] (Phase 4)
Context-aware audio switching.

#### [NEW] [AudioDeviceReceiver.kt](file:///E:/Spoookify/app/src/main/java/com/spoookify/service/AudioDeviceReceiver.kt)
- Listen for Bluetooth and Headphone connection events.
- Trigger `AudioProfileManager` to switch EQ profiles automatically.

## Verification Plan

### Automated Tests
- Unit tests for `SmartQueueManager` to verify recommendation logic.
- Unit tests for `AudioProfileManager` to ensure persistence of presets.

### Manual Verification
- Verify EQ changes are audible on various devices.
- Connect/disconnect Bluetooth to verify "Auto Audio Mode".
- Check that "Sleep Timer" correctly pauses playback.
