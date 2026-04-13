package com.pulse.presentation.lecture.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────
// Reusable slider + inline numeric input that stay in sync
// ─────────────────────────────────────────────────────────────────
@Composable
private fun SliderWithInput(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayText: String,
    inputSuffix: String = "",
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    onInputConfirm: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var inputText by remember(displayText) { mutableStateOf(displayText) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it.filter { c -> c.isDigit() || c == '.' } },
                suffix = {
                    if (inputSuffix.isNotEmpty()) Text(
                        inputSuffix,
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.width(86.dp).height(48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onInputConfirm(inputText)
                    focusManager.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = MaterialTheme.shapes.small
            )
        }

        Slider(
            value = value,
            onValueChange = onSliderChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Main settings panel
// ─────────────────────────────────────────────────────────────────
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
    var localSeekMs by remember(doubleTapSeekOffset) { mutableStateOf(doubleTapSeekOffset.toFloat()) }
    var localLpSpeed by remember(longPressSpeed) { mutableStateOf(longPressSpeed) }

    Card(
        modifier = Modifier
            .padding(if (isMinimal) 8.dp else 28.dp)
            .widthIn(max = 420.dp)
            .heightIn(max = 640.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
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

            Spacer(Modifier.height(if (isMinimal) 10.dp else 16.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 1. PLAYBACK SPEED ─────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SliderWithInput(
                        label = "Playback Speed",
                        value = localSpeed,
                        valueRange = 0.25f..3.0f,
                        steps = 10,
                        displayText = String.format("%.2f", localSpeed),
                        inputSuffix = "x",
                        onSliderChange = { localSpeed = (it * 4).roundToInt() / 4f },
                        onSliderChangeFinished = { onSpeedChanged(localSpeed) },
                        onInputConfirm = { text ->
                            text.toFloatOrNull()?.let {
                                localSpeed = it.coerceIn(0.25f, 3.0f)
                                onSpeedChanged(localSpeed)
                            }
                        }
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { sp ->
                            val sel = kotlin.math.abs(localSpeed - sp) < 0.01f
                            FilterChip(
                                selected = sel,
                                onClick = { localSpeed = sp; onSpeedChanged(sp) },
                                label = { Text("${sp}x", style = MaterialTheme.typography.labelSmall, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    labelColor = Color.White.copy(alpha = 0.8f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = sel,
                                    borderColor = Color.White.copy(alpha = 0.2f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // ── 2. DOUBLE TAP SEEK ────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SliderWithInput(
                        label = "Double Tap Seek",
                        value = localSeekMs,
                        valueRange = 5000f..60000f,
                        steps = 10,
                        displayText = (localSeekMs / 1000f).roundToInt().toString(),
                        inputSuffix = "s",
                        onSliderChange = { localSeekMs = ((it / 5000f).roundToInt() * 5000f).coerceIn(5000f, 60000f) },
                        onSliderChangeFinished = { onDoubleTapSeekOffsetChanged(localSeekMs.toLong()) },
                        onInputConfirm = { text ->
                            text.toIntOrNull()?.let {
                                localSeekMs = (it * 1000f).coerceIn(1000f, 120000f)
                                onDoubleTapSeekOffsetChanged(localSeekMs.toLong())
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30, 45, 60).forEach { s ->
                            val sel = (localSeekMs / 1000f).roundToInt() == s
                            FilterChip(
                                selected = sel,
                                onClick = { localSeekMs = s * 1000f; onDoubleTapSeekOffsetChanged(localSeekMs.toLong()) },
                                label = { Text("${s}s", style = MaterialTheme.typography.labelSmall, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    labelColor = Color.White.copy(alpha = 0.8f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = sel,
                                    borderColor = Color.White.copy(alpha = 0.2f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // ── 3. LONG PRESS SPEED ───────────────────────────────
                SliderWithInput(
                    label = "Long Press Speed",
                    value = localLpSpeed,
                    valueRange = 1.0f..4.0f,
                    steps = 5,
                    displayText = String.format("%.1f", localLpSpeed),
                    inputSuffix = "x",
                    onSliderChange = { localLpSpeed = (it * 2).roundToInt() / 2f },
                    onSliderChangeFinished = { onLongPressSpeedChanged(localLpSpeed) },
                    onInputConfirm = { text ->
                        text.toFloatOrNull()?.let {
                            localLpSpeed = it.coerceIn(1.0f, 4.0f)
                            onLongPressSpeedChanged(localLpSpeed)
                        }
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // ── 4. ASPECT RATIO ───────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aspect Ratio", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                            "Fill" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                            "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL
                        ).forEach { (label, mode) ->
                            val sel = resizeMode == mode
                            Surface(
                                onClick = { onResizeModeChanged(mode) },
                                color = if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── DONE ──────────────────────────────────────────────
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
