package com.pulse.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.pulse.data.local.SettingsManager
import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.presentation.theme.ThemeViewModel
import com.pulse.presentation.theme.ThemeMode
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onSignOut: () -> Unit,
    themeViewModel: ThemeViewModel = koinViewModel()
) {
    val settingsManager: SettingsManager = koinInject()
    val authManager: IBtrAuthManager = koinInject()
    val scrollState = rememberScrollState()
    val themeMode by themeViewModel.themeMode.collectAsState()

    val scope = rememberCoroutineScope()
    
    // Settings states
    val autoPipEnabled by settingsManager.autoPipEnabledFlow.collectAsState(initial = true)
    val resumePlayback by settingsManager.resumePlaybackFlow.collectAsState(initial = true)
    val backgroundPlayback by settingsManager.backgroundPlaybackFlow.collectAsState(initial = true)
    val defaultSpeed by settingsManager.defaultSpeedFlow.collectAsState(initial = 1.0f)
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var cloudSyncEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    val videoQuality by settingsManager.videoQualityFlow.collectAsState(initial = "Auto")
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ══════════════════════════════════════════════
            // PROFILE CARD
            // ══════════════════════════════════════════════
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (authManager.displayName?.firstOrNull() ?: 'P').toString().uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authManager.displayName ?: "Pulse User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = authManager.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Google Account",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════
            // PLAYBACK SETTINGS
            // ══════════════════════════════════════════════
            SettingsSectionHeader("Playback")

            SettingsToggleItem(
                icon = Icons.Default.PictureInPicture,
                title = "Auto Picture-in-Picture",
                subtitle = "Enter PiP when leaving the app during playback",
                checked = autoPipEnabled,
                onCheckedChange = { scope.launch { settingsManager.saveAutoPipEnabled(it) } }
            )

            SettingsToggleItem(
                icon = Icons.Default.Replay,
                title = "Resume Playback",
                subtitle = "Continue from where you left off",
                checked = resumePlayback,
                onCheckedChange = { scope.launch { settingsManager.saveResumePlayback(it) } }
            )

            SettingsToggleItem(
                icon = Icons.Default.Headphones,
                title = "Background Playback",
                subtitle = "Continue audio when the app is in background",
                checked = backgroundPlayback,
                onCheckedChange = { scope.launch { settingsManager.saveBackgroundPlayback(it) } }
            )

            SettingsClickItem(
                icon = Icons.Default.Speed,
                title = "Default Playback Speed",
                subtitle = "${defaultSpeed}x",
                onClick = { showSpeedDialog = true }
            )

            val currentContext = LocalContext.current
            SettingsClickItem(
                icon = Icons.Default.HighQuality,
                title = "Video Quality",
                subtitle = "Auto (Locked for now)",
                onClick = { 
                    android.widget.Toast.makeText(currentContext, "Only Auto quality is supported in this version", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            SettingsDivider()

            // ══════════════════════════════════════════════
            // SYNC & CLOUD
            // ══════════════════════════════════════════════
            SettingsSectionHeader("Cloud & Sync")

            SettingsToggleItem(
                icon = Icons.Default.CloudSync,
                title = "Cross-Device Sync",
                subtitle = "Sync progress, notes & settings across devices",
                checked = cloudSyncEnabled,
                onCheckedChange = { cloudSyncEnabled = it }
            )

            SettingsClickItem(
                icon = Icons.Default.Backup,
                title = "Backup Notes",
                subtitle = "Backup all notes to cloud",
                onClick = { /* TODO: Firestore backup */ }
            )

            SettingsClickItem(
                icon = Icons.Default.Restore,
                title = "Restore Data",
                subtitle = "Restore progress & notes from cloud",
                onClick = { /* TODO: Firestore restore */ }
            )

            SettingsDivider()

            // ══════════════════════════════════════════════
            // APPEARANCE
            // ══════════════════════════════════════════════
            SettingsSectionHeader("Appearance")

            SettingsClickItem(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = when(themeMode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.SYSTEM -> "System"
                },
                onClick = { showThemeDialog = true }
            )

            SettingsDivider()

            // ══════════════════════════════════════════════
            // NOTIFICATIONS
            // ══════════════════════════════════════════════
            SettingsSectionHeader("Notifications")

            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Study Reminders",
                subtitle = "Get reminded to continue studying",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            SettingsDivider()

            // ══════════════════════════════════════════════
            // STORAGE
            // ══════════════════════════════════════════════
            SettingsSectionHeader("Storage")

            SettingsClickItem(
                icon = Icons.Default.CloudDownload,
                title = "Downloads Manager",
                subtitle = "Manage offline videos and save to device",
                onClick = onNavigateToDownloads
            )

            SettingsClickItem(
                icon = Icons.Default.CleaningServices,
                title = "Clear Video Cache",
                subtitle = "Free up space by clearing cached video data",
                onClick = { /* TODO: Clear cache */ }
            )

            SettingsClickItem(
                icon = Icons.Default.FolderDelete,
                title = "Clear Downloaded PDFs",
                subtitle = "Remove offline PDF copies",
                onClick = { /* TODO: Clear PDFs */ }
            )

            SettingsDivider()

            // ══════════════════════════════════════════════
            // ABOUT & SUPPORT
            // ══════════════════════════════════════════════
            SettingsSectionHeader("About")

            SettingsClickItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = "1.0.0",
                onClick = { }
            )

            SettingsClickItem(
                icon = Icons.Default.Policy,
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = { /* TODO: Open web link */ }
            )

            SettingsClickItem(
                icon = Icons.Default.Description,
                title = "Terms of Service",
                subtitle = "View terms of service",
                onClick = { /* TODO: Open web link */ }
            )

            SettingsClickItem(
                icon = Icons.Default.BugReport,
                title = "Report a Bug",
                subtitle = "Help us improve Pulse",
                onClick = { /* TODO: Open feedback form */ }
            )

            SettingsDivider()

            // ══════════════════════════════════════════════
            // ACCOUNT ACTIONS
            // ══════════════════════════════════════════════
            SettingsSectionHeader("Account")

            SettingsClickItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                subtitle = authManager.email ?: "",
                onClick = { showSignOutDialog = true },
                tintColor = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialogs ──

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Default Playback Speed") },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultSpeed == speed,
                                onClick = {
                                    scope.launch { settingsManager.saveDefaultSpeed(speed) }
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${speed}x", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }



    if (showThemeDialog) {
        val themes = listOf(
            "System" to ThemeMode.SYSTEM,
            "Light" to ThemeMode.LIGHT,
            "Dark" to ThemeMode.DARK
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    themes.forEach { (label, mode) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    themeViewModel.setTheme(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your local data will remain on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ══════════════════════════════════════════════
// REUSABLE SETTINGS COMPONENTS
// ══════════════════════════════════════════════

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tintColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (tintColor == MaterialTheme.colorScheme.error) tintColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
