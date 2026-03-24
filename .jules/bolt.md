## 2024-03-24 - Avoid String.format in Compose Recompositions
**Learning:** `String.format` is extremely slow in high-frequency Compose component paths (like the video player position updater, running multiple times a second). It parses the format string repeatedly, creates regex matchers under the hood, and allocates multiple objects, contributing to UI stutter and GC pressure during playback.
**Action:** Replace `String.format` with manual `buildString` block or basic string concatenation for simple integer time formatting in any UI element updating frequently.
