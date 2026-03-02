package com.pulse.presentation.lecture.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.core.data.db.VisualType
import com.pulse.presentation.lecture.AnnotationState

// Professional color palette
private val ToolbarColors = listOf(
    Color(0xFFE53935), // Red
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFFFB8C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF00ACC1), // Cyan
    Color(0xFFFFD600), // Yellow
    Color(0xFF212121), // Black
    Color(0xFFFFFFFF), // White
)

private data class ToolItem(
    val type: VisualType,
    val icon: ImageVector,
    val label: String
)

private val PrimaryTools = listOf(
    ToolItem(VisualType.DRAWING, Icons.Default.Edit, "Pen"),
    ToolItem(VisualType.HIGHLIGHT, Icons.Default.Brush, "Highlight"),
    ToolItem(VisualType.RULER, Icons.Default.HorizontalRule, "Line"),
    ToolItem(VisualType.BOX, Icons.Default.CheckBoxOutlineBlank, "Box"),
    ToolItem(VisualType.ERASER, Icons.Outlined.Delete, "Eraser"),
)

@Composable
fun AnnotationToolbar(
    state: AnnotationState,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf(ExpandedSection.NONE) }
    val haptic = LocalHapticFeedback.current

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Expandable color picker section
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.COLOR,
                enter = fadeIn(tween(150)) + expandVertically(tween(200)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(150))
            ) {
                ColorPickerRow(
                    selectedColor = state.strokeColor,
                    onColorSelected = {
                        state.strokeColor = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }

            // Expandable stroke width section
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.STROKE,
                enter = fadeIn(tween(150)) + expandVertically(tween(200)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(150))
            ) {
                StrokeWidthRow(
                    width = state.strokeWidth,
                    color = state.strokeColor,
                    onWidthChange = { state.strokeWidth = it }
                )
            }

            // Main tool row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool buttons
                PrimaryTools.forEach { tool ->
                    CompactToolButton(
                        icon = tool.icon,
                        label = tool.label,
                        isSelected = state.currentTool == tool.type,
                        onClick = {
                            state.currentTool = tool.type
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            // Collapse if eraser
                            if (tool.type == VisualType.ERASER) {
                                expandedSection = ExpandedSection.NONE
                            }
                        }
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                )

                // Color indicator button
                if (state.currentTool != VisualType.ERASER) {
                    ColorIndicatorButton(
                        color = state.strokeColor,
                        isExpanded = expandedSection == ExpandedSection.COLOR,
                        onClick = {
                            expandedSection = if (expandedSection == ExpandedSection.COLOR)
                                ExpandedSection.NONE else ExpandedSection.COLOR
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }

                // Stroke width button
                if (state.currentTool != VisualType.ERASER) {
                    StrokeIndicatorButton(
                        width = state.strokeWidth,
                        color = state.strokeColor,
                        isExpanded = expandedSection == ExpandedSection.STROKE,
                        onClick = {
                            expandedSection = if (expandedSection == ExpandedSection.STROKE)
                                ExpandedSection.NONE else ExpandedSection.STROKE
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
        }
    }
}

private enum class ExpandedSection { NONE, COLOR, STROKE }

@Composable
private fun CompactToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.1f else 1f,
        tween(150),
        label = "tool_scale"
    )
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        tween(200),
        label = "tool_bg"
    )
    val iconTint by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200),
        label = "tool_tint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .scale(scale)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = iconTint
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarColors.forEach { color ->
            val isSelected = selectedColor == color
            val borderScale by animateFloatAsState(
                if (isSelected) 1.15f else 1f,
                tween(150),
                label = "color_scale"
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .scale(borderScale)
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ) else Modifier
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (color == Color.White) Modifier.border(
                            1.dp,
                            Color.LightGray,
                            CircleShape
                        ) else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onColorSelected(color) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (color == Color.White || color == Color(0xFFFFD600))
                            Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StrokeWidthRow(
    width: Float,
    color: Color,
    onWidthChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Preview dot
        Box(
            modifier = Modifier
                .size(width.dp.coerceIn(4.dp, 24.dp))
                .background(color, CircleShape)
        )

        Slider(
            value = width,
            onValueChange = onWidthChange,
            valueRange = 1f..25f,
            modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = "${width.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
    }
}

@Composable
private fun ColorIndicatorButton(
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 17.dp),
                onClick = onClick
            )
            .then(
                if (isExpanded) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ) else Modifier
            )
            .padding(4.dp)
            .background(color, CircleShape)
            .then(
                if (color == Color.White) Modifier.border(
                    1.dp,
                    Color.LightGray,
                    CircleShape
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
private fun StrokeIndicatorButton(
    width: Float,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 17.dp),
                onClick = onClick
            )
            .then(
                if (isExpanded) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ) else Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LineWeight,
            contentDescription = "Stroke Width",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
