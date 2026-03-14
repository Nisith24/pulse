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
    onDrawComplete: (VisualType, String, Color, Float) -> Unit,
    onAddVisualAtPos: (VisualType, Float, Float, Int) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current drawing session coordinates: List of (NormalizedX, NormalizedY, Pressure)
    var currentPathPoints = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
    var currentDrawTool by remember { mutableStateOf(VisualType.DRAWING) }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (annotationState.isDrawingMode) {
                    Modifier.pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {
                                var isMultiTouch = false
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var activePointerId = down.id
                                
                                val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                                val isEraserHardware = down.type == PointerType.Eraser
                                currentDrawTool = if (isEraserHardware) VisualType.ERASER else annotationState.currentTool

                                // Initial action
                                if (currentDrawTool == VisualType.ERASER) {
                                    eraserPosition = down.position
                                    findAndRemoveVisual(down.position, annotationState, pdfView, visuals, onDeleteVisual)
                                } else if (currentDrawTool == VisualType.TEXT || currentDrawTool == VisualType.STICKY_NOTE) {
                                    val pdfPoint = annotationState.viewToPage(down.position.x, down.position.y)
                                    onAddVisualAtPos(currentDrawTool, pdfPoint.x, pdfPoint.y, annotationState.strokeColor.toArgb())
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
                                        
                                        if (pdfView != null) {
                                            // Handle Native PDFView zoom/pan
                                            val newZoom = (pdfView.zoom * zoom).coerceIn(pdfView.minZoom, pdfView.maxZoom)
                                            if (zoom != 1f) {
                                                pdfView.zoomCenteredTo(newZoom, android.graphics.PointF(centroid.x, centroid.y))
                                            }
                                            if (pan != Offset.Zero) {
                                                pdfView.moveTo(pdfView.currentXOffset + pan.x, pdfView.currentYOffset + pan.y)
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
                                            if (currentDrawTool != VisualType.ERASER && currentDrawTool != VisualType.TEXT && currentDrawTool != VisualType.STICKY_NOTE && currentPathPoints.isNotEmpty()) {
                                                val data = currentPathPoints.joinToString(";") { "${it.first},${it.second},${it.third}" }
                                                onDrawComplete(currentDrawTool, data, annotationState.strokeColor, annotationState.strokeWidth)
                                            }
                                            currentPathPoints.clear()
                                            eraserPosition = null
                                            break
                                        } else {
                                            dragChange.consume()
                                            if (currentDrawTool == VisualType.ERASER) {
                                                eraserPosition = dragChange.position
                                                findAndRemoveVisual(dragChange.position, annotationState, pdfView, visuals, onDeleteVisual)
                                            } else if (currentDrawTool != VisualType.TEXT && currentDrawTool != VisualType.STICKY_NOTE) {
                                                val pdfPoint = annotationState.viewToPage(dragChange.position.x, dragChange.position.y)
                                                currentPathPoints.add(Triple(pdfPoint.x, pdfPoint.y, dragChange.pressure))
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
            val tick = annotationState.invalidationTick
            
            fun drawVisualPath(pointsData: String, type: VisualType, baseColor: Color, baseWidth: Float) {
                val rawPoints = pointsData.split(";").mapNotNull { 
                    val p = it.split(",")
                    if (p.size >= 2) {
                        val pX = p[0].toFloat()
                        val pY = p[1].toFloat()
                        val pZ = if (p.size >= 3) p[2].toFloat() else 1f
                        val screenPos = if (pdfView != null) {
                            annotationState.pageToView(pX, pY)
                        } else {
                            android.graphics.PointF(pX * annotationState.pageWidth, pY * annotationState.pageHeight)
                        }
                        Triple(Offset(screenPos.x, screenPos.y), pZ, type)
                    } else null
                }
                
                if (rawPoints.isEmpty()) return
    
                val zoom = if (pdfView != null) pdfView.zoom else annotationState.currentZoom
                val baseScaledWidth = baseWidth * zoom
                val renderColor = baseColor.copy(alpha = if (type == VisualType.HIGHLIGHT) 0.4f else 1f)
    
                when (type) {
                    VisualType.RULER -> {
                        if (rawPoints.size >= 2) {
                            drawLine(
                                color = renderColor,
                                start = rawPoints.first().first,
                                end = rawPoints.last().first,
                                strokeWidth = baseScaledWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                    VisualType.BOX -> {
                        if (rawPoints.size >= 2) {
                            val start = rawPoints.first().first
                            val end = rawPoints.last().first
                            val rect = androidx.compose.ui.geometry.Rect(start, end)
                            drawRect(
                                color = renderColor,
                                topLeft = rect.topLeft,
                                size = rect.size,
                                style = Stroke(width = baseScaledWidth)
                            )
                        }
                    }
                    else -> {
                        if (rawPoints.size == 1) {
                            drawCircle(color = renderColor, radius = baseScaledWidth / 2f, center = rawPoints.first().first)
                        } else {
                            val path = Path().apply {
                                moveTo(rawPoints.first().first.x, rawPoints.first().first.y)
                                for (i in 1 until rawPoints.size) {
                                    lineTo(rawPoints[i].first.x, rawPoints[i].first.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = renderColor,
                                style = Stroke(
                                    width = baseScaledWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
            
            if (!annotationState.isFocused) {
                visuals.forEach { visual ->
                    when (visual.type) {
                        VisualType.TEXT -> {
                            val pos = if (pdfView != null) annotationState.pageToView(visual.data.split(",")[0].toFloat(), visual.data.split(",")[1].toFloat())
                            else android.graphics.PointF(0f, 0f)
                            drawCircle(Color(visual.color), radius = 10f, center = Offset(pos.x, pos.y))
                        }
                        VisualType.STICKY_NOTE -> {
                            val pos = if (pdfView != null) annotationState.pageToView(visual.data.split(",")[0].toFloat(), visual.data.split(",")[1].toFloat())
                            else android.graphics.PointF(0f, 0f)
                            drawRect(Color(visual.color), topLeft = Offset(pos.x - 15f, pos.y - 15f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                        }
                        else -> drawVisualPath(visual.data, visual.type, Color(visual.color), visual.strokeWidth)
                    }
                }
            }
        }

        // LAYER 2: ── LIVE DRAWING ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!annotationState.isFocused && currentPathPoints.isNotEmpty() && currentDrawTool != VisualType.ERASER) {
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
                val renderColor = annotationState.strokeColor.copy(alpha = if (currentDrawTool == VisualType.HIGHLIGHT) 0.4f else 1f)

                when (currentDrawTool) {
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
                            style = Stroke(width = scaledWidth)
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

private fun findAndRemoveVisual(
    offset: Offset,
    state: AnnotationState,
    pdfView: com.github.barteksc.pdfviewer.PDFView?,
    visuals: List<NoteVisual>,
    onDelete: (Long) -> Unit
) {
    // Aggressive sweeping radius for high responsiveness
    val zoom = pdfView?.zoom ?: state.currentZoom
    val hitRadius = if (pdfView == null) 350f else 300f * zoom 

    // We scan all visuals on the current page. If the eraser tip overlaps a stroke, it's purged.
    for (i in visuals.indices.reversed()) {
        val visual = visuals[i]
        var isHit = false
        
        when (visual.type) {
            VisualType.TEXT, VisualType.STICKY_NOTE -> {
                val p = visual.data.split(",")
                if (p.size >= 2) {
                    val screenPos = if (pdfView != null) state.pageToView(p[0].toFloat(), p[1].toFloat()) else android.graphics.PointF(0f, 0f)
                    if ((Offset(screenPos.x, screenPos.y) - offset).getDistance() < hitRadius) {
                        isHit = true
                    }
                }
            }
            else -> {
                val pStrs = visual.data.split(";")
                var prevPos: Offset? = null
                
                // Adaptive sampling: stay responsive while maintaining precision
                val step = if (pStrs.size > 200) 4 else if (pStrs.size > 100) 2 else 1
                
                for (j in pStrs.indices step step) {
                    val p = pStrs[j].split(",")
                    if (p.size >= 2) {
                        val screenPos = if (pdfView != null) {
                            state.pageToView(p[0].toFloat(), p[1].toFloat())
                        } else {
                            val w = state.pageWidth.takeIf { it > 0f } ?: 1f
                            val h = state.pageHeight.takeIf { it > 0f } ?: 1f
                            android.graphics.PointF(p[0].toFloat() * w, p[1].toFloat() * h)
                        }
                        val currPos = Offset(screenPos.x, screenPos.y)
                        
                        if (prevPos != null) {
                            if (getDistanceToSegment(offset, prevPos, currPos) < hitRadius) {
                                isHit = true
                                break
                            }
                        } else {
                            if ((offset - currPos).getDistance() < hitRadius) {
                                isHit = true
                                break
                            }
                        }
                        prevPos = currPos
                    }
                }
            }
        }
        
        if (isHit) {
            onDelete(visual.id)
            // No break: enable "sweeping" mode to delete everything touched
        }
    }
}

private fun getDistanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (a - b).getDistanceSquared()
    if (l2 == 0f) return (p - a).getDistance()
    var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
    t = t.coerceIn(0f, 1f)
    return (p - Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))).getDistance()
}
