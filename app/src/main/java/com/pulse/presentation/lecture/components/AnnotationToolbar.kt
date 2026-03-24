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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.core.data.db.VisualType
import com.pulse.presentation.lecture.AnnotationState

// ── Industry-Standard Color Palette (Apple/Samsung Notes style) ──
private val ToolbarColors = listOf(
    Color(0xFFE53935), // Scarlet
    Color(0xFFF4511E), // Deep Orange
    Color(0xFFFF9800), // Amber
    Color(0xFFFDD835), // Vivid Yellow
    Color(0xFF43A047), // Forest Green
    Color(0xFF00897B), // Teal
    Color(0xFF1E88E5), // Ocean Blue
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFFAD1457), // Magenta
    Color(0xFF424242), // Graphite
    Color(0xFFECEFF1), // Off White
)

private data class ToolItem(
    val type: VisualType,
    val icon: ImageVector,
    val label: String
)

private val PrimaryTools = listOf(
    ToolItem(VisualType.DRAWING, Icons.Outlined.Edit, "Pen"),
    ToolItem(VisualType.HIGHLIGHT, Icons.Outlined.Brush, "Highlight"),
    ToolItem(VisualType.RULER, Icons.Outlined.HorizontalRule, "Line"),
    ToolItem(VisualType.BOX, Icons.Outlined.CheckBoxOutlineBlank, "Shape"),
    ToolItem(VisualType.ERASER, Icons.Outlined.AutoFixHigh, "Eraser"),
)

