package com.pulse.presentation.lecture.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * A reusable split-screen container that divides two content areas with a draggable handle.
 * Supports both horizontal (tablet) and vertical (phone) orientations.
 *
 * @param splitRatio Current ratio [0.2..0.8] for the primary content area.
 * @param onSplitRatioChange Called when the user drags to change the ratio.
 * @param onDragStopped Called when dragging ends, with the final ratio (for persistence).
 * @param isHorizontal If true, side-by-side (tablet). If false, top-bottom (phone).
 * @param primaryContent The first/top/left panel content.
 * @param secondaryContent The second/bottom/right panel content.
 */
@Composable
fun SplitResizableContainer(
    splitRatio: Float,
    onSplitRatioChange: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    isHorizontal: Boolean,
    primaryContent: @Composable BoxScope.() -> Unit,
    secondaryContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    val visualSize by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 3.dp,
        label = "handle_size"
    )
    val handleAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.4f,
        label = "handle_alpha"
    )
    val handleColor = if (isDragging) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalPx = if (isHorizontal) constraints.maxWidth.toFloat() else constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Primary content
        Box(
            modifier = (if (isHorizontal) {
                Modifier.fillMaxHeight().fillMaxWidth(splitRatio)
            } else {
                Modifier.fillMaxWidth().fillMaxHeight(splitRatio)
            }).clipToBounds(),
            content = primaryContent
        )

        // Secondary content
        Box(
            modifier = (if (isHorizontal) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(1f - splitRatio)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(1f - splitRatio)
            }).clipToBounds(),
            content = secondaryContent
        )

        // Drag handle (invisible 48dp touch target + visible indicator)
        val handleOffset = if (isHorizontal) {
            with(density) { (totalPx * splitRatio).toDp() - 24.dp }
        } else {
            with(density) { (totalPx * splitRatio).toDp() - 24.dp }
        }

        Box(
            modifier = (if (isHorizontal) {
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = handleOffset)
                    .width(48.dp)
                    .fillMaxHeight()
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = handleOffset)
                    .height(48.dp)
                    .fillMaxWidth()
            }).draggable(
                orientation = if (isHorizontal) Orientation.Horizontal else Orientation.Vertical,
                onDragStarted = {
                    isDragging = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                state = rememberDraggableState { delta ->
                    val newRatio = (splitRatio + delta / totalPx).coerceIn(0.2f, 0.8f)
                    if ((newRatio * 10).toInt() != (splitRatio * 10).toInt()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSplitRatioChange(newRatio)
                },
                onDragStopped = {
                    isDragging = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Snap to 0.3, 0.5, 0.7
                    val snapPoints = listOf(0.3f, 0.5f, 0.7f)
                    val snapped = snapPoints.minByOrNull { kotlin.math.abs(it - splitRatio) } ?: splitRatio
                    val finalRatio = if (kotlin.math.abs(snapped - splitRatio) < 0.1f) snapped else splitRatio
                    onSplitRatioChange(finalRatio)
                    onDragStopped(finalRatio)
                }
            ),
            contentAlignment = Alignment.Center
        ) {
            // Visible indicator
            Box(
                modifier = if (isHorizontal) {
                    Modifier
                        .width(visualSize)
                        .fillMaxHeight(0.2f)
                        .background(handleColor.copy(alpha = handleAlpha), CircleShape)
                } else {
                    Modifier
                        .height(visualSize)
                        .fillMaxWidth(0.2f)
                        .background(handleColor.copy(alpha = handleAlpha), CircleShape)
                }
            )
        }
    }
}
