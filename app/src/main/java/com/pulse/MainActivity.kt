package com.pulse

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.Player
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.presentation.auth.LoginScreen
import com.pulse.presentation.downloads.DownloadsScreen
import com.pulse.presentation.lecture.LectureScreen
import com.pulse.presentation.lecture.PlayerProvider
import com.pulse.presentation.library.LibraryScreen
import com.pulse.presentation.navigation.*
import com.pulse.presentation.settings.ProfileSettingsScreen
import com.pulse.presentation.theme.PulseTheme
import org.koin.android.ext.android.inject
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

class MainActivity : ComponentActivity() {
    private val authManager: IBtrAuthManager by inject()
    private val playerProvider: PlayerProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isSignedIn by remember { mutableStateOf(authManager.isSignedIn) }

                    val signInLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                            isSignedIn = true
                        } catch (e: com.google.android.gms.common.api.ApiException) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(this@MainActivity, "Sign-in failed: ${e.statusCode}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    val scope = rememberCoroutineScope()
                    if (!isSignedIn) {
                        LoginScreen(onSignInClick = {
                            val googleAuth = authManager as? com.pulse.data.services.btr.GoogleAuthManager
                            if (googleAuth != null) {
                                val intent = GoogleSignIn.getClient(this, googleAuth.getSignInOptions()).signInIntent
                                signInLauncher.launch(intent)
                            }
                        })
                    } else {
                        PulseAppContent(
                            onSignOut = {
                                scope.launch {
                                    authManager.signOut()
                                    isSignedIn = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Industry Standard: Auto-PiP on home press ──
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfPlaying()
    }

    // ── Industry Standard: Update PiP params for smooth transitions ──
    private fun enterPipIfPlaying() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val player = playerProvider.player
        if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
            try {
                val params = buildPipParams(player)
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to enter PiP", e)
            }
        }
    }

    private fun buildPipParams(player: Player): PictureInPictureParams {
        val videoSize = player.videoSize
        val aspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
            Rational(videoSize.width, videoSize.height)
        } else {
            Rational(16, 9)
        }
        
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)

        // Android 12+: setAutoEnterEnabled for seamless gesture nav PiP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
        }
        
        return builder.build()
    }

    // ── Keep PiP params updated whenever video is playing ──
    override fun onResume() {
        super.onResume()
        updatePipParams()
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val player = playerProvider.player
        if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
            try {
                setPictureInPictureParams(buildPipParams(player))
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun PulseAppContent(onSignOut: () -> Unit = {}) {
    val navController = rememberNavController()
    val playerProvider: PlayerProvider = org.koin.compose.koinInject()
    val miniPlayerId by playerProvider.miniPlayerFlow.collectAsState()

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = LibraryRoute) {
            composable<LibraryRoute> {
                LibraryScreen(
                    onLectureSelected = { id ->
                        if (playerProvider.miniPlayerLectureId == id) {
                            playerProvider.clearMiniPlayerState()
                        } else if (playerProvider.isMiniPlayerActive) {
                            playerProvider.closeMiniPlayer()
                        }
                        navController.navigate(LectureRoute(id))
                    },
                    onNavigateToSettings = { navController.navigate(SettingsRoute) },
                    onNavigateToDownloads = { navController.navigate(DownloadsRoute) }
                )
            }
            composable<DownloadsRoute> {
                DownloadsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLecture = { id -> navController.navigate(LectureRoute(id)) }
                )
            }
            composable<SettingsRoute> {
                ProfileSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDownloads = { navController.navigate(DownloadsRoute) },
                    onSignOut = onSignOut
                )
            }
            composable<LectureRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<LectureRoute>()
                key(route.lectureId) {
                    LectureScreen(
                        lectureId = route.lectureId,
                        onNavigateBack = {
                            playerProvider.activateMiniPlayer(route.lectureId, "")
                            navController.popBackStack(LibraryRoute, inclusive = false)
                        }
                    )
                }
            }
        }

        // Mini-player overlay
        val currentBackStack by navController.currentBackStackEntryFlow.collectAsState(initial = null)
        val isLectureScreen = currentBackStack?.destination?.route?.contains("LectureRoute") == true
        
        if (!isLectureScreen && miniPlayerId != null) {
            com.pulse.presentation.lecture.MiniPlayer(
                player = playerProvider.player,
                onClose = {
                    playerProvider.closeMiniPlayer()
                },
                onExpand = {
                    val id = playerProvider.miniPlayerLectureId
                    playerProvider.clearMiniPlayerState()
                    if (id != null) {
                        navController.navigate(LectureRoute(id))
                    }
                }
            )
        }
    }
}
