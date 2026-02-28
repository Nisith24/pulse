package com.pulse.presentation.lecture

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.pulse.core.data.db.VisualType

/**
 * AnnotationState manages the current state of the PDF annotation tools
 * and the coordinate mapping between screen and PDF page.
 */
class AnnotationState {
    var isDrawingMode by mutableStateOf(false)
    var activeTool by mutableStateOf(VisualType.DRAWING)
    var strokeColor by mutableStateOf(Color.Red)
    var strokeWidth by mutableStateOf(5f)
    
    // PDF Transformation state for coordinate mapping
    var currentZoom by mutableFloatStateOf(1f)
    var currentXOffset by mutableFloatStateOf(0f)
    var currentYOffset by mutableFloatStateOf(0f)
    var pageWidth by mutableFloatStateOf(0f)
    var pageHeight by mutableFloatStateOf(0f)

    /**
     * Converts screen coordinates to normalized coordinates (0..1) relative to the current visible page dimensions.
     */
    fun toNormalized(screenX: Float, screenY: Float): Pair<Float, Float> {
        if (pageWidth <= 0f || pageHeight <= 0f) return 0f to 0f
        
        // (ScreenCoord - PageOffsetInView) / (CurrentPageDimension)
        val normX = (screenX - currentXOffset) / pageWidth
        val normY = (screenY - currentYOffset) / pageHeight
        
        return normX to normY
    }

    /**
     * Converts normalized coordinates (0..1) back to screen coordinates.
     */
    fun fromNormalized(normX: Float, normY: Float): Pair<Float, Float> {
        // (NormCoord * CurrentPageDimension) + PageOffsetInView
        val screenX = (normX * pageWidth) + currentXOffset
        val screenY = (normY * pageHeight) + currentYOffset
        
        return screenX to screenY
    }
}

@Composable
fun rememberAnnotationState() = remember { AnnotationState() }
