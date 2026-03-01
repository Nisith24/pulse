package com.pulse.presentation.lecture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pulse.core.data.db.VisualType
import com.pulse.presentation.lecture.AnnotationState

@Composable
fun AnnotationToolbar(
    state: AnnotationState,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        VisualType.DRAWING to Icons.Default.Edit,
        VisualType.HIGHLIGHT to Icons.Default.Gesture,
        VisualType.TEXT to Icons.Default.TextFields,
        VisualType.STICKY_NOTE to Icons.Default.Note, // Changed Icon for compatibility
        VisualType.ERASER to Icons.Default.AutoFixNormal
    )

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black)

    Surface(
        modifier = modifier.padding(bottom = 8.dp, end = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 12.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Primary Tools
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tools.forEach { (type, icon) ->
                    val isSelected = state.currentTool == type
                    IconButton(
                        onClick = { state.currentTool = type },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else Color.Transparent, 
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.name,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Row 2: Colors & Quick Actions (Hidden for Eraser)
            if (state.currentTool != VisualType.ERASER) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { color ->
                        val isSelected = state.strokeColor == color
                        Surface(
                            onClick = { state.strokeColor = color },
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = color,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {}
                    }
                }
                
                // Compact Width Slider
                Slider(
                    value = state.strokeWidth,
                    onValueChange = { state.strokeWidth = it },
                    valueRange = 2f..20f,
                    modifier = Modifier.width(140.dp).height(24.dp)
                )
            }
        }
    }
}
