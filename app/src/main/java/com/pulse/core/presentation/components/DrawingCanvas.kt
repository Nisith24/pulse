package com.pulse.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
            .then(
                if (annotationState.isDrawingMode) {
                    Modifier.pointerInput(annotationState.activeTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (annotationState.activeTool == VisualType.ERASER) {
                                    // Object Eraser: Find and delete first intersecting stroke
                                    visuals.find { visual ->
                                        visual.data.split(";").any { pStr ->
                                            val p = pStr.split(",")
                                            if (p.size == 2) {
                                                val screenPos = annotationState.fromNormalized(p[0].toFloat(), p[1].toFloat())
                                                val dist = (Offset(screenPos.first, screenPos.second) - offset).getDistance()
                                                dist < 30f // 30px eraser radius
                                            } else false
                                        }
                                    }?.let { onDeleteVisual(it.id) }
                                } else {
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    val normPos = annotationState.toNormalized(offset.x, offset.y)
                                    currentPoints.add(Offset(normPos.first, normPos.second))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (annotationState.activeTool == VisualType.ERASER) {
                                    // Continuous erasing while dragging
                                    visuals.find { visual ->
                                        visual.data.split(";").any { pStr ->
                                            val p = pStr.split(",")
                                            if (p.size == 2) {
                                                val screenPos = annotationState.fromNormalized(p[0].toFloat(), p[1].toFloat())
                                                val dist = (Offset(screenPos.first, screenPos.second) - change.position).getDistance()
                                                dist < 30f
                                            } else false
                                        }
                                    }?.let { onDeleteVisual(it.id) }
                                } else {
                                    val normPos = annotationState.toNormalized(change.position.x, change.position.y)
                                    currentPoints.add(Offset(normPos.first, normPos.second))
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                }
                            },
                            onDragEnd = {
                                if (annotationState.activeTool != VisualType.ERASER && currentPoints.isNotEmpty()) {
                                    val data = currentPoints.joinToString(";") { "${it.x},${it.y}" }
                                    onDrawComplete(
                                        annotationState.activeTool,
                                        data,
                                        annotationState.strokeColor,
                                        annotationState.strokeWidth
                                    )
                                }
                                currentPath = null
                                currentPoints.clear()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Draw existing visuals
        visuals.forEach { visual ->
            val points = visual.data.split(";").mapNotNull { 
                val p = it.split(",")
                if (p.size == 2) {
                    // Convert back from normalized to screen pixels for rendering
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
