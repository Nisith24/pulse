package com.pulse.presentation.lecture.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout

@Composable
fun SettingsOverlay(
    player: ExoPlayer,
    isMinimal: Boolean,
    resizeMode: Int,
    onResizeModeChanged: (Int) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onDoneClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(if (isMinimal) 8.dp else 32.dp)
            .widthIn(max = 400.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            Modifier
                .padding(if (isMinimal) 16.dp else 24.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Video Settings",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(if (isMinimal) 12.dp else 20.dp))

            Text("Playback Speed", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = player.playbackParameters.speed == speed
                    Surface(
                        onClick = { onSpeedChanged(speed) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "${speed}x",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (isMinimal) 12.dp else 20.dp))

            Text("Aspect Ratio", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    "Fill" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                    "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL
                ).forEach { (label, mode) ->
                    val isSelected = resizeMode == mode
                    Surface(
                        onClick = { onResizeModeChanged(mode) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (isMinimal) 16.dp else 24.dp))
            Button(
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
