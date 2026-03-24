## 2025-03-02 - [Avoid String.format in Compose]
**Learning:** Using String.format inside Composable functions or their descendants during video playback (e.g., time formatting) causes excessive object allocations and parsing overhead.
**Action:** Replaced String.format with buildString in ControlsOverlay.kt and NotesPanel.kt.
