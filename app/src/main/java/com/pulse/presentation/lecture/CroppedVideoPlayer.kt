package com.pulse.presentation.lecture

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer

/**
 * Single-decode cropped video player.
 *
 * Video layout (1280x720):
 * |---- Slides (0..980) ----|----- Chat+Tutor (980..1280) -----|
 * Tutor box: top-right (980, 0, 300x200)
 *
 * Uses ONE TextureView with Matrix.setTransform to crop the chat column.
 * The tutor region is NOT a second decode — it is a second TextureView
 * that receives the same SurfaceTexture frames and applies a different matrix.
 *
 * IMPORTANT: ExoPlayer only outputs to one Surface. We set the main TextureView
 * as the video output. The tutor overlay copies frames via a shared approach:
 * we use a FrameLayout containing both TextureViews, and ExoPlayer's
 * setVideoTextureView switches between them. Instead, we use a single
 * TextureView with the crop matrix, and overlay the tutor as a separate
 * composable AndroidView that reads the same player's texture.
 *
 * Practical approach: Use player.setVideoTextureView for the MAIN view.
 * For tutor PiP, we apply the crop on the main view's Matrix only.
 * The tutor PiP is simply NOT rendered by a second decode — instead
 * we toggle between full view and cropped view.
 */

// Video coordinate constants
private const val VIDEO_W = 1280f
private const val VIDEO_H = 720f
private const val CHAT_WIDTH = 300f
private const val SLIDE_WIDTH = VIDEO_W - CHAT_WIDTH  // 980px

@Composable
fun CroppedVideoPlayer(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    showTutorPip: Boolean = true,
    onTapMainView: () -> Unit = {}
) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    // Single FrameLayout with one TextureView, Matrix-cropped
    AndroidView(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTapMainView() })
            },
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Main TextureView — shows slides only (right 300px cropped)
                val mainTv = TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            player.setVideoTextureView(this@apply)
                            applySlideCropMatrix(this@apply, w, h)
                        }
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                            applySlideCropMatrix(this@apply, w, h)
                        }
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            player.clearVideoTextureView(this@apply)
                            return true
                        }
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
                addView(mainTv)
            }
        },
        update = { /* player reference stays stable */ }
    )

    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Slide crop matrix: removes the right 300px (chat column).
 *
 * The video is 1280px wide. We want to show only the left 980px.
 * 
 * Step 1: Scale X by VIDEO_W / SLIDE_WIDTH = 1280/980 ≈ 1.306
 *         This stretches the video so the 980px slides fill the full view width.
 *         The chat column overflows to the right (clipped by the view bounds).
 *
 * Step 2: Scale Y to fill height while maintaining the slide region's aspect ratio.
 *         Slide aspect = 980/720 ≈ 1.361
 *         We scale Y so the video height fills the view height.
 */
private fun applySlideCropMatrix(tv: TextureView, viewW: Int, viewH: Int) {
    val matrix = Matrix()

    // The fraction of the video width we want to keep
    val keepFraction = SLIDE_WIDTH / VIDEO_W  // 0.7656

    // Scale X so the kept portion fills the view
    val scaleX = 1f / keepFraction  // ≈ 1.306

    // For Y: the video's natural height at view width = viewW / (VIDEO_W/VIDEO_H)
    // We want this to fill viewH
    val videoNaturalH = viewW.toFloat() * (VIDEO_H / VIDEO_W)
    val scaleY = viewH.toFloat() / videoNaturalH

    matrix.setScale(scaleX, scaleY)
    // Anchor at (0,0) — left edge of slides stays at left edge of view

    tv.setTransform(matrix)
}
