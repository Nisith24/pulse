package com.pulse.presentation.lecture

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import java.io.File
import com.pulse.core.data.db.NoteVisual
import com.pulse.core.data.db.VisualType
import com.pulse.core.presentation.components.DrawingCanvas
import com.pulse.presentation.lecture.components.PdfPageIndicator
import com.pulse.presentation.lecture.components.PdfSettingsMenu
import com.pulse.presentation.lecture.components.AnnotationToolbar
import com.pulse.presentation.lecture.components.AnnotationToggleButton

@Composable
fun PdfViewer(
    pdfPath: String,
    title: String,
    initialPage: Int,
    totalPages: Int = 0,
    isPdfDownloaded: Boolean,
    pdfDownloadState: com.pulse.data.repository.PdfDownloadState = com.pulse.data.repository.PdfDownloadState.Idle,
    visuals: List<NoteVisual>,
    annotationState: AnnotationState,
    onPageChanged: (Int) -> Unit,
    onAddVisual: (VisualType, String, Int, Int, Float, Float) -> Unit,
    onAddVisualAtPos: (VisualType, Float, Float, Int, Int, Float, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    onCreateBlankNote: () -> Unit = {},
    onAddPage: () -> Unit = {},
    onAddLocalPdf: () -> Unit = {},
    onAddDrivePdf: () -> Unit = {},
    onClose: () -> Unit = {},
    isHorizontal: Boolean = false,
    onOrientationChange: (Boolean) -> Unit = {},
    onPageSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentPage by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(initialPage) }
    var pdfTotalPages by remember { mutableIntStateOf(0) }
    val displayTotalPages = if (pdfPath == "blank_note") totalPages else pdfTotalPages
    
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    val pdfViewRef = remember { mutableStateOf<PDFView?>(null) }

    val isContentUri = pdfPath.startsWith("content://")
    val fileExists = if (isContentUri) true else (pdfPath.isNotEmpty() && File(pdfPath).exists())
    
    key(pdfPath) {
        var loadError by remember { mutableStateOf<String?>(null) }
        
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // ── PROFESSIONAL CONTENT VALIDATION ──
            val shouldRenderPdf = (pdfPath == "blank_note" || (pdfPath.isNotEmpty() && (isContentUri || fileExists))) && loadError == null
            if (!shouldRenderPdf) {
                when (pdfDownloadState) {
                    is com.pulse.data.repository.PdfDownloadState.Downloading -> {
                        PdfDownloadProgressOverlay(state = pdfDownloadState)
                    }
                    is com.pulse.data.repository.PdfDownloadState.Error -> {
                        PdfDownloadErrorOverlay(
                            message = pdfDownloadState.message,
                            onRetry = onAddDrivePdf 
                        )
                    }
                    else -> {
                        PdfPlaceholder(
                            pdfPath = pdfPath,
                            onAddLocalPdf = onAddLocalPdf,
                            onAddDrivePdf = onAddDrivePdf,
                            onCreateBlankNote = onCreateBlankNote
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (pdfPath == "blank_note") {
                        NotebookMode(
                            title = title,
                            totalPages = totalPages,
                            currentPage = currentPage,
                            isHorizontal = isHorizontal,
                            annotationState = annotationState,
                            visuals = visuals,
                            onPageChanged = { currentPage = it; onPageChanged(it) },
                            onAddVisual = onAddVisual,
                            onAddVisualAtPos = onAddVisualAtPos,
                            onDeleteVisual = onDeleteVisual
                        )
                    } else if (fileExists || isContentUri) {
                        PdfMode(
                            pdfPath = pdfPath,
                            initialPage = currentPage, // Use saved currentPage
                            isHorizontal = isHorizontal,
                            isContentUri = isContentUri,
                            annotationState = annotationState,
                            visuals = visuals,
                            currentPage = currentPage,
                            onStatusUpdate = { p, total ->
                                currentPage = p
                                pdfTotalPages = total
                                onPageChanged(p)
                            },
                            onAddVisual = onAddVisual,
                            onAddVisualAtPos = onAddVisualAtPos,
                            onDeleteVisual = onDeleteVisual,
                            onError = { loadError = it },
                            pdfViewRef = pdfViewRef
                        )
                    } else {
                        PdfPlaceholder(
                            pdfPath = pdfPath,
                            onAddLocalPdf = onAddLocalPdf,
                            onAddDrivePdf = onAddDrivePdf,
                            onCreateBlankNote = onCreateBlankNote
                        )
                    }
                }

                // ── INDUSTRY OVERLAYS ──
                PdfSettingsMenu(
                    state = annotationState,
                    onClose = onClose,
                    isHorizontal = isHorizontal,
                    onOrientationChange = onOrientationChange,
                    onAddDrivePdf = onAddDrivePdf,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                )

                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    AnnotationToggleButton(
                        isEditing = annotationState.isDrawingMode,
                        onClick = { annotationState.isDrawingMode = !annotationState.isDrawingMode }
                    )
                    
                    // Spacer for the FloatingActionButton in LectureScreen
                    Spacer(modifier = Modifier.height(72.dp)) 
                }

                PdfPageIndicator(
                    currentPage = currentPage,
                    totalPages = displayTotalPages,
                    isNotebook = pdfPath == "blank_note",
                    onAddPage = onAddPage,
                    onClick = { 
                        pageInput = (currentPage + 1).toString()
                        showGoToPageDialog = true 
                    },
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = annotationState.isDrawingMode,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically { it },
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnnotationToolbar(
                        state = annotationState,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // ── PAGE NAV DIALOG ──
            if (showGoToPageDialog) {
                GoToPageDialog(
                    pageInput = pageInput,
                    maxPage = displayTotalPages,
                    onInputChanged = { pageInput = it },
                    onConfirm = { target ->
                        if (pdfPath == "blank_note") {
                            currentPage = target - 1
                            onPageChanged(currentPage)
                        } else {
                            pdfViewRef.value?.jumpTo(target - 1)
                        }
                        showGoToPageDialog = false
                    },
                    onDismiss = { showGoToPageDialog = false }
                )
            }
        }
    }
}

@Composable
private fun PdfPlaceholder(
    pdfPath: String,
    onAddLocalPdf: () -> Unit,
    onAddDrivePdf: () -> Unit,
    onCreateBlankNote: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (pdfPath.isEmpty()) "Ready to add content" else "Unable to open PDF document",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))
        
        // Local PDF Button
        Button(
            onClick = onAddLocalPdf,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Select PDF")
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Blank Note Button
        OutlinedButton(
            onClick = onCreateBlankNote,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Blank Note")
        }

        Spacer(Modifier.height(32.dp))

        // ── DRIVE PDF BUTTON (Requested Location) ──
        TextButton(
            onClick = onAddDrivePdf,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "PDF from Drive",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NotebookMode(
    title: String,
    totalPages: Int,
    currentPage: Int,
    isHorizontal: Boolean,
    annotationState: AnnotationState,
    visuals: List<NoteVisual>,
    onPageChanged: (Int) -> Unit,
    onAddVisual: (VisualType, String, Int, Int, Float, Float) -> Unit,
    onAddVisualAtPos: (VisualType, Float, Float, Int, Int, Float, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))) { totalPages }
    
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        LaunchedEffect(maxWidth, maxHeight) {
            annotationState.pageWidth = with(density) { maxWidth.toPx() }
            annotationState.pageHeight = with(density) { maxHeight.toPx() }
            annotationState.resetTransform()
        }

        if (isHorizontal) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !annotationState.isDrawingMode,
                modifier = Modifier.fillMaxSize().background(Color.White)
            ) { pageIndex ->
                NotebookPage(title, pageIndex, annotationState, visuals, onAddVisual, onAddVisualAtPos, onDeleteVisual)
            }
        } else {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = !annotationState.isDrawingMode,
                modifier = Modifier.fillMaxSize().background(Color.White)
            ) { pageIndex ->
                NotebookPage(title, pageIndex, annotationState, visuals, onAddVisual, onAddVisualAtPos, onDeleteVisual)
            }
        }
    }
}

