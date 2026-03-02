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

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (annotationState.isDrawingMode) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                
                                val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                                val isEraserHardware = down.type == PointerType.Eraser
                                
                                // In Drawing Mode, we handle EVERYTHING.
                                down.consume()
                                val activePointerId = down.id
                                currentDrawTool = if (isEraserHardware) VisualType.ERASER else annotationState.currentTool

                                if (currentDrawTool == VisualType.ERASER) {
                                    findAndRemoveVisual(down.position, annotationState, pdfView, visuals, onDeleteVisual)
                                } else if (currentDrawTool == VisualType.TEXT || currentDrawTool == VisualType.STICKY_NOTE) {
                                    val pdfPoint = if (pdfView != null) {
                                        annotationState.viewToPage(down.position.x, down.position.y)
                                    } else {
                                        val w = annotationState.pageWidth.takeIf { it > 0 } ?: 1f
                                        val h = annotationState.pageHeight.takeIf { it > 0 } ?: 1f
                                        android.graphics.PointF(down.position.x / w, down.position.y / h)
                                    }
                                    onAddVisualAtPos(currentDrawTool, pdfPoint.x, pdfPoint.y, annotationState.strokeColor.toArgb())
                                } else {
                                    val pdfPoint = if (pdfView != null) {
                                        annotationState.viewToPage(down.position.x, down.position.y)
                                    } else {
                                        val w = annotationState.pageWidth.takeIf { it > 0 } ?: 1f
                                        val h = annotationState.pageHeight.takeIf { it > 0 } ?: 1f
                                        android.graphics.PointF(down.position.x / w, down.position.y / h)
                                    }
                                    currentPathPoints.add(Triple(pdfPoint.x, pdfPoint.y, down.pressure))
                                }
                                
                                while (true) {
                                    val event = awaitPointerEvent()
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
                                        break
                                    } else {
                                        dragChange.consume()
                                        if (currentDrawTool == VisualType.ERASER) {
                                            findAndRemoveVisual(dragChange.position, annotationState, pdfView, visuals, onDeleteVisual)
                                        } else if (currentDrawTool != VisualType.TEXT && currentDrawTool != VisualType.STICKY_NOTE) {
                                            val pdfPoint = if (pdfView != null) {
                                                annotationState.viewToPage(dragChange.position.x, dragChange.position.y)
                                            } else {
                                                val w = annotationState.pageWidth.takeIf { it > 0 } ?: 1f
                                                val h = annotationState.pageHeight.takeIf { it > 0 } ?: 1f
                                                android.graphics.PointF(dragChange.position.x / w, dragChange.position.y / h)
                                            }
                                            currentPathPoints.add(Triple(pdfPoint.x, pdfPoint.y, dragChange.pressure))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        // LAYER 1: ── SAVED VISUALS ── (Only redraws when DB emits new list)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read tick to force redraw when PDFView zoom/pan changes
            @Suppress("UNUSED_VARIABLE")
            val tick = annotationState.invalidationTick
            
            fun drawVisualPath(pointsData: String, type: VisualType, baseColor: Color, baseWidth: Float) {
                val rawPoints = pointsData.split(";").mapNotNull { 
                    val p = it.split(",")
                    if (p.size >= 2) {
                        val pX = p[0].toFloat()
                        val pY = p[1].toFloat()
                        val screenPos = if (pdfView != null) {
                            annotationState.pageToView(pX, pY)
                        } else {
                            android.graphics.PointF(pX * annotationState.pageWidth, pY * annotationState.pageHeight)
                        }
                        Offset(screenPos.x, screenPos.y)
                    } else null
                }
                
                if (rawPoints.isEmpty()) return
    
                val zoom = pdfView?.zoom ?: 1f
                val scaledWidth = baseWidth * zoom
                val renderColor = baseColor.copy(alpha = if (type == VisualType.HIGHLIGHT) 0.4f else 1f)
    
                when (type) {
                    VisualType.RULER -> {
                        if (rawPoints.size >= 2) {
                            drawLine(
                                color = renderColor,
                                start = rawPoints.first(),
                                end = rawPoints.last(),
                                strokeWidth = scaledWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                    VisualType.BOX -> {
                        if (rawPoints.size >= 2) {
                            val start = rawPoints.first()
                            val end = rawPoints.last()
                            val rect = androidx.compose.ui.geometry.Rect(start, end)
                            drawRect(
                                color = renderColor,
                                topLeft = rect.topLeft,
                                size = rect.size,
                                style = Stroke(width = scaledWidth)
                            )
                        }
                    }
                    else -> {
                        if (rawPoints.size == 1) {
                            drawCircle(color = renderColor, radius = scaledWidth / 2f, center = rawPoints.first())
                        } else {
                            val path = Path().apply {
                                moveTo(rawPoints.first().x, rawPoints.first().y)
                                for (i in 1 until rawPoints.size) {
                                    lineTo(rawPoints[i].x, rawPoints[i].y)
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

        // LAYER 2: ── LIVE DRAWING ── (Redraws at 60fps with only 1 line)
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
                
                val zoom = pdfView?.zoom ?: 1f
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
    // Inverse zoom factor to keep eraser size consistent
    val zoom = pdfView?.zoom ?: 1f
    val hitRadius = 40f * zoom 

    val target = visuals.find { visual ->
        when (visual.type) {
            VisualType.TEXT, VisualType.STICKY_NOTE -> {
                val p = visual.data.split(",")
                val screenPos = if (pdfView != null) state.pageToView(p[0].toFloat(), p[1].toFloat()) else android.graphics.PointF(0f, 0f)
                (Offset(screenPos.x, screenPos.y) - offset).getDistance() < hitRadius
            }
            else -> {
                visual.data.split(";").any { pStr ->
                    val p = pStr.split(",")
                    if (p.size >= 2) {
                        val screenPos = if (pdfView != null) {
                            state.pageToView(p[0].toFloat(), p[1].toFloat())
                        } else {
                            android.graphics.PointF(p[0].toFloat() * state.pageWidth, p[1].toFloat() * state.pageHeight)
                        }
                        (Offset(screenPos.x, screenPos.y) - offset).getDistance() < hitRadius
                    } else false
                }
            }
        }
    }
    target?.let { onDelete(it.id) }
}
