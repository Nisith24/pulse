package com.pulse.presentation.lecture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun ControlsOverlay(
    player: ExoPlayer,
    title: String,
    isPlaying: Boolean,
    isMinimal: Boolean,
    isPip: Boolean,
    showControls: Boolean,
    currentPosition: Long,
    duration: Long,
    isOrientationLocked: Boolean,
    onOrientationToggle: () -> Unit,
    onPipClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }

    AnimatedVisibility(visible = showControls && !isPip, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize()) {
            // Title
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = Color.White,
                    style = if (isMinimal) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(if (isMinimal) 8.dp else 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bottom Seekbar + Time + Buttons 
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(bottom = 4.dp)
            ) {
                // Seekbar
                val displayPosition = if (isSeeking) seekPosition else currentPosition
                val sliderValue = if (duration > 0) displayPosition.toFloat() / duration.toFloat() else 0f

                Slider(
                    value = sliderValue,
                    onValueChange = { fraction ->
                        isSeeking = true
                        seekPosition = (fraction * duration).toLong()
                    },
                    onValueChangeFinished = {
                        player.seekTo(seekPosition)
                        isSeeking = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(44.dp), // Increased height for better touch target
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )

                // Time + buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time display
                    Text(
                        text = "${formatTime(displayPosition)} / ${formatTime(duration)}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.weight(1f))

                    // Bottom Play/Pause small button
                    IconButton(
                        onClick = { if (isPlaying) player.pause() else player.play() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val iconSize = if (isMinimal) 30.dp else 36.dp
                    val innerIcon = if (isMinimal) 18.dp else 22.dp

                    IconButton(onClick = onOrientationToggle, modifier = Modifier.size(iconSize)) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Orientation Lock",
                            tint = if (isOrientationLocked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(innerIcon)
                        )
                    }
                    IconButton(onClick = onPipClick, modifier = Modifier.size(iconSize)) {
                        Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White, modifier = Modifier.size(innerIcon))
                    }
                    IconButton(onClick = onFullscreenToggle, modifier = Modifier.size(iconSize)) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(innerIcon))
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(iconSize)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(innerIcon))
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
