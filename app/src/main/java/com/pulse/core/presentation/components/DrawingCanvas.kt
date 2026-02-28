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
import com.pulse.core.data.db.NoteVisual
import com.pulse.core.data.db.VisualType
import com.pulse.presentation.lecture.AnnotationState

@Composable
fun DrawingCanvas(
    annotationState: AnnotationState,
    visuals: List<NoteVisual>,
    onDrawComplete: (VisualType, String, Color, Float) -> Unit,
    onDeleteVisual: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // 1. Wait for ANY pointer to go down
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                        val isEraserHardware = down.type == PointerType.Eraser
                        val shouldDraw = isStylus || annotationState.isDrawingMode
                        
                        if (!shouldDraw) {
                            continue
                        }

                        val activePointerId = down.id
                        val effectiveTool = if (isEraserHardware) VisualType.ERASER else annotationState.activeTool

                        // Start Drawing/Erasing
                        if (effectiveTool == VisualType.ERASER) {
                            findAndRemoveVisual(down.position, annotationState, visuals, onDeleteVisual)
                        } else {
                            currentPath = Path().apply { moveTo(down.position.x, down.position.y) }
                            val normPos = annotationState.toNormalized(down.position.x, down.position.y)
                            currentPoints.add(Offset(normPos.first, normPos.second))
                        }
                        down.consume()
                        
                        // 2. Track that specific pointer until it's released
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.find { it.id == activePointerId }
                            
                            // Palm Rejection: If current session is a stylus, ignore and consume all touch changes
                            if (isStylus) {
                                event.changes.filter { it.type == PointerType.Touch }.forEach { it.consume() }
                            }

                            if (dragChange == null || !dragChange.pressed) {
                                // Released or Lost
                                if (effectiveTool != VisualType.ERASER && currentPoints.isNotEmpty()) {
                                    val data = currentPoints.joinToString(";") { "${it.x},${it.y}" }
                                    onDrawComplete(
                                        effectiveTool,
                                        data,
                                        annotationState.strokeColor,
                                        annotationState.strokeWidth
                                    )
                                }
                                currentPath = null
                                currentPoints.clear()
                                break
                            } else {
                                // Dragged
                                dragChange.consume()
                                if (effectiveTool == VisualType.ERASER) {
                                    findAndRemoveVisual(dragChange.position, annotationState, visuals, onDeleteVisual)
                                } else {
                                    val normPos = annotationState.toNormalized(dragChange.position.x, dragChange.position.y)
                                    currentPoints.add(Offset(normPos.first, normPos.second))
                                    currentPath?.lineTo(dragChange.position.x, dragChange.position.y)
                                }
                            }
                        }
                    }
                }
            }
    )
 {
        // Draw existing visuals
        visuals.forEach { visual ->
            val points = visual.data.split(";").mapNotNull { 
                val p = it.split(",")
                if (p.size == 2) {
                    val screenPos = annotationState.fromNormalized(p[0].toFloat(), p[1].toFloat())
                    Offset(screenPos.first, screenPos.second)
                } else null
            }
            
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(visual.color).copy(alpha = if (visual.type == VisualType.HIGHLIGHT) 0.4f else 1f),
                    style = Stroke(
                        width = visual.strokeWidth * annotationState.currentZoom, 
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }

        // Draw current path in real-time
        currentPath?.let {
            drawPath(
                path = it,
                color = annotationState.strokeColor,
                style = Stroke(
                    width = annotationState.strokeWidth * annotationState.currentZoom,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

private fun findAndRemoveVisual(
    offset: Offset,
    state: AnnotationState,
    visuals: List<NoteVisual>,
    onDelete: (Long) -> Unit
) {
    visuals.find { visual ->
        visual.data.split(";").any { pStr ->
            val p = pStr.split(",")
            if (p.size == 2) {
                val screenPos = state.fromNormalized(p[0].toFloat(), p[1].toFloat())
                val dist = (Offset(screenPos.first, screenPos.second) - offset).getDistance()
                dist < 30f // 30px eraser radius
            } else false
        }
    }?.let { onDelete(it.id) }
}
