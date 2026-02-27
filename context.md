# PULSE Project Context

## Overview
**PULSE** is a native Android application built with Kotlin and Jetpack Compose. It serves as an educational platform/library to stream video lectures and read PDF notes directly from Google Drive, as well as manage local media, while allowing users to take timestamped notes.

## Tech Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles (Use Cases)
*   **Dependency Injection:** Koin
*   **Local Storage:** Room Database (Offline metadata & notes), DataStore
*   **Media Player:** AndroidX Media3 (ExoPlayer) with advanced settings (Speed, Aspect Ratio) and caching.
*   **PDF Viewer:** Android PDF Viewer (Pdfium) with bound-aware rendering.
*   **Network/API:** OkHttp, Google Drive API v3 (REST)
*   **Authentication:** Google Sign-In (Play Services Auth)
*   **Asynchrony:** Kotlin Coroutines & Flows

## Core Features
1.  **Hybrid Library (Homepage):**
    *   **Tabs:** "Home" for local device files and "BTR" for Google Drive files.
    *   **Local Management:** Add videos/PDFs from the device via Storage Access Framework (SAF). Displays actual filenames and persists access across restarts using `takePersistableUriPermission`.
    *   **Remote Sync:** Authenticates via `DriveAuthManager` and pares Drive files based on naming conventions.
2.  **Lecture Screen (Player & Reader):**
    *   **Responsive Multi-Pane:** Tablet-optimized horizontal split or phone-optimized vertical stack. Features a draggable divider to adjust panel sizes.
    *   **Advanced Video Player:** Supports streaming (remote) and local playback. Includes on-the-fly Playback Speed control (0.5x - 2.0x) and Aspect Ratio switching (Fit, Zoom, Stretch).
    *   **Bounded PDF Reader:** Custom PDF rendering engine that strictly honors layout boundaries (no overlays/bleeding) and remembers the last page.
    *   **Timestamped Notes:** Integrated notes panel for capturing thoughts linked to specific video timecodes.

## App Architecture
*   `com.pulse.MainActivity`: Entry point and routing.
*   `com.pulse.domain`: Domain layer containing Use Cases (`GetLectureStreamUrlUseCase`, `SyncLecturesUseCase`) and interfaces.
*   `com.pulse.data`: Implementation layer.
    *   `db`: Room entities (`Lecture`, `Note`) and DAOs.
    *   `drive`: `DriveService` and `DriveAuthManager`.
    *   `repository`: `LectureRepository` coordinating local and remote data.
*   `com.pulse.presentation`: UI layer.
    *   `library`: Tabbed navigation, file picking, and content listing.
    *   `lecture`: Integrated player, PDF viewer, and notes management.

## Recent Fixes & Changes
*   **UI Holistics:** Fixed critical PDF rendering issues where the document would overlay the video panel. Resolved via strict clipping (`clipToBounds`), Surface isolation, and enforcing `FitPolicy.BOTH` in the native render engine.
*   **Local File Support:** Revamped the app to support local media files. Introduced logic to extract real filenames from content URIs and added a removal function (X button) to manage the local library.
*   **Advanced Media Controls:** Integrated Playback Speed and Aspect Ratio settings into the `VideoPlayer` overlay.
*   **Security & Persistence:** Implemented persistable URI grants for all local files to ensure they remain playable even after the device or app is restarted.
*   **Database Schema:** Updated to v4 to include `isLocal` metadata and support hybrid local/remote content tracking.
