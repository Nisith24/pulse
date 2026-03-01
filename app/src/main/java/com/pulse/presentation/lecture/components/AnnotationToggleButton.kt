package com.pulse.presentation.lecture.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnnotationToggleButton(
    isEditing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = if (isEditing) MaterialTheme.colorScheme.errorContainer 
                        else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isEditing) MaterialTheme.colorScheme.onErrorContainer 
                       else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.size(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Close else Icons.Default.DriveFileRenameOutline,
                contentDescription = "Toggle Annotation Tool",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
