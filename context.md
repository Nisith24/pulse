# PULSE Technical Documentation

## 1. Architecture & Design Patterns
- **Language**: 100% Kotlin.
- **UI Framework**: **Jetpack Compose** (Material 3) for the entire application.
- **Architecture**: **MVVM (Model-View-ViewModel)** with Clean Architecture principles.
    - **Presentation Layer**: Compose Screens and State-driven ViewModels.
    - **Domain Layer**: Pure Kotlin Use Cases (`GetLectureStreamUrlUseCase`, `SyncLecturesUseCase`) and Repository interfaces.
    - **Data Layer**: Room Database, DataStore, Google Drive API implementation.
- **Dependency Injection**: **Koin** (Modules for `appModule`, `viewModelModule`, `useCaseModule`).
- **Reactive Stream**: **Kotlin Coroutines & Flows** for non-blocking UI and data propagation.
- **Navigation**: Compose Navigation with strongly typed routes (using ID-based arguments).

## 2. Data Layer & Schema

### Database Schema (Room v4)
The app maintains a local cache of both remote (Google Drive) and local (Internal Storage) metadata.

#### **`lectures` Table**
Holds metadata for both Video and PDF pairs.
- `id` (PK): Drive File ID (String) or UUID (Local).
- `name`: Display name.
- `videoId` / `pdfId`: Remote source identifiers.
- `videoLocalPath` / `pdfLocalPath`: URIs for local files (SAF).
- `isLocal`: Boolean (True if added from device, False if from Drive).
- `isPdfDownloaded`: Tracking status for Drive files.
- `lastPosition` (ms) & `videoDuration` (ms): Playback progress.
- `lastPage`: PDF reading progress.
- `speed`: Saved playback rate (e.g., 1.5f).
- `isFavorite`: Boolean toggle for starred content.
- `updatedAt`: Sync timestamp.


#### **`notes` Table**
Timestamped user annotations.
- `id`: Auto-gen Long.
- `lectureId`: Foreign Key (CASCADE) to `lectures`.
- `timestamp`: Video millisecond position.
- `text`: Note content.

### Cloud & Services Integration
- **Services Architecture**: The app uses a modular services architecture located in `com.pulse.data.services`.
- **BTR Service**: The primary cloud service for lecture management.
    - **`BtrAuthManager`**: Manages Cloud/OAuth2 tokens via Play Services (agnostic abstraction).
    - **`BtrService`**: Low-level REST API calls for file listing and generating secure `streamUrl` for BTR content.
- **Naming Convention Strategy**: The system automatically pairs Videos and PDFs by parsing file names for matching prefixes/module numbers during sync.
- **Connectivity Monitoring**: A `NetworkMonitor` utility tracks real-time internet availability using Kotlin Flows, powering the "Offline Mode" indicators in the Cloud/BTR sections.

## 3. Presentation Layer & UI Implementation

### Library Screen (`LibraryScreen.kt`)
- **Tabbed Interface**: "Home" (Local Media) and "**SERVICES**" (Modular Cloud Library).
- **Service Directory**: The SERVICES tab features a hierarchical menu (e.g., BTR service list) allowing for future expansion of cloud capabilities.
- **Advanced Searching**: Reactive `combine` filter that processes queries across `StateFlow` streams without UI lag.
- **Smart Grouping logic**:
    - **Grouping**: Uses Regex `\d+` to extract module numbers from lecture names.
    - **Sections**: Renders `StickyHeader`-style labels ("Module X", "Other Files").
    - **Natural Sorting**: Implements custom sorting where numbers are compared numerically (Module 2 < Module 10), preventing alphabetical errors.
- **Premium UI Components**: 
    - **3D Service Cards**: High-elevation, premium cards for BTR services with prominent iconography.
    - **Large Actions**: Enhanced `LargeFloatingActionButton` in the Home tab for better ergonomics.
    - **Offline Indicators**: Real-time status bar in the Cloud tab when connectivity is lost.
