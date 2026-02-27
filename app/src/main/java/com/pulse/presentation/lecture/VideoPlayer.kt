package com.pulse.presentation.lecture

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun VideoPlayer(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {},
    onPipClick: () -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {}
) {
    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    LaunchedEffect(showControls, showSettings) {
        if (showControls && !showSettings) {
            delay(3000)
            showControls = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    player.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Apply the modifier (which contains weight/size) to the outer container
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (showSettings) showSettings = false else showControls = !showControls
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    controllerAutoShow = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = resizeMode
                if (showControls) view.showController() else view.hideController()
            },
            onRelease = { view ->
                // Ensure media is cleared immediately when the view leaves the screen
                view.player?.stop()
                view.player?.clearMediaItems()
            }
        )

        AnimatedVisibility(visible = isBuffering, enter = fadeIn(), exit = fadeOut()) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        }

        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onPipClick,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    ) {
                        Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White)
                    }

                    IconButton(
                        onClick = onFullscreenToggle,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    ) {
                        Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                    }
                    
                    IconButton(
                        onClick = { showSettings = !showSettings },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                if (showSettings) {
                    Card(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text("Advanced Player Settings", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            
                            Text("Playback Speed", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                                listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    FilterChip(
                                        selected = player.playbackParameters.speed == speed,
                                        onClick = { onSpeedChanged(speed) },
                                        label = { Text("${speed}x") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Aspect Ratio", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                                listOf("Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fill" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL)
                                    .forEach { (label, mode) ->
                                    FilterChip(
                                        selected = resizeMode == mode,
                                        onClick = { resizeMode = mode },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showSettings = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                        }
                    }
                }
            }
        }
    }
}
