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
        VisualType.ERASER to Icons.Default.Clear // Eraser icon
    )

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black)

    Surface(
        modifier = modifier.padding(bottom = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tool Selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tools.forEach { (type, icon) ->
                    val isSelected = state.activeTool == type
                    IconButton(
                        onClick = { state.activeTool = type },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else Color.Transparent, 
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.name,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Color Selection
            if (state.activeTool != VisualType.ERASER) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { color ->
                        val isSelected = state.strokeColor == color
                        Surface(
                            onClick = { state.strokeColor = color },
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = color,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
                        ) {}
                    }
                }

                // Stroke Width
                Row(
                    modifier = Modifier.width(200.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = state.strokeWidth,
                        onValueChange = { state.strokeWidth = it },
                        valueRange = 2f..30f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
