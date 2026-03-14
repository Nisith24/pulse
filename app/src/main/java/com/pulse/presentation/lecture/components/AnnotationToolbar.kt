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

// High-Tech Cyber Color Palette
private val ToolbarColors = listOf(
    Color(0xFFFF2A55), // Neon Red
    Color(0xFF0A84FF), // Cyber Blue
    Color(0xFF32D74B), // Acid Green
    Color(0xFFFF9F0A), // Energy Orange
    Color(0xFFBF5AF2), // Plasma Purple
    Color(0xFF64D2FF), // Laser Cyan
    Color(0xFFFFD60A), // Electric Yellow
    Color(0xFF2C2C2E), // Onyx
    Color(0xFFFFFFFF), // Pure White
)

private data class ToolItem(
    val type: VisualType,
    val icon: ImageVector,
    val label: String
)

private val PrimaryTools = listOf(
    ToolItem(VisualType.DRAWING, Icons.Outlined.Edit, "Draw"),
    ToolItem(VisualType.HIGHLIGHT, Icons.Outlined.Brush, "Glow"),
    ToolItem(VisualType.RULER, Icons.Outlined.HorizontalRule, "Line"),
    ToolItem(VisualType.BOX, Icons.Outlined.CheckBoxOutlineBlank, "Box"),
    ToolItem(VisualType.ERASER, Icons.Outlined.AutoFixHigh, "Erase"),
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

    // Floating Glassmorphic Pill Container
    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(16.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(32.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Expandable Color section (Animated)
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.COLOR,
                enter = fadeIn(tween(200)) + expandVertically(tween(250, easing = LinearOutSlowInEasing)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200, easing = FastOutSlowInEasing))
            ) {
                ColorPickerRow(
                    selectedColor = state.strokeColor,
                    onColorSelected = {
                        state.strokeColor = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }

            // Expandable Stroke Width section (Animated)
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.STROKE,
                enter = fadeIn(tween(200)) + expandVertically(tween(250, easing = LinearOutSlowInEasing)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200, easing = FastOutSlowInEasing))
            ) {
                StrokeWidthRow(
                    width = state.strokeWidth,
                    color = state.strokeColor,
                    onWidthChange = { state.strokeWidth = it }
                )
            }

            // Core Tools Layout
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryTools.forEach { tool ->
                    CompactToolButton(
                        icon = tool.icon,
                        label = tool.label,
                        isSelected = state.currentTool == tool.type,
                        activeColor = state.strokeColor,
                        onClick = {
                            state.currentTool = tool.type
                            state.activeTool = tool.type
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (tool.type == VisualType.ERASER) {
                                expandedSection = ExpandedSection.NONE
                            }
                        }
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )

                // Only show color and stroke options for drawing tools (not eraser)
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
    activeColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1f, tween(200), label = "scale")
    val bgColor by animateColorAsState(
        if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent, 
        tween(250), 
        label = "bg_color"
    )
    val iconTint by animateColorAsState(
        if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        tween(250),
        label = "tint_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
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
            .fillMaxWidth(0.9f)
            .padding(bottom = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarColors.forEach { color ->
            val isSelected = selectedColor == color
            val borderScale by animateFloatAsState(if (isSelected) 1.3f else 1f, tween(200), label = "c_scale")
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(borderScale)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onColorSelected(color) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
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
            .fillMaxWidth(0.85f)
            .padding(bottom = 16.dp, top = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )

        Text(
            text = "${width.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ColorIndicatorButton(
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isExpanded) 1.1f else 1f, tween(200), label = "")
    
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(38.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .padding(4.dp)
            .background(color, CircleShape),
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
    val scale by animateFloatAsState(if (isExpanded) 1.1f else 1f, tween(200), label = "")
    val bgColor by animateColorAsState(if (isExpanded) color.copy(alpha = 0.15f) else Color.Transparent, tween(250), label = "")
    
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(38.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LineWeight,
            contentDescription = "Stroke Width",
            modifier = Modifier.size(20.dp),
            tint = if (isExpanded) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        // High-tech size indicator
        Text(
            text = "${width.toInt()}",
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
            color = if (isExpanded) color else MaterialTheme.colorScheme.primary
        )
    }
}
