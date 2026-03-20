package com.pulse.presentation.lecture

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.pulse.core.data.db.VisualType

/**
 * AnnotationState manages the current state of the PDF annotation tools
 * and the coordinate mapping between screen and PDF page.
 *
 * Industry-standard design:
 * - Single source of truth for all annotation tool state
 * - Coordinate mapping is injected via pdfToScreenMapper/screenToPdfMapper for PDFs
 * - Notebook mode uses simple zoom/pan-based mapping
 * - Invalidation tick forces Canvas recomposition on native View changes
 */
class AnnotationState {
    // ── Tool State ──
    var isDrawingMode by mutableStateOf(false)
    var activeTool by mutableStateOf(VisualType.DRAWING)
    var currentTool by mutableStateOf(VisualType.DRAWING)
    var strokeColor by mutableStateOf(Color(0xFFE53935))  // Default: Scarlet
    var strokeWidth by mutableFloatStateOf(5f)
    var strokeAlpha by mutableFloatStateOf(1f)
    var fillEnabled by mutableStateOf(false)
    var isFocused by mutableStateOf(false)

    // ── Notebook / Canvas transform state ──
    var pageWidth by mutableFloatStateOf(0f)
    var pageHeight by mutableFloatStateOf(0f)
    var currentZoom by mutableFloatStateOf(1f)
    var currentXOffset by mutableFloatStateOf(0f)
    var currentYOffset by mutableFloatStateOf(0f)

    // Tick incremented by PDFView onDraw / FrameLayout onLayoutChange to force Canvas recomposition
    var invalidationTick by mutableIntStateOf(0)

    // ── Injected coordinate mappers (set by PdfMode) ──
    var pdfToScreenMapper: ((Float, Float) -> android.graphics.PointF)? = null
    var screenToPdfMapper: ((Float, Float) -> android.graphics.PointF)? = null

    /**
     * Converts screen coordinates to PDF page coordinates (normalized).
     * Falls back to Notebook zoom/pan mapping when no PDF mapper is installed.
     */
    fun viewToPage(screenX: Float, screenY: Float): android.graphics.PointF {
        screenToPdfMapper?.let { return it.invoke(screenX, screenY) }

        // Notebook mapping: Convert to normalized (0.0 to 1.0) coordinates
        val w = pageWidth.takeIf { it > 0 } ?: 1f
        val h = pageHeight.takeIf { it > 0 } ?: 1f
        return android.graphics.PointF(
            ((screenX - currentXOffset) / currentZoom) / w,
            ((screenY - currentYOffset) / currentZoom) / h
        )
    }

    /**
     * Converts PDF page coordinates back to screen coordinates.
     * Falls back to Notebook zoom/pan mapping when no PDF mapper is installed.
     */
    fun pageToView(pdfX: Float, pdfY: Float): android.graphics.PointF {
        pdfToScreenMapper?.let { return it.invoke(pdfX, pdfY) }

        // Notebook mapping: Scale from normalized back to screen pixels
        val w = pageWidth.takeIf { it > 0 } ?: 1f
        val h = pageHeight.takeIf { it > 0 } ?: 1f
        return android.graphics.PointF(
            (pdfX * w) * currentZoom + currentXOffset,
            (pdfY * h) * currentZoom + currentYOffset
        )
    }

    /**
     * Reset zoom/pan to default (useful on page change or container resize in notebook mode).
     */
    fun resetTransform() {
        currentZoom = 1f
        currentXOffset = 0f
        currentYOffset = 0f
        invalidationTick++
    }
}

@Composable
fun rememberAnnotationState() = remember { AnnotationState() }
