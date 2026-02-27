package com.pulse.presentation.lecture

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.util.Rational
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
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
    viewModel: LectureViewModel = koinViewModel(parameters = { parametersOf(lectureId) })
) {
    val settingsManager: SettingsManager = koinInject()
    val savedRatio by settingsManager.splitRatioFlow.collectAsState(initial = 0.55f)
    var splitRatio by remember { mutableFloatStateOf(0.55f) }
    var initialized by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(savedRatio) {
        if (!initialized) {
            splitRatio = savedRatio
            initialized = true
        }
    }
    
    val lecture by viewModel.lecture.collectAsState()

    DisposableEffect(lectureId) {
        onDispose {
            // Holistic Cleanup: Ensure nothing remains when user exits
            viewModel.cleanup()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.updateLocalPdfPath(it.toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }
    }

    val onAddLocalPdf = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    val onPipClick = {
        if (context is ComponentActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
            builder.setAspectRatio(Rational(16, 9))
            context.enterPictureInPictureMode(builder.build())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (!isFullscreen) {
                FloatingActionButton(
                    onClick = { showNotes = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes")
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isFullscreen) {
                VideoPlayer(
                    player = viewModel.player,
                    modifier = Modifier.fillMaxSize(),
                    isFullscreen = true,
                    onFullscreenToggle = { isFullscreen = false },
                    onPipClick = onPipClick,
                    onSpeedChanged = { viewModel.setPlaybackSpeed(it) }
                )
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val screenWidthDp = maxWidth
                    val isTablet = screenWidthDp > 840.dp
                    val totalWidth = constraints.maxWidth.toFloat()

                    if (isTablet) {
                        Row(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(splitRatio)
                                    .clipToBounds()
                            ) {
                                VideoPlayer(
                                    player = viewModel.player,
                                    modifier = Modifier.fillMaxSize(),
                                    onFullscreenToggle = { isFullscreen = true },
                                    onPipClick = onPipClick,
                                    onSpeedChanged = { viewModel.setPlaybackSpeed(it) }
                                )
                            }

                            Box(
                                Modifier
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
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f - splitRatio)
                                    .clipToBounds()
                            ) {
                                lecture?.let { l ->
                                    PdfViewer(
                                        pdfPath = l.pdfLocalPath, 
                                        initialPage = l.lastPage,
                                        isPdfDownloaded = l.isPdfDownloaded,
                                        onPageChanged = { page -> viewModel.updatePdfPage(page) },
                                        onAddLocalPdf = onAddLocalPdf,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().clipToBounds()) {
                            VideoPlayer(
                                player = viewModel.player,
                                modifier = Modifier.weight(0.5f),
                                onFullscreenToggle = { isFullscreen = true },
                                onPipClick = onPipClick,
                                onSpeedChanged = { viewModel.setPlaybackSpeed(it) }
                            )
                            PdfViewer(
                                pdfPath = lecture?.pdfLocalPath ?: "", 
                                initialPage = lecture?.lastPage ?: 0,
                                isPdfDownloaded = lecture?.isPdfDownloaded ?: false,
                                onPageChanged = { page -> viewModel.updatePdfPage(page) },
                                onAddLocalPdf = onAddLocalPdf,
                                modifier = Modifier.weight(0.5f)
                            )
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
