package com.pulse.presentation.lecture

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.util.Rational
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.pulse.data.local.SettingsManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureScreen(
    lectureId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: LectureViewModel = koinViewModel(
        key = lectureId,
        parameters = { parametersOf(lectureId) }
    )
) {
    val settingsManager: SettingsManager = koinInject()
    val playerProvider: PlayerProvider = koinInject()
    val savedRatio by settingsManager.splitRatioFlow.collectAsState(initial = 0.55f)
    var splitRatio by remember { mutableFloatStateOf(0.55f) }
    var initialized by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(true) }
    var showNotes by remember { mutableStateOf(false) }
    var showPdf by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lecture by viewModel.lecture.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val highlights by viewModel.notes.collectAsState(initial = emptyList())
    val visuals by viewModel.visuals.collectAsState(initial = emptyList())
    val pdfHorizontalOrientation by viewModel.pdfHorizontalOrientation.collectAsState()
    val isBackgroundPlaybackEnabled by settingsManager.backgroundPlaybackFlow.collectAsState(initial = true)

    LaunchedEffect(savedRatio, lecture) {
        if (!initialized && savedRatio > 0.1f) {
            splitRatio = savedRatio
            initialized = true
        }
        
        // Standalone PDF Mode: If no video is present, expand PDF to full screen automatically
        lecture?.let { l ->
            val hasVideo = l.videoId != null || (l.videoLocalPath != null && l.videoLocalPath != "")
            if (!hasVideo && l.pdfLocalPath != "blank_note" && (l.pdfId != null || (l.pdfLocalPath != null && l.pdfLocalPath != ""))) {
                splitRatio = 0f
            }
        }
    }

    // ── Industry Standard PiP: Observe system PiP state ──
    var isPip by remember { mutableStateOf(false) }
    val annotationState = rememberAnnotationState()

    DisposableEffect(context) {
        val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isPip = info.isInPictureInPictureMode
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.addOnPictureInPictureModeChangedListener(listener)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.removeOnPictureInPictureModeChangedListener(listener)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lectureId, lifecycleOwner) {
        viewModel.playSessionIfNeeded()
    }

    // PDF picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            viewModel.updateLocalPdfPath(it.toString())
        }
    }

    val onAddLocalPdf = {
        // SAF OpenDocument doesn't need READ_EXTERNAL_STORAGE, and requesting it can crash on some devices.
        pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }

    // ── PiP button click: request system PiP ──
    val onPipClick = {
        if (context is ComponentActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Stability: Clear orientation lock before entering PiP to avoid sizing glitches
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
            
            val videoSize = viewModel.player.videoSize
            val aspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
                 Rational(videoSize.width, videoSize.height)
            } else {
                Rational(16, 9)
            }
            context.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
            )
        }
    }

    val onClosePdf = {
        showPdf = false
        viewModel.updateLocalPdfPath("")
    }

    val onCreateBlankNote = {
        viewModel.updateLocalPdfPath("blank_note")
    }

    // Standard Operating Procedure: Handle Stop on Exit and Lifecycle
    DisposableEffect(lectureId, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE, 
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    Log.d("LectureScreen", "Lifecycle $event: Saving progress")
                    viewModel.saveProgress()
                    val activity = context as? androidx.activity.ComponentActivity
                    val inPip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else false
                    if (!inPip && !isBackgroundPlaybackEnabled) {
                        viewModel.player.pause()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val activity = context as? ComponentActivity
            val isRotating = activity?.isChangingConfigurations == true
            val goingToMiniPlayer = playerProvider.isMiniPlayerActive && playerProvider.miniPlayerLectureId == lectureId
            val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity?.isInPictureInPictureMode == true
            } else false
            if (!isRotating && !goingToMiniPlayer && !inPip) {
                Log.d("LectureScreen", "Disposing: Exiting session")
                viewModel.onExit()
            } else {
                viewModel.saveProgress()
            }
        }
    }

    // ── UI ──
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (!isFullscreen && !isPip) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    if (!showPdf) {
                        FloatingActionButton(
                            onClick = { showPdf = true },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Show PDF", modifier = Modifier.size(20.dp))
                        }
                    }
                    FloatingActionButton(
                        onClick = { showNotes = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes")
                    }
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isPip) PaddingValues(0.dp) else padding)
                .statusBarsPadding(),
            color = if (isPip) Color.Black else MaterialTheme.colorScheme.background
        ) {
            when (val state = playerState) {
                is PlayerUiState.ERROR -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                else -> {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val totalWidth = constraints.maxWidth.toFloat()
                        val isTablet = maxWidth > 840.dp
                        
                        // 1. One Single Stable Placement for Video (prevents PiP freezes)
                        Box(
                            modifier = when {
                                isPip || isFullscreen -> Modifier.fillMaxSize()
                                isTablet && showPdf -> Modifier.fillMaxHeight().fillMaxWidth(splitRatio)
                                !isTablet && showPdf -> Modifier.fillMaxWidth().fillMaxHeight(splitRatio)
                                else -> Modifier.fillMaxSize()
                            }.clipToBounds()
                        ) {
                            VideoPlayer(
                                player = viewModel.player,
                                title = if (isPip) "" else (lecture?.name ?: ""),
                                modifier = Modifier.fillMaxSize(),
                                isFullscreen = isFullscreen,
                                isPip = isPip,
                                onFullscreenToggle = { isFullscreen = !isFullscreen },
                                onPipClick = onPipClick,
                                onSpeedChanged = { viewModel.setPlaybackSpeed(it) }
                            )
                        }

                        // 2. The other components (PDF, drag handle)
                        if (!isPip && !isFullscreen && showPdf) {
                            if (isTablet) {
                                // 1. PDF View Tablet (Drawn first, lower Z)
                                Box(
                                    Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .fillMaxWidth(1f - splitRatio)
                                        .clipToBounds()
                                ) {
                                    lecture?.let { l ->
                                        PdfViewer(
                                            pdfPath = l.pdfLocalPath,
                                            title = l.name,
                                            initialPage = 0,
                                            isPdfDownloaded = l.isPdfDownloaded,
                                            visuals = visuals,
                                            annotationState = annotationState,
                                            onPageChanged = { },
                                            onAddVisual = { type, data, page, color, width ->
                                                viewModel.addVisual(type, data, page, color, width)
                                            },
                                            onDeleteVisual = { id -> viewModel.deleteVisual(id) },
                                            onAddLocalPdf = onAddLocalPdf,
                                            onCreateBlankNote = onCreateBlankNote,
                                            onAddPage = { viewModel.addPage() },
                                            totalPages = l.pdfPageCount,
                                            onClose = onClosePdf,
                                            isHorizontal = pdfHorizontalOrientation,
                                            onOrientationChange = { viewModel.savePdfOrientation(it) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // 2. Drag Handle Tablet (Drawn last, highest Z)
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val dragHandleStart = with(density) { (totalWidth * splitRatio).toDp() }

                                Box(
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = dragHandleStart - 6.dp)
                                        .width(12.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .draggable(
                                            orientation = Orientation.Horizontal,
                                            state = rememberDraggableState { delta ->
                                                splitRatio = (splitRatio + delta / totalWidth).coerceIn(0.2f, 0.8f)
                                            },
                                            onDragStopped = {
                                                scope.launch { settingsManager.saveSplitRatio(splitRatio) }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(16.dp))
                                }
                            } else {
                                // 1. PDF View Phone (Drawn first, lower Z)
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .fillMaxHeight(1f - splitRatio)
                                        .clipToBounds()
                                ) {
                                    PdfViewer(
                                        pdfPath = lecture?.pdfLocalPath ?: "",
                                        title = lecture?.name ?: "Lecture",
                                        initialPage = 0,
                                        isPdfDownloaded = lecture?.isPdfDownloaded ?: false,
                                        visuals = visuals,
                                        annotationState = annotationState,
                                        onPageChanged = { },
                                        onAddVisual = { type, data, page, color, width ->
                                            viewModel.addVisual(type, data, page, color, width)
                                        },
                                        onDeleteVisual = { id -> viewModel.deleteVisual(id) },
                                        onAddLocalPdf = onAddLocalPdf,
                                        onCreateBlankNote = onCreateBlankNote,
                                        onAddPage = { viewModel.addPage() },
                                        totalPages = lecture?.pdfPageCount ?: 0,
                                        onClose = onClosePdf,
                                        isHorizontal = pdfHorizontalOrientation,
                                        onOrientationChange = { viewModel.savePdfOrientation(it) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // 2. Drag Handle Phone (Drawn last, highest Z)
                                val dragHandleStart = maxHeight * splitRatio
                                val totalHeightPx = constraints.maxHeight.toFloat()

                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = dragHandleStart - 6.dp)
                                        .height(12.dp)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .draggable(
                                            orientation = Orientation.Vertical,
                                            state = rememberDraggableState { delta ->
                                                splitRatio = (splitRatio + delta / totalHeightPx).coerceIn(0.2f, 0.8f)
                                            },
                                            onDragStopped = {
                                                scope.launch { settingsManager.saveSplitRatio(splitRatio) }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showNotes) {
            ModalBottomSheet(
                onDismissRequest = { showNotes = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                modifier = Modifier.fillMaxHeight(0.7f)
            ) {
                NotesPanel(viewModel = viewModel)
            }
        }
    }
}