@Composable
private fun NotebookPage(
    title: String,
    pageIndex: Int,
    annotationState: AnnotationState,
    visuals: List<NoteVisual>,
    onAddVisual: (VisualType, String, Int, Int, Float, Float) -> Unit,
    onAddVisualAtPos: (VisualType, Float, Float, Int, Int, Float, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        DrawingCanvas(
            annotationState = annotationState,
            visuals = visuals.filter { it.pageNumber == pageIndex },
            onDrawComplete = { type, data, color, width, alpha ->
                onAddVisual(type, data, pageIndex, color.toArgb(), width, alpha)
            },
            onAddVisualAtPos = { type, x, y, color, width, alpha ->
                onAddVisualAtPos(type, x, y, pageIndex, color, width, alpha)
            },
            onDeleteVisual = onDeleteVisual,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PdfMode(
    pdfPath: String,
    initialPage: Int,
    isHorizontal: Boolean,
    isContentUri: Boolean,
    annotationState: AnnotationState,
    visuals: List<NoteVisual>,
    currentPage: Int,
    onStatusUpdate: (Int, Int) -> Unit,
    onAddVisual: (VisualType, String, Int, Int, Float, Float) -> Unit,
    onAddVisualAtPos: (VisualType, Float, Float, Int, Int, Float, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    onError: (String) -> Unit,
    pdfViewRef: MutableState<PDFView?>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // ── NATIVE-OVERLAY INTEGRATION ──
    val composeOverlay = remember { 
        androidx.compose.ui.platform.ComposeView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        } 
    }

    // Track container size to force remapping on resize
    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }
    
    // ── RECOMPOSITION TRIGGER: Observe tool changes to refresh overlay logic ──
    val activeTool = annotationState.currentTool
    val activeMode = annotationState.isDrawingMode

    // Sync the Compose overlay content
    // ── REACTIVE STATE HOLDERS for the overlay's independent Compose tree ──
    val overlayVisuals = remember { mutableStateOf<List<NoteVisual>>(emptyList()) }
    val overlayPage = remember { mutableIntStateOf(currentPage) }
    val overlayPdfView = remember { mutableStateOf(pdfViewRef.value) }

    // Keep overlay state in sync with parent composition reactively
    SideEffect {
        overlayVisuals.value = visuals.filter { it.pageNumber == currentPage }
        overlayPage.intValue = currentPage
        overlayPdfView.value = pdfViewRef.value
    }

    LaunchedEffect(Unit) {
        composeOverlay.setContent {
            DrawingCanvas(
                annotationState = annotationState,
                pdfView = overlayPdfView.value,
                visuals = overlayVisuals.value,
                onDrawComplete = { type, data, color, width, alpha ->
                    onAddVisual(type, data, overlayPage.intValue, color.toArgb(), width, alpha)
                },
                onAddVisualAtPos = { type, x, y, color, width, alpha ->
                    onAddVisualAtPos(type, x, y, overlayPage.intValue, color, width, alpha)
                },
                onDeleteVisual = onDeleteVisual,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Coordinate mappers — always read pdfView dimensions live at call time.
    // Re-installed whenever pdfView ref changes OR container resizes so closures stay fresh.
    LaunchedEffect(pdfViewRef.value, containerWidth, containerHeight) {
        pdfViewRef.value?.let { pdfView ->
            annotationState.pdfToScreenMapper = { x, y ->
                // Read width at invocation time, not capture time
                val w = pdfView.width.toFloat().takeIf { it > 0 } ?: 1f
                android.graphics.PointF(
                    (x * w) * pdfView.zoom + pdfView.currentXOffset,
                    (y * w) * pdfView.zoom + pdfView.currentYOffset
                )
            }
            annotationState.screenToPdfMapper = { x, y ->
                val w = pdfView.width.toFloat().takeIf { it > 0 } ?: 1f
                android.graphics.PointF(
                    ((x - pdfView.currentXOffset) / pdfView.zoom) / w,
                    ((y - pdfView.currentYOffset) / pdfView.zoom) / w
                )
            }
            annotationState.invalidationTick++
        }
    }

    // Reload on orientation change without destroying the native view
    LaunchedEffect(isHorizontal) {
        pdfViewRef.value?.let { pdfView ->
            loadPdfInternal(
                pdfView = pdfView,
                path = pdfPath,
                page = currentPage,
                isHorizontal = isHorizontal,
                isContentUri = isContentUri,
                onError = onError,
                onStatusUpdate = onStatusUpdate
            )
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val frameLayout = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val pdfView = PDFView(ctx, null).apply {
                id = android.view.View.generateViewId()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isFocusableInTouchMode = true
            }
            pdfViewRef.value = pdfView
            
            // Force Canvas recomposition on every PDFView draw (zoom/pan/scroll)
            pdfView.viewTreeObserver.addOnDrawListener {
                annotationState.invalidationTick++
            }

            // ── RESIZE FIX: Re-map coordinates when container layout changes ──
            frameLayout.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val newW = right - left
                val newH = bottom - top
                val oldW = oldRight - oldLeft
                val oldH = oldBottom - oldTop
                if (newW != oldW || newH != oldH) {
                    containerWidth = newW
                    containerHeight = newH
                    // Force the overlay to re-layout to match new container size
                    composeOverlay.requestLayout()
                    annotationState.invalidationTick++
                }
            }

            frameLayout.addView(pdfView)
            frameLayout.addView(composeOverlay)
            composeOverlay.setOnTouchListener { _, _ -> false }

            loadPdfInternal(
                pdfView = pdfView,
                path = pdfPath,
                page = currentPage,
                isHorizontal = isHorizontal,
                isContentUri = isContentUri,
                onError = onError,
                onStatusUpdate = onStatusUpdate
            )
            frameLayout
        },
        update = { frameLayout ->
            val drawingActive = annotationState.isDrawingMode
            val overlay = frameLayout.getChildAt(1) as? androidx.compose.ui.platform.ComposeView
            overlay?.apply {
                isEnabled = drawingActive
                isClickable = drawingActive
                isFocusable = drawingActive
                setOnTouchListener { _, _ -> false }
            }
        },
        onRelease = {
            pdfViewRef.value?.recycle()
            pdfViewRef.value = null
        }
    )
}

@Composable
private fun GoToPageDialog(
    pageInput: String,
    maxPage: Int,
    onInputChanged: (String) -> Unit,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to Page") },
        text = {
            Column {
                Text("Enter page number (1-$maxPage)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = pageInput,
                    onValueChange = { onInputChanged(it.filter { c -> c.isDigit() }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = pageInput.toIntOrNull()
                if (target != null && target in 1..maxPage) {
                    onConfirm(target)
                }
                onDismiss()
            }) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun loadPdfInternal(
    pdfView: PDFView,
    path: String,
    page: Int,
    isHorizontal: Boolean,
    isContentUri: Boolean,
    onError: (String) -> Unit,
    onStatusUpdate: (Int, Int) -> Unit
) {
    pdfView.minZoom = 0.7f
    pdfView.midZoom = 1.75f
    pdfView.maxZoom = 5.0f
    
    val config = if (isContentUri) pdfView.fromUri(Uri.parse(path)) else pdfView.fromFile(File(path))
    
    config
        .defaultPage(page)
        .enableSwipe(true)
        .swipeHorizontal(isHorizontal)
        .enableDoubletap(true)
        .enableAntialiasing(true)
        .spacing(8)
        .scrollHandle(DefaultScrollHandle(pdfView.context))
        .onPageChange { p, total -> onStatusUpdate(p, total) }
        .onError { it -> onError("Failed to load PDF: ${it.message}") }
        .pageFitPolicy(FitPolicy.WIDTH)
        .fitEachPage(true)
        .load()
}

@Composable
fun PdfDownloadProgressOverlay(state: com.pulse.data.repository.PdfDownloadState.Downloading) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Downloading Document...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.width(280.dp)
                .height(8.dp)
                .clipToBounds(),
        )
        
        Spacer(Modifier.height(16.dp))
        
        val speedMb = state.speedBytesPerSec / (1024.0 * 1024.0)
        val downloadedMb = state.downloadedBytes / (1024.0 * 1024.0)
        val totalMb = state.totalBytes / (1024.0 * 1024.0)
        
        val progressText = if (state.totalBytes > 0) {
            String.format("%.1f MB / %.1f MB (%.1f MB/s)", downloadedMb, totalMb, speedMb)
        } else {
            String.format("%.1f MB downloaded (%.1f MB/s)", downloadedMb, speedMb)
        }
        
        Text(
            text = progressText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PdfDownloadErrorOverlay(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Download Failed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry Download")
        }
    }
}
