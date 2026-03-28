package com.pulse.presentation.lecture

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.util.Rational
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.data.local.SettingsManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.pulse.presentation.components.DrivePdfPicker
import com.pulse.presentation.lecture.components.SplitResizableContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LectureScreen(
    lectureId: String,
    sourceFolderId: String? = null,
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
    
    val drivePdfs by viewModel.drivePdfs.collectAsState()
    val isLoadingDrivePdfs by viewModel.isLoadingDrivePdfs.collectAsState()
    val pdfDownloadState by viewModel.pdfDownloadState.collectAsState()
    var showDrivePdfPicker by remember { mutableStateOf(false) }

    val onAddDrivePdf = {
        viewModel.loadDrivePdfs(sourceFolderId)
        showDrivePdfPicker = true
    }
    
    androidx.activity.compose.BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onNavigateBack()
        }
    }

    val lecture by viewModel.lecture.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val highlights by viewModel.notes.collectAsState(initial = emptyList())
    val visuals by viewModel.visuals.collectAsState(initial = emptyList())
    val pdfHorizontalOrientation by viewModel.pdfHorizontalOrientation.collectAsState()
    val isBackgroundPlaybackEnabled by settingsManager.backgroundPlaybackFlow.collectAsState(initial = true)

    val hasVideo = remember(lecture) {
        val l = lecture
        l != null && (!l.videoId.isNullOrEmpty() || !l.videoLocalPath.isNullOrEmpty())
    }

    LaunchedEffect(savedRatio, lecture) {
        if (!initialized && savedRatio > 0.1f) {
            splitRatio = savedRatio
            initialized = true
        }
    }

    LaunchedEffect(playerState) {
        if (playerState is PlayerUiState.ERROR) {
            isFullscreen = false
        }
    }

    // ── PiP state ──
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
            } catch (_: SecurityException) {
                // Some providers don't support persistable permissions
            } catch (_: Exception) {}
            showPdf = true
            viewModel.importLocalPdf(it)
        }
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.playSessionIfNeeded()
    }

    val onAddLocalPdf = {
        pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }

    val onPipClick = {
        if (context is ComponentActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        showPdf = true
        val l = viewModel.lecture.value
        viewModel.updateLocalPdfPath(if (l != null) "blank_note_${l.id}" else "blank_note")
    }

    // ── Lifecycle management ──
    DisposableEffect(lectureId, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE, 
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    viewModel.saveProgress()
                    val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else false
                    if (!inPip && !isBackgroundPlaybackEnabled) {
                        viewModel.player.pause()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_START,
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.playSessionIfNeeded()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val isRotating = activity?.isChangingConfigurations == true
            val goingToMiniPlayer = playerProvider.isMiniPlayerActive && playerProvider.miniPlayerLectureId == lectureId
            val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity?.isInPictureInPictureMode == true
            } else false
            val isFinishing = activity?.isFinishing == true

            if (!isRotating && !goingToMiniPlayer && !inPip && isFinishing) {
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

                    androidx.compose.animation.AnimatedVisibility(
                        visible = !annotationState.isDrawingMode,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally { it },
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally { it }
                    ) {
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
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isPip) PaddingValues(0.dp) else padding),
            color = if (isPip) Color.Black else MaterialTheme.colorScheme.background
        ) {
            val state = playerState
            if (state is PlayerUiState.PERMISSION_REQUIRED) {
                PermissionRequiredScreen(
                    onGrant = { authLauncher.launch(state.intent) }
                )
            } else {
                val l = lecture
                if (l == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { motionEvent ->
                                handleStylusInput(motionEvent, viewModel)
                            }
                    ) {
                        val isTablet = maxWidth > 840.dp
                        val isStandalonePdf = !hasVideo && (l.pdfLocalPath.isNotEmpty() || l.pdfId != null) && !l.pdfLocalPath.startsWith("blank_note")

                        // Auto-download Drive PDFs
                        if (showPdf && !l.isPdfDownloaded && l.pdfId != null) {
                            LaunchedEffect(l.id) {
                                viewModel.downloadPdf()
                            }
                        }

                        if (isStandalonePdf) {
                            // ── Standalone PDF Mode ──
                            LecturePdfViewer(
                                lecture = l,
                                pdfDownloadState = pdfDownloadState,
                                visuals = visuals,
                                annotationState = annotationState,
                                viewModel = viewModel,
                                onAddLocalPdf = onAddLocalPdf,
                                onCreateBlankNote = onCreateBlankNote,
                                onAddDrivePdf = onAddDrivePdf,
                                onClosePdf = onClosePdf,
                                isHorizontal = l.pdfIsHorizontal,
                                onOrientationChange = { viewModel.updatePdfOrientation(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // ── Hybrid Split-Screen Mode ──
                            
                            // 1. Video panel
                            if (hasVideo) {
                                Box(
                                    modifier = when {
                                        isPip || isFullscreen -> Modifier.fillMaxSize()
                                        isTablet && showPdf -> Modifier.fillMaxHeight().fillMaxWidth(splitRatio)
                                        !isTablet && showPdf -> Modifier.fillMaxWidth().fillMaxHeight(splitRatio)
                                        else -> Modifier.fillMaxSize()
                                    }.clipToBounds()
                                ) {
                                    if (state is PlayerUiState.ERROR) {
                                        VideoErrorScreen(
                                            message = state.message,
                                            onRetry = { viewModel.playSessionIfNeeded() }
                                        )
                                    } else {
                                        VideoPlayer(
                                            player = viewModel.player,
                                            title = if (isPip) "" else (lecture?.name ?: ""),
                                            modifier = Modifier.fillMaxSize(),
                                            isFullscreen = isFullscreen,
                                            isPip = isPip,
                                            onNavigateBack = onNavigateBack,
                                            onFullscreenToggle = { isFullscreen = !isFullscreen },
                                            onPipClick = onPipClick,
                                            onSpeedChanged = { viewModel.setPlaybackSpeed(it) }
                                        )
                                    }
                                }
                            }

                            // 2. PDF panel with drag handle (split-screen)
                            if (!isPip && !isFullscreen && showPdf) {
                                SplitResizableContainer(
                                    splitRatio = splitRatio,
                                    onSplitRatioChange = { splitRatio = it },
                                    onDragStopped = { finalRatio ->
                                        scope.launch { settingsManager.saveSplitRatio(finalRatio) }
                                    },
                                    isHorizontal = isTablet,
                                    primaryContent = {
                                        // Empty — video is already placed above via absolute positioning
                                    },
                                    secondaryContent = {
                                        LecturePdfViewer(
                                            lecture = l,
                                            pdfDownloadState = pdfDownloadState,
                                            visuals = visuals,
                                            annotationState = annotationState,
                                            viewModel = viewModel,
                                            onAddLocalPdf = onAddLocalPdf,
                                            onCreateBlankNote = onCreateBlankNote,
                                            onAddDrivePdf = onAddDrivePdf,
                                            onClosePdf = onClosePdf,
                                            isHorizontal = pdfHorizontalOrientation,
                                            onOrientationChange = { viewModel.savePdfOrientation(it) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
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

        if (showDrivePdfPicker) {
            val folderPdf by viewModel.folderPdf.collectAsState()
            DrivePdfPicker(
                pdfs = drivePdfs,
                onPdfSelected = { pdf ->
                    showDrivePdfPicker = false
                    showPdf = true
                    viewModel.attachDrivePdf(pdf)
                },
                onDismissRequest = { showDrivePdfPicker = false },
                isLoading = isLoadingDrivePdfs,
                folderPdf = folderPdf
            )
        }
    }
}

// ── Extracted helper: Eliminates 3x duplicate PdfViewer call sites ──

@Composable
private fun LecturePdfViewer(
    lecture: com.pulse.core.data.db.Lecture,
    pdfDownloadState: com.pulse.data.repository.PdfDownloadState,
    visuals: List<com.pulse.core.data.db.NoteVisual>,
    annotationState: AnnotationState,
    viewModel: LectureViewModel,
    onAddLocalPdf: () -> Unit,
    onCreateBlankNote: () -> Unit,
    onAddDrivePdf: () -> Unit,
    onClosePdf: () -> Unit,
    isHorizontal: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    PdfViewer(
        pdfPath = lecture.pdfLocalPath,
        title = lecture.name,
        initialPage = lecture.lastPdfPage,
        isPdfDownloaded = lecture.isPdfDownloaded,
        pdfDownloadState = pdfDownloadState,
        visuals = visuals,
        annotationState = annotationState,
        onPageChanged = { viewModel.updatePdfState(it) },
        onAddVisual = { type, data, page, color, width, alpha ->
            viewModel.addVisual(type, data, page, color, width, alpha)
        },
        onAddVisualAtPos = { type, x, y, page, color, width, alpha ->
            viewModel.addVisualAtPos(type, x, y, page, color, width, alpha)
        },
        onDeleteVisual = { id -> viewModel.deleteVisual(id) },
        onAddLocalPdf = onAddLocalPdf,
        onCreateBlankNote = onCreateBlankNote,
        onAddDrivePdf = onAddDrivePdf,
        onAddPage = { viewModel.addPage() },
        totalPages = lecture.pdfPageCount,
        onClose = onClosePdf,
        isHorizontal = isHorizontal,
        onOrientationChange = onOrientationChange,
        modifier = modifier
    )
}

// ── Extracted sub-screens ──

@Composable
private fun PermissionRequiredScreen(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Cloud Access Required", style = MaterialTheme.typography.titleMedium)
            Text(
                "This video requires additional permission from Google Drive.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onGrant) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun VideoErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry Video")
            }
        }
    }
}

// ── Stylus handler: Extract touch logic from lambda ──

private fun handleStylusInput(motionEvent: MotionEvent, viewModel: LectureViewModel): Boolean {
    val isStylusTool = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                       motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
    if (isStylusTool && motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
        val stylusBtn = MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY
        if (motionEvent.buttonState and stylusBtn != 0) {
            if (viewModel.player.isPlaying) viewModel.player.pause()
            else viewModel.player.play()
        }
    }
    return false // Don't consume — let events flow to children
}