- **Favoriting Engine**: 
    - Integrated filtering logic that combines search and favorite status in a single reactive stream.
    - Persistent toggle icons on every lecture card with immediate UI updates.


### Lecture Screen (`LectureScreen.kt`)
- **Responsive Layout**:
    - **BoxWithConstraints**: Detects screen size to switch between `Row` (Tablet/Landscape) and `Column` (Phone/Portrait).
    - **Draggable Divider**: Implements a custom horizontal divider for tablets to adjust video/PDF split ratio, persisted via `DataStore`.
- **Integrated PDF Reader**:
    - **Engine**: Powered by `AndroidView` wrapping a native Pdfium-based `PDFView`.
    - **State Persistence**: Remembers page position and handles resource `recycle()` on dispose.
    - **Visibility Toggle**: A close button allows users to dismiss the PDF to maximize video screen real estate.
- **Advanced Video Player (`VideoPlayer.kt`)**:
    - **Media3 (ExoPlayer)**: High-performance streaming with custom `PlayerProvider` to manage session lifecycles across rotations.
    - **World Standard Gestures**: 
        - **Brightness/Volume**: Vertical swipes (Left/Right) with dynamic feedback overlays.
        - **Seeking**: Double-tap (10s back/forward) with visual indicators.
    - **Minimalist Mode**: UI automatically scales down icons and padding when the window size is restricted (e.g., small split-screen).
    - **Settings Controls**:
        - **Speed Control**: 0.5x, 1.0x, 1.25x, 1.5x, 2.0x.
        - **Aspect Ratio**: Fit, Zoom (Crop), Fill (Stretch).
    - **Sync System**: Custom Compose controls are synced with native `PlayerView` visibility via `ControllerVisibilityListener`.

### Picture-in-Picture (PiP) Implementation
- **Activity-Level Logic**: Implemented in `MainActivity` using `onUserLeaveHint` and `setAutoEnterEnabled` (Android 12+).
- **Seamless Continuity**: Video playback continues uninterrupted when switching to PiP; the `LectureScreen` stays mounted but switches to a minimalist "Early Return" rendering pattern.
- **Industry Standard Parameters**: Automatically calculates and updates `AspectRatio` based on the active video stream's dimensions.
- **Clean UI**: In PiP mode, all custom Compose overlays (Title, FABs, PDF, Notes, Gestures) are completely disabled. Only the raw video stream is rendered, using system-level controls for a premium look.
- **Download Management**:
    - **Location**: Downloads are saved to the public `Movies/Pulse` directory for easy user access.
    - **Reliability**: Supports robust cancellation and real-time progress/speed/ETA statistics.
    - **Offline Playback**: Automatically detects and plays local video files when cloud content is downloaded, enabling seamless offline viewing.


## 4. Core Business Logic

### Playback Persistence
- **State Preservation**: The `LectureViewModel` uses a periodic `Job` (every 5 seconds) and an `ON_STOP` lifecycle flush to ensure playback position is never lost.
- **Non-Cancellable Save**: Database updates use `NonCancellable` context to prevent truncation of save operations during Activity destruction.

### Security & File Access
- **SAF Persistence**: Implements `takePersistableUriPermission` for all local media, ensuring that the app retains access to selected files even after a device reboot.
- **Scoped Storage**: Adheres to Android's storage requirements by using the Storage Access Framework for local video imports.

### Note Management
- **Video Linking**: Every note is inherently tied to a millisecond timestamp.
- **Seeking**: Tapping a note in the `NotesPanel` triggers a `player.seekTo()` call, enabling rapid review of specific lecture segments.

## 5. Build & Environment
- **SDK**: CompileSDK 35, MinSDK 26.
- **Gradle**: Implementation of KSP (Kotlin Symbol Processing) for Room and Compose Compiler.
- **Database Migrations**: Automatic cleanup and schema updates via `fallbackToDestructiveMigration` and periodic version bumps (Current: v7).
- **Caching**: Uses ExoPlayer `SimpleCache` for smoother streaming of Drive-hosted content.

