package com.pulse.presentation.lecture

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.io.File

import com.pulse.core.data.db.NoteVisual
import com.pulse.core.data.db.VisualType
import com.pulse.core.presentation.components.DrawingCanvas
import androidx.compose.ui.graphics.toArgb

@Composable
fun PdfViewer(
    pdfPath: String,
    title: String,
    initialPage: Int,
    totalPages: Int = 0,
    isPdfDownloaded: Boolean,
    visuals: List<NoteVisual>,
    annotationState: AnnotationState,
    onPageChanged: (Int) -> Unit,
    onAddVisual: (VisualType, String, Int, Int, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    onCreateBlankNote: () -> Unit = {},
    onAddPage: () -> Unit = {},
    onAddLocalPdf: () -> Unit = {},
    onClose: () -> Unit = {},
    isHorizontal: Boolean = true,
    onOrientationChange: (Boolean) -> Unit = {},
    onPageSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(initialPage) }
    var pdfTotalPages by remember { mutableIntStateOf(0) }
    val displayTotalPages = if (pdfPath == "blank_note") totalPages else pdfTotalPages
    
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    val pdfViewRef = remember { mutableStateOf<PDFView?>(null) }

    val isContentUri = pdfPath.startsWith("content://")
    val fileExists = if (isContentUri) true else File(pdfPath).exists()
    var loadError by remember { mutableStateOf<String?>(null) }
    
    // Using key(pdfPath, isHorizontal) forces Compose to completely demount and recreate the internal 
    // AndroidView whenever the path OR orientation changes, ensuring settings are applied fresh.
    key(pdfPath, isHorizontal) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (pdfPath != "blank_note" && (pdfPath.isEmpty() || (!isContentUri && !fileExists))) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isPdfDownloaded) "PDF file not found" else "No PDF attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onAddLocalPdf,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Local PDF")
                    }
                    OutlinedButton(
                        onClick = onCreateBlankNote,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Blank Note")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (pdfPath == "blank_note") {
                        // ── BLANK NOTEBOOK MODE ──
                        val pagerState = rememberPagerState(initialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))) { totalPages }
                        LaunchedEffect(pagerState.currentPage) {
                            currentPage = pagerState.currentPage
                            onPageChanged(currentPage)
                        }
                        // Support jumping to page
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
                                annotationState.currentZoom = 1f
                                annotationState.currentXOffset = 0f
                                annotationState.currentYOffset = 0f
                            }
                            
                        if (isHorizontal) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = !annotationState.isDrawingMode,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            ) { pageIndex ->
                                Box(Modifier.fillMaxSize()) {
                                    // ── NATIVE NOTEBOOK PAGE ──
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(Modifier.height(40.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontWeight = FontWeight.Light,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
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

                                    // ── PAGE SPECIFIC DRAWINGS ──
                                    DrawingCanvas(
                                        annotationState = annotationState,
                                        visuals = visuals.filter { it.pageNumber == pageIndex },
                                        onDrawComplete = { type, data, color, width ->
                                            onAddVisual(type, data, pageIndex, color.toArgb(), width)
                                        },
                                        onDeleteVisual = onDeleteVisual,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            VerticalPager(
                                state = pagerState,
                                userScrollEnabled = !annotationState.isDrawingMode,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            ) { pageIndex ->
                                Box(Modifier.fillMaxSize()) {
                                    // ── NATIVE NOTEBOOK PAGE ──
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(Modifier.height(40.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontWeight = FontWeight.Light,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
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

                                    // ── PAGE SPECIFIC DRAWINGS ──
                                    DrawingCanvas(
                                        annotationState = annotationState,
                                        visuals = visuals.filter { it.pageNumber == pageIndex },
                                        onDrawComplete = { type, data, color, width ->
                                            onAddVisual(type, data, pageIndex, color.toArgb(), width)
                                        },
                                        onDeleteVisual = onDeleteVisual,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        }
                    } else {
                        // ── PDF MODE ──
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
                                }
                                
                                pdfViewRef.value = pdfView
                                frameLayout.addView(pdfView)
                                
                                loadPdfIntoView(
                                    pdfView = pdfView,
                                    path = pdfPath,
                                    page = initialPage,
                                    isHorizontal = isHorizontal,
                                    isContentUri = isContentUri,
                                    onError = { loadError = it },
                                    onStatusUpdate = { p, total ->
                                        currentPage = p
                                        pdfTotalPages = total
                                        onPageChanged(p)
                                    },
                                    onTransformUpdate = { zoom, xOff, yOff, w, h ->
                                        annotationState.currentZoom = zoom
                                        annotationState.currentXOffset = xOff
                                        annotationState.currentYOffset = yOff
                                        annotationState.pageWidth = w
                                        annotationState.pageHeight = h
                                    }
                                )
                                frameLayout
                            },
                            update = { view ->
                                 val pdfView = (view as? FrameLayout)?.getChildAt(0) as? PDFView
                                 pdfView?.let { pv ->
                                     try {
                                         pv.setSwipeEnabled(!annotationState.isDrawingMode)
                                     } catch (e: Exception) {}
                                 }
                            },
                            onRelease = { view: android.widget.FrameLayout ->
                                val pdfView = view.getChildAt(0) as? PDFView
                                pdfView?.recycle()
                                pdfViewRef.value = null
                            }
                        )
                    }

                    loadError?.let { error ->
                        androidx.compose.material3.Text(
                            text = error,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp).align(Alignment.Center)
                        )
                    }

                    // Global DrawingCanvas only for PDF Mode (Notebook uses per-page canvas)
                    if (pdfPath != "blank_note") {
                        DrawingCanvas(
                            annotationState = annotationState,
                            visuals = visuals.filter { it.pageNumber == currentPage },
                            onDrawComplete = { type, data, color, width ->
                                onAddVisual(type, data, currentPage, color.toArgb(), width)
                            },
                            onDeleteVisual = onDeleteVisual,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // New Industry-Standard Settings Menu (Gear Icon)
                PdfSettingsMenu(
                    state = annotationState,
                    onClose = onClose,
                    isHorizontal = isHorizontal,
                    onOrientationChange = onOrientationChange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )

                // Page Indicator
                Surface(
                    onClick = { 
                        pageInput = (currentPage + 1).toString()
                        showGoToPageDialog = true 
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${currentPage + 1} / $displayTotalPages",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        if (pdfPath == "blank_note") {
                            IconButton(
                                onClick = onAddPage,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add 10 Pages",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Go to Page Dialog
            if (showGoToPageDialog) {
                AlertDialog(
                    onDismissRequest = { showGoToPageDialog = false },
                    title = { Text("Go to Page") },
                    text = {
                        Column {
                            Text("Enter page number (1-$displayTotalPages)", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            TextField(
                                value = pageInput,
                                onValueChange = { pageInput = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val target = pageInput.toIntOrNull()
                            if (target != null && target in 1..displayTotalPages) {
                                if (pdfPath == "blank_note") {
                                    currentPage = target - 1
                                    onPageChanged(currentPage)
                                } else {
                                    pdfViewRef.value?.jumpTo(target - 1)
                                }
                            }
                            showGoToPageDialog = false
                        }) {
                            Text("Go")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGoToPageDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Compact Settings Suite for PDF Annotations.
 */
@Composable
fun PdfSettingsMenu(
    state: AnnotationState,
    onClose: () -> Unit,
    isHorizontal: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val tools = (if (state.isDrawingMode) listOf(
        VisualType.DRAWING to Icons.Default.Edit,
        VisualType.HIGHLIGHT to Icons.Default.Gesture,
        VisualType.ERASER to Icons.Default.Delete
    ) else emptyList<Pair<VisualType, androidx.compose.ui.graphics.vector.ImageVector>>())

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = if (state.isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = if (state.isDrawingMode) Icons.Default.Edit else Icons.Default.Settings,
                contentDescription = "PDF Settings"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            // 1. Toggle Annotation Mode
            DropdownMenuItem(
                text = { Text(if (state.isDrawingMode) "Disable Annotations" else "Enable Annotation Mode") },
                onClick = { 
                    state.isDrawingMode = !state.isDrawingMode
                    expanded = false
                },
                leadingIcon = { 
                    Icon(if (state.isDrawingMode) Icons.Default.Close else Icons.Default.Edit, contentDescription = null) 
                }
            )

            if (state.isDrawingMode) {
                Divider()
                // 2. Tool Picker
                Text("Tools", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    tools.forEach { (type, icon) ->
                        FilterChip(
                            selected = state.activeTool == type,
                            onClick = { state.activeTool = type },
                            label = { Icon(icon, null, Modifier.size(16.dp)) }
                        )
                    }
                }

                Divider()
                // 3. Color Picker (Compact)
                Text("Colors", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black)
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { color ->
                        Surface(
                            onClick = { state.strokeColor = color },
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = color,
                            border = if (state.strokeColor == color) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null
                        ) {}
                    }
                }

                Divider()
                // 4. Stroke Width
                Text("Stroke Width: ${state.strokeWidth.toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                Slider(
                    value = state.strokeWidth,
                    onValueChange = { state.strokeWidth = it },
                    valueRange = 2f..20f,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Divider()

            // 5. Orientation Toggle
            DropdownMenuItem(
                text = { Text(if (isHorizontal) "Vertical Scroll" else "Horizontal Scroll") },
                onClick = { 
                    onOrientationChange(!isHorizontal)
                    expanded = false
                },
                leadingIcon = { 
                    Icon(if (isHorizontal) Icons.Default.ViewDay else Icons.Default.ViewWeek, contentDescription = null) 
                }
            )

            Divider()

            // 6. Global Actions
            DropdownMenuItem(
                text = { Text("Close PDF", color = MaterialTheme.colorScheme.error) },
                onClick = { 
                    onClose()
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

private fun loadPdfIntoView(
    pdfView: PDFView,
    path: String,
    page: Int,
    isHorizontal: Boolean,
    isContentUri: Boolean,
    onError: (String) -> Unit,
    onStatusUpdate: (Int, Int) -> Unit,
    onTransformUpdate: (zoom: Float, xOff: Float, yOff: Float, pageWidth: Float, pageHeight: Float) -> Unit
) {
    val context = pdfView.context
    
    // Set zoom levels on the view directly as they aren't on the configurator
    pdfView.minZoom = 0.7f
    pdfView.midZoom = 1.75f
    pdfView.maxZoom = 5.0f
    
    val config = if (isContentUri) {
        pdfView.fromUri(Uri.parse(path))
    } else {
        pdfView.fromFile(File(path))
    }
    
    config
        .defaultPage(page)
        .enableSwipe(true)
        .swipeHorizontal(isHorizontal)
        .enableDoubletap(true)
        .enableAntialiasing(true)
        .nightMode(false)
        .spacing(8)
        .scrollHandle(DefaultScrollHandle(context))
        .onPageChange { p, total -> onStatusUpdate(p, total) }
        .onPageScroll { _, _ -> 
            val zoom = pdfView.zoom
            val pageWidth = pdfView.width * zoom // This is an estimate, onDraw is better
            val pageHeight = pdfView.height * zoom
            // Use current offsets as base
            onTransformUpdate(zoom, pdfView.currentXOffset, pdfView.currentYOffset, pageWidth, pageHeight)
        }
        .onDraw { _, pageWidth, pageHeight, displayedPage ->
            if (displayedPage == pdfView.currentPage) {
                // IMPORTANT: In PDFView, currentXOffset is the document translation.
                // If a page is smaller than the view, it's centered. 
                // We need to add that centering offset to our annotation anchor.
                val centerX = (pdfView.width - pageWidth).coerceAtLeast(0f) / 2f
                val centerY = (pdfView.height - pageHeight).coerceAtLeast(0f) / 2f
                
                onTransformUpdate(
                    pdfView.zoom, 
                    pdfView.currentXOffset + centerX, 
                    pdfView.currentYOffset + centerY, 
                    pageWidth, 
                    pageHeight
                )
            }
        }
        .onError { t -> 
            t.printStackTrace()
            onError("Failed to load PDF: ${t.message}")
        }
        .pageFitPolicy(FitPolicy.WIDTH)
        .fitEachPage(true)
        .load()
}
