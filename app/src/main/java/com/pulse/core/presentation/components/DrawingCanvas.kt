package com.pulse.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.*
import com.pulse.core.data.db.NoteVisual
import com.pulse.core.data.db.VisualType
import com.pulse.presentation.lecture.AnnotationState

@Composable
fun DrawingCanvas(
    annotationState: AnnotationState,
    pdfView: com.github.barteksc.pdfviewer.PDFView? = null,
    visuals: List<NoteVisual>,
    onDrawComplete: (VisualType, String, Color, Float, Float) -> Unit,
    onAddVisualAtPos: (VisualType, Float, Float, Int, Float, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // ── PERFORMANCE OPTIMIZATION: PRE-PARSED VISUALS & CACHED PATHS ──
    // We parse strings and pre-build Path objects once per change.
    val parsedVisualMap = remember(visuals) {
        visuals.associate { visual ->
            val result: Any? = if (visual.type == VisualType.TEXT || visual.type == VisualType.STICKY_NOTE) {
                val parts = visual.data.split(",")
                if (parts.size >= 2) Offset(parts[0].toFloatOrNull() ?: 0f, parts[1].toFloatOrNull() ?: 0f) else null
            } else {
                val points = visual.data.split(";").mapNotNull { pointStr ->
                    val coords = pointStr.split(",")
                    if (coords.size >= 2) {
                        Triple(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f, if (coords.size >= 3) coords[2].toFloatOrNull() ?: 1f else 1f)
                    } else null
                }
                if (points.isEmpty()) null
                else if (visual.type == VisualType.DRAWING || visual.type == VisualType.HIGHLIGHT) {
                    // Pre-calculate normalized path (we'll scale it during draw)
                    points
                } else points // BOX / RULER store point list
            }
            visual.id to result
        }
    }

    // ── STALE STATE FIX: Always read latest variables in pointerInput ──
    val currentVisuals by rememberUpdatedState(visuals)
    val currentPdfView by rememberUpdatedState(pdfView)
    val currentOnDrawComplete by rememberUpdatedState(onDrawComplete)
    val currentOnAddVisualAtPos by rememberUpdatedState(onAddVisualAtPos)
    val currentOnDeleteVisual by rememberUpdatedState(onDeleteVisual)

    var currentPathPoints = remember { mutableStateListOf<Triple<Float, Float, Float>>() }

    // Read currentTool directly from annotationState — no local copy to avoid stale state
    // after toolbar close/reopen. A local `effectiveTool` is used only within each gesture
    // to handle hardware eraser override.
    var effectiveTool by remember { mutableStateOf(annotationState.currentTool) }

    var eraserPosition by remember { mutableStateOf<Offset?>(null) }
    
    // Batched deletions for "less write"
    val pendingDeletions = remember { mutableStateOf(setOf<Long>()) }

    // Keep effectiveTool in sync with the toolbar selection between gestures
    LaunchedEffect(annotationState.currentTool) {
        effectiveTool = annotationState.currentTool
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (annotationState.isDrawingMode) {
                    Modifier.pointerInput(annotationState.isDrawingMode, annotationState.currentTool) {
                        forEachGesture {
                            awaitPointerEventScope {
                                var isMultiTouch = false
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var activePointerId = down.id
                                
                                val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                                val isEraserHardware = down.type == PointerType.Eraser
                                // Determine the tool for THIS gesture: hardware eraser overrides,
                                // otherwise always read the latest from annotationState
                                val gestureTool = if (isEraserHardware) VisualType.ERASER else annotationState.currentTool
                                effectiveTool = gestureTool

                                // Initial action
                                if (gestureTool == VisualType.ERASER) {
                                    eraserPosition = down.position
                                    findAndCollectHits(down.position, annotationState, currentPdfView, currentVisuals, pendingDeletions)
                                } else if (gestureTool == VisualType.TEXT || gestureTool == VisualType.STICKY_NOTE) {
                                    val pdfPoint = annotationState.viewToPage(down.position.x, down.position.y)
                                    currentOnAddVisualAtPos(gestureTool, pdfPoint.x, pdfPoint.y, annotationState.strokeColor.toArgb(), annotationState.strokeWidth, annotationState.strokeAlpha)
                                } else {
                                    val pdfPoint = annotationState.viewToPage(down.position.x, down.position.y)
                                    currentPathPoints.add(Triple(pdfPoint.x, pdfPoint.y, down.pressure))
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    
                                    // Detect multi-touch to switch to pan/zoom mode
                                    if (event.changes.size > 1) {
                                        isMultiTouch = true
                                        currentPathPoints.clear()
                                        eraserPosition = null
                                    }

                                    if (isMultiTouch) {
                                        // Standard Pan/Zoom logic
                                        val pan = event.calculatePan()
                                        val zoom = event.calculateZoom()
                                        val centroid = event.calculateCentroid()
                                        
                                        if (currentPdfView != null) {
                                            // Handle Native PDFView zoom/pan
                                            val view = currentPdfView!!
                                            val newZoom = (view.zoom * zoom).coerceIn(view.minZoom, view.maxZoom)
                                            if (zoom != 1f) {
                                                view.zoomCenteredTo(newZoom, android.graphics.PointF(centroid.x, centroid.y))
                                            }
                                            if (pan != Offset.Zero) {
                                                view.moveTo(view.currentXOffset + pan.x, view.currentYOffset + pan.y)
                                            }
                                        } else {
                                            // Handle Notebook mode zoom/pan
                                            if (zoom != 1f) {
                                                annotationState.currentZoom = (annotationState.currentZoom * zoom).coerceIn(1f, 5f)
                                            }
                                            annotationState.currentXOffset += pan.x
                                            annotationState.currentYOffset += pan.y
                                        }
                                        
                                        annotationState.invalidationTick++
                                        event.changes.forEach { it.consume() }
                                        if (event.changes.none { it.pressed }) break
                                    } else {
                                        // Single-touch/Stylus drawing logic
                                        val dragChange = event.changes.find { it.id == activePointerId }
                                        
                                        if (isStylus) {
                                            event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                                        }

                                        if (dragChange == null || !dragChange.pressed) {
                                            // FINALIZE SESSION
                                            if (gestureTool == VisualType.ERASER) {
                                                // Batch delete all IDs collected during this drag
                                                pendingDeletions.value.forEach { currentOnDeleteVisual(it) }
                                                pendingDeletions.value = emptySet()
                                            } else if (gestureTool != VisualType.TEXT && gestureTool != VisualType.STICKY_NOTE && currentPathPoints.isNotEmpty()) {
                                                val data = currentPathPoints.joinToString(";") { "${it.first},${it.second},${it.third}" }
                                                currentOnDrawComplete(gestureTool, data, annotationState.strokeColor, annotationState.strokeWidth, annotationState.strokeAlpha)
                                            }
                                            currentPathPoints.clear()
                                            eraserPosition = null
                                            break
                                        } else {
                                            dragChange.consume()
                                            if (gestureTool == VisualType.ERASER) {
                                                eraserPosition = dragChange.position
                                                findAndCollectHits(dragChange.position, annotationState, currentPdfView, currentVisuals, pendingDeletions)
                                            } else if (gestureTool != VisualType.TEXT && gestureTool != VisualType.STICKY_NOTE) {
                                                val pdfPoint = annotationState.viewToPage(dragChange.position.x, dragChange.position.y)
                                                // Simple distance-based point suppression for "less write" and smoother strings
                                                val lastPoint = currentPathPoints.lastOrNull()
                                                if (lastPoint == null || 
                                                    (pdfPoint.x - lastPoint.first).let { it * it } + 
                                                    (pdfPoint.y - lastPoint.second).let { it * it } > 0.000001f) {
                                                    currentPathPoints.add(Triple(pdfPoint.x, pdfPoint.y, dragChange.pressure))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        // LAYER 1: ── SAVED VISUALS ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tick = annotationState.invalidationTick // Observe tick for coordinate sync
            val zoom = if (pdfView != null) pdfView.zoom else annotationState.currentZoom

            if (!annotationState.isFocused) {
                visuals.forEach { visual ->
                    if (pendingDeletions.value.contains(visual.id)) return@forEach // Local hide during erasure for "fast" feel
                    val preParsedData = parsedVisualMap[visual.id] ?: return@forEach

                    val renderColor = Color(visual.color).copy(alpha = visual.alpha)
                    val baseScaledWidth = visual.strokeWidth * zoom

                    when (visual.type) {
                        VisualType.TEXT -> {
                            val data = preParsedData as? Offset ?: return@forEach
                            val pos = if (pdfView != null) annotationState.pageToView(data.x, data.y)
                            else android.graphics.PointF(data.x * annotationState.pageWidth, data.y * annotationState.pageHeight)
                            drawCircle(renderColor, radius = 10f, center = Offset(pos.x, pos.y))
                        }
                        VisualType.STICKY_NOTE -> {
                            val data = preParsedData as? Offset ?: return@forEach
                            val pos = if (pdfView != null) annotationState.pageToView(data.x, data.y)
                            else android.graphics.PointF(data.x * annotationState.pageWidth, data.y * annotationState.pageHeight)
                            drawRect(renderColor, topLeft = Offset(pos.x - 15f, pos.y - 15f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                        }
                        VisualType.RULER -> {
                            val points = preParsedData as? List<Triple<Float, Float, Float>> ?: return@forEach
                            if (points.size >= 2) {
                                val sP = points.first()
                                val eP = points.last()
                                val startPos = if (pdfView != null) annotationState.pageToView(sP.first, sP.second)
                                else android.graphics.PointF(sP.first * annotationState.pageWidth, sP.second * annotationState.pageHeight)
                                val endPos = if (pdfView != null) annotationState.pageToView(eP.first, eP.second)
                                else android.graphics.PointF(eP.first * annotationState.pageWidth, eP.second * annotationState.pageHeight)

                                drawLine(color = renderColor, start = Offset(startPos.x, startPos.y), end = Offset(endPos.x, endPos.y), strokeWidth = baseScaledWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                        }
                        VisualType.BOX -> {
                            val points = preParsedData as? List<Triple<Float, Float, Float>> ?: return@forEach
                            if (points.size >= 2) {
                                val sP = points.first()
                                val eP = points.last()
                                val startPos = if (pdfView != null) annotationState.pageToView(sP.first, sP.second)
                                else android.graphics.PointF(sP.first * annotationState.pageWidth, sP.second * annotationState.pageHeight)
                                val endPos = if (pdfView != null) annotationState.pageToView(eP.first, eP.second)
                                else android.graphics.PointF(eP.first * annotationState.pageWidth, eP.second * annotationState.pageHeight)

                                val rect = androidx.compose.ui.geometry.Rect(Offset(startPos.x, startPos.y), Offset(endPos.x, endPos.y))
                                drawRect(color = renderColor, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = baseScaledWidth))
                            }
                        }
                        else -> {
                            val points = preParsedData as? List<Triple<Float, Float, Float>> ?: return@forEach
                            if (points.size == 1) {
                                val p = points.first()
                                val pos = if (pdfView != null) annotationState.pageToView(p.first, p.second)
                                else android.graphics.PointF(p.first * annotationState.pageWidth, p.second * annotationState.pageHeight)
                                drawCircle(color = renderColor, radius = baseScaledWidth / 2f, center = Offset(pos.x, pos.y))
                            } else if (points.size > 1) {
                                val path = Path().apply {
                                    val start = points.first()
                                    val startPos = if (pdfView != null) annotationState.pageToView(start.first, start.second)
                                    else android.graphics.PointF(start.first * annotationState.pageWidth, start.second * annotationState.pageHeight)
                                    moveTo(startPos.x, startPos.y)
                                    for (i in 1 until points.size) {
                                        val p = points[i]
                                        val screenPos = if (pdfView != null) annotationState.pageToView(p.first, p.second)
                                        else android.graphics.PointF(p.first * annotationState.pageWidth, p.second * annotationState.pageHeight)
                                        lineTo(screenPos.x, screenPos.y)
                                    }
                                }
                                drawPath(path = path, color = renderColor, style = Stroke(width = baseScaledWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                    }
                }
            }
        }

        // LAYER 2: ── LIVE DRAWING ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!annotationState.isFocused && currentPathPoints.isNotEmpty() && effectiveTool != VisualType.ERASER) {
                val screenPoints = currentPathPoints.map { 
                    val s = if (pdfView != null) {
                        annotationState.pageToView(it.first, it.second)
                    } else {
                        android.graphics.PointF(it.first * (annotationState.pageWidth.takeIf { it > 0 } ?: 1f), it.second * (annotationState.pageHeight.takeIf { it > 0 } ?: 1f))
                    }
                    Offset(s.x, s.y)
                }
                
                val zoom = if (pdfView != null) pdfView.zoom else annotationState.currentZoom
                val scaledWidth = annotationState.strokeWidth * zoom
                val renderColor = annotationState.strokeColor.copy(alpha = annotationState.strokeAlpha)

                when (effectiveTool) {
                    VisualType.RULER -> {
                        drawLine(
                            color = renderColor,
                            start = screenPoints.first(),
                            end = screenPoints.last(),
                            strokeWidth = scaledWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    VisualType.BOX -> {
                        val start = screenPoints.first()
                        val end = screenPoints.last()
                        val rect = androidx.compose.ui.geometry.Rect(start, end)
                        drawRect(
                            color = renderColor,
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = if (annotationState.fillEnabled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = scaledWidth)
                        )
                    }
                    else -> {
                        val path = Path().apply {
                            moveTo(screenPoints.first().x, screenPoints.first().y)
                            for (i in 1 until screenPoints.size) {
                                lineTo(screenPoints[i].x, screenPoints[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = renderColor,
                            style = Stroke(
                                width = scaledWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Eraser indicator
            eraserPosition?.let { pos ->
                val zoom = if (pdfView != null) pdfView.zoom else annotationState.currentZoom
                val radius = (120f * zoom).coerceIn(20f, 250f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = radius,
                    center = pos,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    radius = radius,
                    center = pos
                )
            }
        }
    }
}

private fun findAndCollectHits(
    screenOffset: Offset,
    annotationState: AnnotationState,
    pdfView: com.github.barteksc.pdfviewer.PDFView?,
    visuals: List<NoteVisual>,
    collectedIds: MutableState<Set<Long>>
) {
    // Aggressive sweeping radius for high responsiveness
    val hitRadius = 25f
    val currentSet = collectedIds.value.toMutableSet()
    var changed = false

    visuals.forEach { visual ->
        if (currentSet.contains(visual.id)) return@forEach
        
        var isHit = false
        val parts = visual.data.split(";")
        
        if (visual.type == VisualType.TEXT || visual.type == VisualType.STICKY_NOTE) {
            val coords = visual.data.split(",")
            if (coords.size >= 2) {
                val pos = if (pdfView != null) annotationState.pageToView(coords[0].toFloat(), coords[1].toFloat())
                else android.graphics.PointF(coords[0].toFloat() * annotationState.pageWidth, coords[1].toFloat() * annotationState.pageHeight)
                if ((screenOffset - Offset(pos.x, pos.y)).getDistance() < hitRadius) isHit = true
            }
        } else {
            // Path collision
            var prevPos: Offset? = null
            for (pStr in parts) {
                val coords = pStr.split(",")
                if (coords.size >= 2) {
                    val p = if (pdfView != null) annotationState.pageToView(coords[0].toFloat(), coords[1].toFloat())
                    else android.graphics.PointF(coords[0].toFloat() * annotationState.pageWidth, coords[1].toFloat() * annotationState.pageHeight)
                    val currPos = Offset(p.x, p.y)
                    
                    if (prevPos != null) {
                        if (getDistanceToSegment(screenOffset, prevPos, currPos) < hitRadius) {
                            isHit = true
                            break
                        }
                    } else {
                        if ((screenOffset - currPos).getDistance() < hitRadius) {
                            isHit = true
                            break
                        }
                    }
                    prevPos = currPos
                }
            }
        }
        
        if (isHit) {
            currentSet.add(visual.id)
            changed = true
        }
    }
    if (changed) {
        collectedIds.value = currentSet
    }
}

private fun getDistanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (a - b).getDistanceSquared()
    if (l2 == 0f) return (p - a).getDistance()
    var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
    t = t.coerceIn(0f, 1f)
    return (p - Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))).getDistance()
}
