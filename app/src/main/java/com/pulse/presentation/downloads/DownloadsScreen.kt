package com.pulse.presentation.downloads

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.DownloadStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLecture: (String) -> Unit = {},
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val downloaded by viewModel.downloadedLectures.collectAsState()
    val cloudOnly by viewModel.cloudOnlyLectures.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search downloads...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Column {
                            Text("Downloads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Video in HOME page | Download only needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (isSearching) { { isSearching = false ; viewModel.setSearchQuery("") } } else onNavigateBack) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Storage Info Header (Minimal)
            item {
                val totalDownloaded = downloaded.size
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Library Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("$totalDownloaded files cached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // ═══════════════════════════════════
            // ACTIVE DOWNLOADS
            // ═══════════════════════════════════
            val activeItems = (cloudOnly + downloaded).filter { activeDownloads.containsKey(it.id) }.distinctBy { it.id }
            if (activeItems.isNotEmpty()) {
                item { SectionHeader("Active Downloads", Icons.Default.Downloading) }
                items(activeItems, key = { "active-${it.id}" }) { lecture ->
                    val progress = activeDownloads[lecture.id]
                    ActiveDownloadItem(
                        lecture = lecture,
                        progress = progress,
                        onRetry = { viewModel.startDownload(lecture) },
                        onCancel = { viewModel.cancelDownload(lecture.id) }
                    )
                }
            }

            // ═══════════════════════════════════
            // DOWNLOADED (OFFLINE)
            // ═══════════════════════════════════
            if (downloaded.isNotEmpty()) {
                item { SectionHeader("Downloaded", Icons.Default.DownloadDone) }
                items(downloaded, key = { "dl-${it.id}" }) { lecture ->
                    DownloadedItem(
                        lecture = lecture,
                        onPlay = { onNavigateToLecture(lecture.id) },
                        onDelete = { viewModel.deleteOffline(lecture.id) },
                        onSaveToDevice = { viewModel.saveToDevice(lecture) }
                    )
                }
                item {
                    // Clear All button
                    Surface(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear All Downloads", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ═══════════════════════════════════
            // AVAILABLE FOR DOWNLOAD
            // ═══════════════════════════════════
            val availableItems = cloudOnly.filter { !activeDownloads.containsKey(it.id) }
            if (availableItems.isNotEmpty()) {
                item { SectionHeader("Available for Download", Icons.Default.CloudDownload) }
                items(availableItems, key = { "cloud-${it.id}" }) { lecture ->
                    AvailableItem(
                        lecture = lecture,
                        onDownload = { viewModel.startDownload(lecture) }
                    )
                }
            }

            // Empty state
            if (downloaded.isEmpty() && cloudOnly.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(16.dp))
                            Text("No videos available", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Sync your BTR library to see cloud videos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Downloads") },
            text = { Text("This will remove all offline videos from the app. Videos will still be available in the cloud for re-download.") },
            confirmButton = {
                Button(onClick = { showClearAllDialog = false; viewModel.clearAllDownloads() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), letterSpacing = 1.sp)
    }
}

@Composable
private fun ActiveDownloadItem(lecture: Lecture, progress: DownloadStatus?, onRetry: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = lecture.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when (progress) {
                    is DownloadStatus.Downloading -> {
                        val progressPercent = (progress.progress * 100).toInt()
                        Text(text = if (progress.speed.isNotEmpty()) "Downloading ($progressPercent%) • ${progress.speed}" else "Starting download...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { progress.progress }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = MaterialTheme.colorScheme.primaryContainer, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadStatus.Done -> Text("Complete ✓", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    is DownloadStatus.Error -> Text("Failed: ${progress.message}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    else -> {}
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadedItem(lecture: Lecture, onPlay: () -> Unit, onDelete: () -> Unit, onSaveToDevice: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var bookmarkSuccess by remember { mutableStateOf(false) }

    Surface(onClick = onPlay, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = lecture.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(text = "Tap to play offline", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Row {
                IconButton(onClick = { 
                    onSaveToDevice()
                    bookmarkSuccess = true
                }, modifier = Modifier.size(36.dp)) { 
                    Icon(if (bookmarkSuccess) Icons.Default.BookmarkAdded else Icons.Default.Save, contentDescription = "Save to Device", tint = if (bookmarkSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) 
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("Delete Offline Copy?") }, text = { Text("\"${lecture.name}\" will be removed from offline storage.") }, confirmButton = { Button(onClick = { showDeleteConfirm = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } }, dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } })
    }
}

@Composable
private fun AvailableItem(lecture: Lecture, onDownload: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = lecture.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = "Cloud • Tap to download", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            FilledTonalIconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(20.dp)) }
        }
    }
}
