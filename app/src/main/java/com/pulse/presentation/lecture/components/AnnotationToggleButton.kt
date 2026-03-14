package com.pulse.presentation.lecture.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun AnnotationToggleButton(
    isEditing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(isEditing, label = "annotation_toggle")

    val containerColor by transition.animateColor(
        transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
        label = "container"
    ) {
        if (it) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor by transition.animateColor(
        transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
        label = "content"
    ) {
        if (it) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimaryContainer
    }

    val rotation by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium) },
        label = "rotation"
    ) { if (it) 90f else 0f }

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium) },
        label = "scale"
    ) { if (it) 1.05f else 1f }

    val elevation by transition.animateDp(
        transitionSpec = { tween(280) },
        label = "elevation"
    ) { if (it) 8.dp else 4.dp }

    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = elevation,
            pressedElevation = elevation + 2.dp
        ),
        modifier = modifier
            .size(48.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Cross-fade between icons with rotation
            androidx.compose.animation.AnimatedContent(
                targetState = isEditing,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(200)
                    )).togetherWith(
                        fadeOut(tween(150)) + scaleOut(
                            targetScale = 0.6f,
                            animationSpec = tween(150)
                        )
                    )
                },
                label = "icon_switch"
            ) { editing ->
                Icon(
                    imageVector = if (editing) Icons.Default.Close
                    else Icons.Default.DriveFileRenameOutline,
                    contentDescription = if (editing) "Stop Annotating" else "Start Annotating",
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (editing) rotation else 0f)
                )
            }
        }
    }
}
