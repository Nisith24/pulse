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
    // Current drawing session coordinates: List of (NormalizedX, NormalizedY, Pressure)
    var currentPathPoints = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
    var currentDrawTool by remember { mutableStateOf(VisualType.DRAWING) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(annotationState.isDrawingMode) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                        val isEraserHardware = down.type == PointerType.Eraser
                        
                        // ── INDUSTRY STANDARD INTERACTION LOGIC ──
                        // Intercept events ONLY if we are explicitly in 'Drawing Mode'.
                        // This allows fluid swiping/scrolling with both finger and stylus in normal mode.
                        val shouldIntercept = annotationState.isDrawingMode
                        
                        if (!shouldIntercept) {
                            // Let the event bubble down to PDFView for swiping/zooming
                            continue
                        }

                        // We ARE drawing or erasing. From here, we consume to stop PDF from shifting.
                        down.consume()
                        val activePointerId = down.id
                        currentDrawTool = if (isEraserHardware) VisualType.ERASER else annotationState.activeTool

                        if (currentDrawTool == VisualType.ERASER) {
                            findAndRemoveVisual(down.position, annotationState, visuals, onDeleteVisual)
                        } else {
                            val norm = annotationState.toNormalized(down.position.x, down.position.y)
                            currentPathPoints.add(Triple(norm.first, norm.second, down.pressure))
                        }
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.find { it.id == activePointerId }
                            
                            // Palm Rejection: If this is a stylus session, kill all secondary Touch pointers
                            if (isStylus) {
                                event.changes.forEach { if (it.type == PointerType.Touch) it.consume() }
                            }

                            if (dragChange == null || !dragChange.pressed) {
                                // Session finished
                                if (currentDrawTool != VisualType.ERASER && currentPathPoints.isNotEmpty()) {
                                    // Serialize: x,y,p;x,y,p...
                                    val data = currentPathPoints.joinToString(";") { "${it.first},${it.second},${it.third}" }
                                    onDrawComplete(
                                        currentDrawTool,
                                        data,
                                        annotationState.strokeColor,
                                        annotationState.strokeWidth
                                    )
                                }
                                currentPathPoints.clear()
                                break
                            } else {
                                // Dragged
                                dragChange.consume()
                                if (currentDrawTool == VisualType.ERASER) {
                                    findAndRemoveVisual(dragChange.position, annotationState, visuals, onDeleteVisual)
                                } else {
                                    val norm = annotationState.toNormalized(dragChange.position.x, dragChange.position.y)
                                    currentPathPoints.add(Triple(norm.first, norm.second, dragChange.pressure))
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // ── ADV RENDERING PIPELINE ──
        // We render segments individually to support variable pressure-based widths.
        
        fun drawVisualPath(pointsData: String, type: VisualType, baseColor: Color, baseWidth: Float) {
            val rawPoints = pointsData.split(";").mapNotNull { 
                val p = it.split(",")
                if (p.size >= 2) {
                    val pX = p[0].toFloat()
                    val pY = p[1].toFloat()
                    val pPress = if (p.size >= 3) p[2].toFloat() else 1f
                    val screenPos = annotationState.fromNormalized(pX, pY)
                    Triple(screenPos.first, screenPos.second, pPress)
                } else null
            }
            
            if (rawPoints.size < 2) return

            for (i in 0 until rawPoints.size - 1) {
                val p1 = rawPoints[i]
                val p2 = rawPoints[i+1]
                
                // Pressure mapping: width scales between 0.5x and 1.5x of baseWidth based on 0..1 pressure
                // Fallback to 1.0 if not a pressure-sensitive stylus
                val pressureFact = if (p2.third > 0) 0.5f + p2.third else 1.0f
                val segmentWidth = baseWidth * pressureFact * annotationState.currentZoom

                drawLine(
                    color = baseColor.copy(alpha = if (type == VisualType.HIGHLIGHT) 0.4f else 1f),
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = segmentWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        // 1. Draw Saved Visuals
        visuals.forEach { visual ->
            drawVisualPath(visual.data, visual.type, Color(visual.color), visual.strokeWidth)
        }

        // 2. Draw Real-time Path
        if (currentPathPoints.isNotEmpty()) {
            val screenPoints = currentPathPoints.map { 
                val s = annotationState.fromNormalized(it.first, it.second)
                Triple(s.first, s.second, it.third)
            }
            for (i in 0 until screenPoints.size - 1) {
                val p1 = screenPoints[i]
                val p2 = screenPoints[i+1]
                val pressureFact = if (p2.third > 0) 0.5f + p2.third else 1.0f
                drawLine(
                    color = annotationState.strokeColor,
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = annotationState.strokeWidth * pressureFact * annotationState.currentZoom,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
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
            if (p.size >= 2) {
                val screenPos = state.fromNormalized(p[0].toFloat(), p[1].toFloat())
                val dist = (Offset(screenPos.first, screenPos.second) - offset).getDistance()
                dist < 30f // 30px eraser radius
            } else false
        }
    }?.let { onDelete(it.id) }
}
