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
    var isFocused by mutableStateOf(false)
    
    // PDF Transformation state for coordinate mapping
    var pageWidth by mutableFloatStateOf(0f)
    var pageHeight by mutableFloatStateOf(0f)
    var currentZoom by mutableFloatStateOf(1f)
    var currentXOffset by mutableFloatStateOf(0f)
    var currentYOffset by mutableFloatStateOf(0f)
    var currentTool by mutableStateOf(VisualType.DRAWING)
    
    // Tick incremented by PDFView onDraw to force Canvas recomposition
    var invalidationTick by mutableIntStateOf(0)
    
    // Injected by PDF Screen to handle coordinate mapping without direct dependency
    var pdfToScreenMapper: ((Float, Float) -> android.graphics.PointF)? = null
    var screenToPdfMapper: ((Float, Float) -> android.graphics.PointF)? = null

    /**
     * Converts screen coordinates to PDF page coordinates (unscaled points).
     */
    fun viewToPage(screenX: Float, screenY: Float): android.graphics.PointF {
        val mapper = screenToPdfMapper
        if (mapper != null) return mapper.invoke(screenX, screenY)
        
        // Fallback: Default mapping if no mapper injected
        return android.graphics.PointF(screenX, screenY)
    }

    /**
     * Converts PDF page coordinates back to screen coordinates.
     */
    fun pageToView(pdfX: Float, pdfY: Float): android.graphics.PointF {
        val mapper = pdfToScreenMapper
        if (mapper != null) return mapper.invoke(pdfX, pdfY)
        
        // Fallback: Default mapping if no mapper injected
        return android.graphics.PointF(pdfX, pdfY)
    }
}

@Composable
fun rememberAnnotationState() = remember { AnnotationState() }
