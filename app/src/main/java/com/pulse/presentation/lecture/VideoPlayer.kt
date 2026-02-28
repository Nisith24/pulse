package com.pulse.presentation.lecture

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material3.*
import android.content.pm.ActivityInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.media.AudioManager
import kotlin.math.roundToInt
import com.pulse.presentation.lecture.components.ControlsOverlay
import com.pulse.presentation.lecture.components.SettingsOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayer(
    player: ExoPlayer,
    title: String = "",
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    isPip: Boolean = false,
    onFullscreenToggle: () -> Unit = {},
    onPipClick: () -> Unit = {},
    onSpeedChanged: (Float) -> Unit = {}
) {
    var showControls by remember { mutableStateOf(!isPip) }
    var showSettings by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }

    var gestureType by remember { mutableStateOf("") }
    var gestureValue by remember { mutableFloatStateOf(0f) }

    // Orientation Lock state
    var isOrientationLocked by remember { mutableStateOf(false) }

    // Seekbar state
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    // Sync playing state
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }


    // Periodically update position from player
    LaunchedEffect(player) {
        while (true) {
            if (!isSeeking) {
                currentPosition = player.currentPosition.coerceAtLeast(0L)
            }
            duration = player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val isMinimal = containerWidth < 300.dp || containerHeight < 200.dp

        // ── Video Surface ──
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx).inflate(com.pulse.R.layout.texture_player_view, null) as PlayerView
                view.apply {
                    this.player = player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = player
                if (isPip) {
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    view.resizeMode = resizeMode
                }
            }
        )

        // ── Gesture Overlay (vertical only for brightness/volume + double-tap seek) ──
        if (!isPip) {
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                gestureType = if (offset.x < constraints.maxWidth / 2f) "brightness" else "volume"
                            },
                            onDragEnd = { gestureType = "" },
                            onDragCancel = { gestureType = "" },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -(dragAmount / constraints.maxHeight.toFloat())

                                if (gestureType == "brightness") {
                                    activity?.window?.let { window ->
                                        val lp = window.attributes
                                        var current = lp.screenBrightness
                                        if (current < 0) current = 0.5f
                                        lp.screenBrightness = (current + delta).coerceIn(0.01f, 1f)
                                        window.attributes = lp
                                        gestureValue = lp.screenBrightness
                                    }
                                } else if (gestureType == "volume") {
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val sensitivity = 2.0f
                                    val newVolFragment = ((currentVolume.toFloat() / maxVolume) + delta * sensitivity).coerceIn(0f, 1f)
                                    val newVolume = (newVolFragment * maxVolume).roundToInt()
                                    if (newVolume != currentVolume) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                    }
                                    gestureValue = newVolFragment
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                if (!showControls) showSettings = false
                            },
                            onDoubleTap = { offset ->
                                val seekOffset = 15000L
                                if (offset.x < constraints.maxWidth / 2f) {
                                    player.seekTo((player.currentPosition - seekOffset).coerceAtLeast(0))
                                    gestureType = "rw"
                                } else {
                                    player.seekTo((player.currentPosition + seekOffset).coerceAtMost(player.duration))
                                    gestureType = "ff"
                                }
                                scope.launch {
                                    delay(600)
                                    if (gestureType == "rw" || gestureType == "ff") gestureType = ""
                                }
                            }
                        )
                    }
            )

            // Auto-hide controls
            LaunchedEffect(showControls, showSettings) {
                if (showControls && !showSettings) {
                    delay(5000)
                    showControls = false
                }
            }
        }

        // ── Gesture Feedback Overlay ──
        AnimatedVisibility(
            visible = gestureType.isNotEmpty() && !isPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (gestureType) {
                        "brightness" -> {
                            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { gestureValue },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.width(80.dp).height(6.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("${(gestureValue * 100).roundToInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        "volume" -> {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { gestureValue },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.width(80.dp).height(6.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("${(gestureValue * 100).roundToInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        "rw" -> {
                            Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("-15s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        "ff" -> {
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("+15s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // ── Center Controls (RW, Play/Pause, FF) ──
        AnimatedVisibility(
            visible = showControls && !isPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isMinimal) 24.dp else 48.dp)
            ) {
                // Backward 15s
                IconButton(
                    onClick = { player.seekTo((player.currentPosition - 15000L).coerceAtLeast(0)) },
                    modifier = Modifier
                        .size(if (isMinimal) 48.dp else 64.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "-15s",
                        tint = Color.White,
                        modifier = Modifier.size(if (isMinimal) 28.dp else 36.dp)
                    )
                }

                // Play/Pause
                IconButton(
                    onClick = { 
                        if (isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier
                        .size(if (isMinimal) 72.dp else 96.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(if (isMinimal) 36.dp else 48.dp)
                    )
                }

                // Forward 15s
                IconButton(
                    onClick = { player.seekTo((player.currentPosition + 15000L).coerceAtMost(player.duration)) },
                    modifier = Modifier
                        .size(if (isMinimal) 48.dp else 64.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "+15s",
                        tint = Color.White,
                        modifier = Modifier.size(if (isMinimal) 28.dp else 36.dp)
                    )
                }
            }
        }

        // ── Controls Overlay (HIDDEN in PiP) ──

        ControlsOverlay(
            player = player,
            title = title,
            isPlaying = isPlaying,
            isMinimal = isMinimal,
            isPip = isPip,
            showControls = showControls,
            currentPosition = currentPosition,
            duration = duration,
            isOrientationLocked = isOrientationLocked,
            onOrientationToggle = {
                isOrientationLocked = !isOrientationLocked
                activity?.requestedOrientation = if (isOrientationLocked) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_USER
                }
            },
            onPipClick = onPipClick,
            onFullscreenToggle = onFullscreenToggle,
            onSettingsClick = { showSettings = !showSettings }
        )

        // Settings panel
        if (showSettings) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SettingsOverlay(
                    player = player,
                    isMinimal = isMinimal,
                    resizeMode = resizeMode,
                    onResizeModeChanged = { resizeMode = it },
                    onSpeedChanged = onSpeedChanged,
                    onDoneClick = { showSettings = false }
                )
            }
        }
    }
}
