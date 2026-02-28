import com.pulse.presentation.lecture.components.PdfPageIndicator
import com.pulse.presentation.lecture.components.PdfSettingsMenu

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
    
    key(pdfPath, isHorizontal) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // ── PROFESSIONAL CONTENT VALIDATION ──
            val shouldRenderPdf = pdfPath == "blank_note" || (pdfPath.isNotEmpty() && (isContentUri || fileExists || pdfPath.contains("/data/user/0/")))
            
            if (!shouldRenderPdf) {
                PdfPlaceholder(
                    pdfPath = pdfPath,
                    onAddLocalPdf = onAddLocalPdf,
                    onCreateBlankNote = onCreateBlankNote
                )
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
                            onDeleteVisual = onDeleteVisual
                        )
                    } else {
                        PdfMode(
                            pdfPath = pdfPath,
                            initialPage = initialPage,
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
                            onDeleteVisual = onDeleteVisual,
                            onError = { loadError = it },
                            pdfViewRef = pdfViewRef
                        )
                    }
                }

                loadError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp).align(Alignment.Center)
                    )
                }

                // ── INDUSTRY OVERLAYS ──
                PdfSettingsMenu(
                    state = annotationState,
                    onClose = onClose,
                    isHorizontal = isHorizontal,
                    onOrientationChange = onOrientationChange,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                )

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
            text = if (pdfPath.isEmpty()) "Loading PDF content..." else "PDF document not ready",
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
            Text("Select PDF File")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCreateBlankNote,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Blank Note")
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
    onAddVisual: (VisualType, String, Int, Int, Float) -> Unit,
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
            annotationState.currentZoom = 1f
            annotationState.currentXOffset = 0f
            annotationState.currentYOffset = 0f
        }

        if (isHorizontal) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !annotationState.isDrawingMode,
                modifier = Modifier.fillMaxSize().background(Color.White)
            ) { pageIndex ->
                NotebookPage(title, pageIndex, annotationState, visuals, onAddVisual, onDeleteVisual)
            }
        } else {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = !annotationState.isDrawingMode,
                modifier = Modifier.fillMaxSize().background(Color.White)
            ) { pageIndex ->
                NotebookPage(title, pageIndex, annotationState, visuals, onAddVisual, onDeleteVisual)
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
    onAddVisual: (VisualType, String, Int, Int, Float) -> Unit,
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
    onAddVisual: (VisualType, String, Int, Int, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    onError: (String) -> Unit,
    pdfViewRef: MutableState<PDFView?>
) {
    Box(Modifier.fillMaxSize()) {
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
                
                loadPdfInternal(
                    pdfView = pdfView,
                    path = pdfPath,
                    page = initialPage,
                    isHorizontal = isHorizontal,
                    isContentUri = isContentUri,
                    onError = onError,
                    onStatusUpdate = onStatusUpdate,
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
                 pdfView?.setSwipeEnabled(!annotationState.isDrawingMode)
            },
            onRelease = { view ->
                (view.getChildAt(0) as? PDFView)?.recycle()
                pdfViewRef.value = null
            }
        )

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
    onStatusUpdate: (Int, Int) -> Unit,
    onTransformUpdate: (zoom: Float, xOff: Float, yOff: Float, pageWidth: Float, pageHeight: Float) -> Unit
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
        .onPageScroll { _, _ -> 
            val z = pdfView.zoom
            onTransformUpdate(z, pdfView.currentXOffset, pdfView.currentYOffset, pdfView.width * z, pdfView.height * z)
        }
        .onDraw { _, pw, ph, dp ->
            if (dp == pdfView.currentPage) {
                val cx = (pdfView.width - pw).coerceAtLeast(0f) / 2f
                val cy = (pdfView.height - ph).coerceAtLeast(0f) / 2f
                onTransformUpdate(pdfView.zoom, pdfView.currentXOffset + cx, pdfView.currentYOffset + cy, pw, ph)
            }
        }
        .onError { onError("Failed to load PDF: ${it.message}") }
        .pageFitPolicy(FitPolicy.WIDTH)
        .fitEachPage(true)
        .load()
}
