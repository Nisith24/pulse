package com.pulse.presentation.lecture.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout

@Composable
fun SettingsOverlay(
    player: ExoPlayer,
    isMinimal: Boolean,
    resizeMode: Int,
    doubleTapSeekOffset: Long,
    longPressSpeed: Float,
    onResizeModeChanged: (Int) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onDoubleTapSeekOffsetChanged: (Long) -> Unit,
    onLongPressSpeedChanged: (Float) -> Unit,
    onDoneClick: () -> Unit
) {

    var localSpeed by remember(player.playbackParameters.speed) { mutableStateOf(player.playbackParameters.speed) }
    var localSeekOffset by remember(doubleTapSeekOffset) { mutableStateOf(doubleTapSeekOffset.toFloat()) }
    var localLongPressSpeed by remember(longPressSpeed) { mutableStateOf(longPressSpeed) }
    Card(
        modifier = Modifier
            .padding(if (isMinimal) 8.dp else 32.dp)
            .widthIn(max = 400.dp)
            .heightIn(max = 600.dp),
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

            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Text("Playback Speed: ${String.format("%.1fx", localSpeed)}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = localSpeed,
                    onValueChange = { localSpeed = (it * 2).roundToInt() / 2f }, // Snap to 0.5 steps
                    onValueChangeFinished = { onSpeedChanged(localSpeed) },
                    valueRange = 0.5f..3.0f,
                    steps = 4, // 0.5, 1.0, 1.5, 2.0, 2.5, 3.0
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(if (isMinimal) 12.dp else 20.dp))

                Text("Double Tap Seek Offset: ${(localSeekOffset / 1000).toInt()}s", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = localSeekOffset,
                    onValueChange = { localSeekOffset = ((it / 5000f).roundToInt() * 5000f).coerceIn(5000f, 30000f) }, // Snap to 5s steps
                    onValueChangeFinished = { onDoubleTapSeekOffsetChanged(localSeekOffset.toLong()) },
                    valueRange = 5000f..30000f,
                    steps = 4, // 5 intermediate: 10s, 15s, 20s, 25s (endpoints: 5s, 30s)
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(if (isMinimal) 12.dp else 20.dp))

                Text("Long Press Speed: ${String.format("%.1fx", localLongPressSpeed)}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = localLongPressSpeed,
                    onValueChange = { localLongPressSpeed = (it * 2).roundToInt() / 2f }, // Snap to 0.5 steps
                    onValueChangeFinished = { onLongPressSpeedChanged(localLongPressSpeed) },
                    valueRange = 1.5f..3.0f,
                    steps = 2, // 2.0, 2.5 (endpoints: 1.5, 3.0)
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

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
}
