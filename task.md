# Production & Efficiency Plan

## 1. Architectural Integrity
- **Scoping**: Ensure `ViewModel` components correctly map to the lifecycle of their respective screens. In Compose without Navigation component, view models created by `koinViewModel()` map to the Activity. Be aware that avoiding `Navigation` means explicit state clearing must be built for re-entrancy.
- **Resource Management**: Singleton resources like `ExoPlayer` require explicit hand-offs. Use Session IDs to ensure that stale ViewModels do not prematurely terminate active sessions belonging to new ones.

## 2. Best Practices for Media Playback
- **Asynchronous Preparation**: Always prepare the media player on a background thread or utilize Exoplayer's internal async preparation to not block the UI thread.
- **Automatic Cleanup**: Tie the player `release()` to the Activity/Service `onDestroy()` method. Tie `stop()` and `clearMediaItems()` to the active screen leaving the composition (e.g. `DisposableEffect`).

## 3. Storage and Caching
- **Offline Mode First**: Expand upon `FileStorageManager` for resilient video caching.
- **Data Eviction**: Optimize `LeastRecentlyUsedCacheEvictor` to not exceed a strict cache limit that will create UX problems for low-storage devices.

## 4. UI/UX Flow
- **Resume UX**: Implement UI visual indicators during the `LOADING` state so the user understands the player is buffering from the saved position.

## 5. Maintenance
- **Testing**: Integrate UI tests for player lifecycle cases (Screen A -> Screen B -> Screen A) to ensure regressions do not re-introduce the singleton override bug.
