package com.pulse.presentation.lecture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.core.data.db.VisualType
import com.pulse.presentation.lecture.AnnotationState

/**
 * Standard Page Indicator for PDF viewing
 */
@Composable
fun PdfPageIndicator(
    currentPage: Int,
    totalPages: Int,
    isNotebook: Boolean,
    onAddPage: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${currentPage + 1} / $totalPages",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (isNotebook) {
                IconButton(
                    onClick = onAddPage,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Pages",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Premium Dropdown Settings Menu for PDF Stack
 */
@Composable
fun PdfSettingsMenu(
    state: AnnotationState,
    onClose: () -> Unit,
    isHorizontal: Boolean,
    onOrientationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val tools = listOf(
        VisualType.DRAWING to Icons.Default.Edit,
        VisualType.HIGHLIGHT to Icons.Default.Gesture,
        VisualType.ERASER to Icons.Default.Delete
    )
    
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black)

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = if (state.isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = if (state.isDrawingMode) Icons.Default.Edit else Icons.Default.Settings,
                contentDescription = "PDF Overlay Settings"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp)
        ) {
            // ── ANNOTATION MODE TOGGLE ──
            DropdownMenuItem(
                text = { Text(if (state.isDrawingMode) "Disable Markers" else "Enable Annotation Mode") },
                onClick = { 
                    state.isDrawingMode = !state.isDrawingMode
                    expanded = false
                },
                leadingIcon = { 
                    Icon(if (state.isDrawingMode) Icons.Default.Close else Icons.Default.Edit, contentDescription = null) 
                }
            )

            if (state.isDrawingMode) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // ── TOOL SELECTION ──
                ListItem(
                    headlineContent = { Text("Tools", style = MaterialTheme.typography.labelSmall) },
                    supportingContent = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            tools.forEach { (type, icon) ->
                                InputChip(
                                    selected = state.activeTool == type,
                                    onClick = { state.activeTool = type },
                                    label = { Icon(icon, null, Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // ── COLOR SELECTION ──
                ListItem(
                    headlineContent = { Text("Colors", style = MaterialTheme.typography.labelSmall) },
                    supportingContent = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            colors.forEach { color ->
                                Surface(
                                    onClick = { state.strokeColor = color },
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = color,
                                    border = if (state.strokeColor == color) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {}
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // ── STROKE WIDTH ──
                Text(
                    "Weight: ${state.strokeWidth.toInt()}", 
                    style = MaterialTheme.typography.labelSmall, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Slider(
                    value = state.strokeWidth,
                    onValueChange = { state.strokeWidth = it },
                    valueRange = 2f..25f,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── ORIENTATION ──
            DropdownMenuItem(
                text = { Text(if (isHorizontal) "Vertical Scroll" else "Horizontal Swipe") },
                onClick = { 
                    onOrientationChange(!isHorizontal)
                    expanded = false
                },
                leadingIcon = { 
                    Icon(if (isHorizontal) Icons.Default.ViewDay else Icons.Default.ViewWeek, contentDescription = null) 
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── ACTIONS ──
            DropdownMenuItem(
                text = { Text("Close Workspace", color = MaterialTheme.colorScheme.error) },
                onClick = { 
                    onClose()
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}
