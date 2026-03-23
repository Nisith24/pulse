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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

// ── Content Mode: Strong typing replaces "blank_note" magic string pattern ──
private enum class ContentMode { BLANK_NOTE, PDF_FILE, CONTENT_URI, PLACEHOLDER }

private fun resolveContentMode(pdfPath: String): ContentMode = when {
    pdfPath == "blank_note" -> ContentMode.BLANK_NOTE
    pdfPath.startsWith("content://") -> ContentMode.CONTENT_URI
    pdfPath.isNotEmpty() && File(pdfPath).exists() -> ContentMode.PDF_FILE
    else -> ContentMode.PLACEHOLDER
}

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
    val contentMode = remember(pdfPath) { resolveContentMode(pdfPath) }
    val displayTotalPages = if (contentMode == ContentMode.BLANK_NOTE) totalPages else pdfTotalPages

    var showGoToPageDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    val pdfViewRef = remember { mutableStateOf<PDFView?>(null) }

    key(pdfPath) {
        var loadError by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val shouldRenderContent = contentMode != ContentMode.PLACEHOLDER && loadError == null
            if (!shouldRenderContent) {
                // Show download/error/placeholder overlays
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
                    when (contentMode) {
                        ContentMode.BLANK_NOTE -> {
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
                        }
                        ContentMode.PDF_FILE, ContentMode.CONTENT_URI -> {
                            PdfMode(
                                pdfPath = pdfPath,
                                initialPage = currentPage,
                                isHorizontal = isHorizontal,
                                isContentUri = contentMode == ContentMode.CONTENT_URI,
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
                        }
                        ContentMode.PLACEHOLDER -> { /* handled above */ }
                    }
                }

                // ── Overlays ──
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
                    isNotebook = contentMode == ContentMode.BLANK_NOTE,
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

            // ── Page Navigation Dialog ──
            if (showGoToPageDialog) {
                GoToPageDialog(
                    pageInput = pageInput,
                    maxPage = displayTotalPages,
                    onInputChanged = { pageInput = it },
                    onConfirm = { target ->
                        if (contentMode == ContentMode.BLANK_NOTE) {
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

// ── Placeholder ──

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

        Button(
            onClick = onAddLocalPdf,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Select PDF")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCreateBlankNote,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Blank Note")
        }

        Spacer(Modifier.height(32.dp))

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

// ── Notebook Mode ──

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
    val safeInitialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitialPage) { totalPages }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage && currentPage in 0 until totalPages) {
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

        val pagerContent: @Composable (Int) -> Unit = { pageIndex ->
            NotebookPage(title, pageIndex, annotationState, visuals, onAddVisual, onAddVisualAtPos, onDeleteVisual)
        }

        if (isHorizontal) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !annotationState.isDrawingMode,
                modifier = Modifier.fillMaxSize().background(Color.White),
                key = { it }
            ) { pagerContent(it) }
        } else {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = !annotationState.isDrawingMode,
                modifier = Modifier.fillMaxSize().background(Color.White),
                key = { it }
            ) { pagerContent(it) }
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
    // derivedStateOf avoids re-filtering on every recomposition
    val pageVisuals by remember(visuals, pageIndex) {
        derivedStateOf { visuals.filter { it.pageNumber == pageIndex } }
    }

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
            visuals = pageVisuals,
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

// ── PDF Mode (Native PDFView + Compose Overlay) ──

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

    // Compose overlay for drawing on top of native PDFView
    val composeOverlay = remember {
        androidx.compose.ui.platform.ComposeView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }
    }

    // Track container size for coordinate remapping
    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }

    // Pre-compute page visuals to avoid re-filtering on every tick
    val pageVisuals by remember(visuals, currentPage) {
        derivedStateOf { visuals.filter { it.pageNumber == currentPage } }
    }
    val currentPdfViewSnapshot = pdfViewRef.value

    // Set overlay content when visuals, page, or pdfView changes
    LaunchedEffect(pageVisuals, currentPage, currentPdfViewSnapshot) {
        composeOverlay.setContent {
            DrawingCanvas(
                annotationState = annotationState,
                pdfView = currentPdfViewSnapshot,
                visuals = pageVisuals,
                onDrawComplete = { type, data, color, width, alpha ->
                    onAddVisual(type, data, currentPage, color.toArgb(), width, alpha)
                },
                onAddVisualAtPos = { type, x, y, color, width, alpha ->
                    onAddVisualAtPos(type, x, y, currentPage, color, width, alpha)
                },
                onDeleteVisual = onDeleteVisual,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Install coordinate mappers when pdfView or container size changes
    LaunchedEffect(pdfViewRef.value, containerWidth, containerHeight) {
        pdfViewRef.value?.let { pdfView ->
            annotationState.pdfToScreenMapper = { x, y ->
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

    // Reload PDF on orientation change
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

            // Re-map coordinates when container layout changes
            frameLayout.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val newW = right - left
                val newH = bottom - top
                if (newW != oldRight - oldLeft || newH != oldBottom - oldTop) {
                    containerWidth = newW
                    containerHeight = newH
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

// ── Go To Page Dialog ──

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

// ── PDF Loading ──

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
        .onError { e -> onError("Failed to load PDF: ${e.message}") }
        .pageFitPolicy(FitPolicy.WIDTH)
        .fitEachPage(true)
        .load()
}

// ── Download Progress Overlay ──

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

// ── Download Error Overlay ──

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