@Composable
fun AnnotationToolbar(
    state: AnnotationState,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf(ExpandedSection.NONE) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Collapse panels when switching tools to keep UI clean
    LaunchedEffect(state.currentTool) {
        if (state.currentTool == VisualType.ERASER) {
            expandedSection = ExpandedSection.NONE
        }
        // Auto-set defaults for Highlighter
        if (state.currentTool == VisualType.HIGHLIGHT) {
            state.strokeAlpha = 0.4f
            state.strokeWidth = 15f
        } else if (state.currentTool != VisualType.ERASER) {
            state.strokeAlpha = 1.0f
        }
    }

    // ── Main Toolbar Container ──
    Column(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.99f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(28.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Expandable Sections (Color, Stroke, Opacity) ──
        ExpandableSectionContent(
            expandedSection = expandedSection,
            state = state
        )

        if (expandedSection != ExpandedSection.NONE) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }

        // ── Main Row: Tools + Undo/Redo + System ──
        MainToolRow(
            state = state,
            expandedSection = expandedSection,
            onSectionExpanded = { newSection -> expandedSection = newSection }
        )

        // ── Drag Handle ──
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .width(28.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

private enum class ExpandedSection { NONE, COLOR, STROKE, OPACITY }

@Composable
private fun ExpandableSectionContent(
    expandedSection: ExpandedSection,
    state: AnnotationState
) {
    val haptic = LocalHapticFeedback.current

    AnimatedContent(
        targetState = expandedSection,
        transitionSpec = {
            (fadeIn(tween(220)) + expandVertically(tween(220))).togetherWith(
                fadeOut(tween(180)) + shrinkVertically(tween(180))
            )
        },
        label = "expanded_content"
    ) { section ->
        when (section) {
            ExpandedSection.COLOR -> {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionHeader("COLOR PALETTE")
                    ColorPickerGrid(
                        selectedColor = state.strokeColor,
                        onColorSelected = {
                            state.strokeColor = it
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
            ExpandedSection.STROKE -> {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionHeader("STROKE WEIGHT")
                    StrokeWidthSelector(
                        width = state.strokeWidth,
                        color = state.strokeColor,
                        onWidthChange = { state.strokeWidth = it }
                    )

                    if (state.currentTool == VisualType.BOX) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                                .clickable { state.fillEnabled = !state.fillEnabled }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fill Shape", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = state.fillEnabled,
                                onCheckedChange = { state.fillEnabled = it },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }
                }
            }
            ExpandedSection.OPACITY -> {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionHeader("OPACITY / ALPHA")
                    OpacitySelector(
                        alpha = state.strokeAlpha,
                        color = state.strokeColor,
                        onAlphaChange = { state.strokeAlpha = it }
                    )
                }
            }
            ExpandedSection.NONE -> Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
private fun MainToolRow(
    state: AnnotationState,
    expandedSection: ExpandedSection,
    onSectionExpanded: (ExpandedSection) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo Button
        IconButton(
            onClick = { /* ViewModel should handle this, passed via lambda */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Undo,
                contentDescription = "Undo",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        PrimaryTools.forEach { tool ->
            ToolButton(
                icon = tool.icon,
                label = tool.label,
                isSelected = state.currentTool == tool.type,
                activeColor = if (tool.type == VisualType.ERASER)
                    MaterialTheme.colorScheme.error
                else
                    state.strokeColor,
                onClick = {
                    state.currentTool = tool.type
                    state.activeTool = tool.type
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (tool.type == VisualType.ERASER) {
                        onSectionExpanded(ExpandedSection.NONE)
                    }
                }
            )
        }

        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(24.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        if (state.currentTool != VisualType.ERASER) {
            // Color Button
            ColorIndicatorButton(
                color = state.strokeColor,
                isExpanded = expandedSection == ExpandedSection.COLOR,
                onClick = {
                    onSectionExpanded(
                        if (expandedSection == ExpandedSection.COLOR)
                            ExpandedSection.NONE else ExpandedSection.COLOR
                    )
                }
            )

            // Stroke Button
            StrokeIndicatorButton(
                width = state.strokeWidth,
                color = state.strokeColor,
                isExpanded = expandedSection == ExpandedSection.STROKE,
                onClick = {
                    onSectionExpanded(
                        if (expandedSection == ExpandedSection.STROKE)
                            ExpandedSection.NONE else ExpandedSection.STROKE
                    )
                }
            )

            // Opacity Button
            OpacityIndicatorButton(
                alpha = state.strokeAlpha,
                color = state.strokeColor,
                isExpanded = expandedSection == ExpandedSection.OPACITY,
                onClick = {
                    onSectionExpanded(
                        if (expandedSection == ExpandedSection.OPACITY)
                            ExpandedSection.NONE else ExpandedSection.OPACITY
                    )
                }
            )
        }

        // Redo Button
        IconButton(
            onClick = { /* ViewModel redo */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Redo,
                contentDescription = "Redo",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Tool Button ──
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val transition = updateTransition(isSelected, label = "tool_transition")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium) },
        label = "scale"
    ) { if (it) 1.1f else 1f }

    val bgColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "bg"
    ) { if (it) activeColor.copy(alpha = 0.14f) else Color.Transparent }

    val iconTint by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "tint"
    ) { if (it) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) }

    val labelColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "label_tint"
    ) { if (it) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .background(bgColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.3.sp
            ),
            color = labelColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )

        // Active indicator line
        AnimatedVisibility(
            visible = isSelected,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(14.dp)
                    .height(2.dp)
                    .background(activeColor, RoundedCornerShape(1.dp))
            )
        }
    }
}

// ── Color Picker Grid ──
@Composable
private fun ColorPickerGrid(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    // Display in 2 rows for better layout
    val firstRow = ToolbarColors.take(6)
    val secondRow = ToolbarColors.drop(6)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            firstRow.forEach { color ->
                ColorSwatch(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorSelected(color) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            secondRow.forEach { color ->
                ColorSwatch(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "swatch_scale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else 0.dp,
        animationSpec = tween(200),
        label = "swatch_border"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "check_alpha"
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color, CircleShape)
            .then(
                if (isSelected) Modifier.border(
                    width = borderWidth,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) else Modifier.border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.12f),
                    shape = CircleShape
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checkAlpha > 0f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (color == Color(0xFFECEFF1) || color == Color(0xFFFDD835))
                    Color.Black.copy(alpha = checkAlpha)
                else
                    Color.White.copy(alpha = checkAlpha)
            )
        }
    }
}

// ── Stroke Width Selector ──
@Composable
private fun StrokeWidthSelector(
    width: Float,
    color: Color,
    onWidthChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live preview dot
        Box(
            modifier = Modifier
                .size(width.dp.coerceIn(4.dp, 26.dp))
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {}

        Slider(
            value = width,
            onValueChange = onWidthChange,
            valueRange = 1f..25f,
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.8f),
                inactiveTrackColor = color.copy(alpha = 0.15f)
            )
        )

        // Width value badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f),
            contentColor = color,
            modifier = Modifier.widthIn(min = 32.dp)
        ) {
            Text(
                text = "${width.toInt()}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Color Indicator Button ──
@Composable
private fun ColorIndicatorButton(
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val transition = updateTransition(isExpanded, label = "color_btn")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.75f) },
        label = "scale"
    ) { if (it) 1.1f else 1f }

    val borderWidth by transition.animateDp(
        transitionSpec = { tween(200) },
        label = "border"
    ) { if (it) 2.5.dp else 1.dp }

    val borderColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "border_color"
    ) {
        if (it) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .padding(5.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {}
}

// ── Stroke Indicator Button ──
@Composable
private fun StrokeIndicatorButton(
    width: Float,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val transition = updateTransition(isExpanded, label = "stroke_btn")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.75f) },
        label = "scale"
    ) { if (it) 1.1f else 1f }

    val bgColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "bg"
    ) { if (it) color.copy(alpha = 0.12f) else Color.Transparent }

    val borderColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "border_color"
    ) {
        if (it) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LineWeight,
            contentDescription = "Stroke Width",
            modifier = Modifier.size(18.dp),
            tint = if (isExpanded) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// ── Opacity Selector ──
@Composable
private fun OpacitySelector(
    alpha: Float,
    color: Color,
    onAlphaChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Opacity preview square
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = alpha))
        )

        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0.1f..1f,
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.8f),
                inactiveTrackColor = color.copy(alpha = 0.15f)
            )
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.widthIn(min = 44.dp)
        ) {
            Text(
                text = "${(alpha * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Opacity Indicator Button ──
@Composable
private fun OpacityIndicatorButton(
    alpha: Float,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val transition = updateTransition(isExpanded, label = "opacity_btn")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.75f) },
        label = "scale"
    ) { if (it) 1.1f else 1f }

    val borderColor by transition.animateColor(
        transitionSpec = { tween(200) },
        label = "border_color"
    ) {
        if (it) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Opacity,
            contentDescription = "Opacity",
            modifier = Modifier.size(18.dp),
            tint = if (isExpanded) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f).copy(alpha = alpha.coerceAtLeast(0.3f))
        )
    }
}
