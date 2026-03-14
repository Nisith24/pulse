package com.pulse.presentation.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
    var showClearAllDialog by remember { mutableStateOf(false) }

    val allLectures = remember(downloaded, cloudOnly) {
        (downloaded + cloudOnly).distinctBy { it.id }
    }
    
    val groupedLectures = remember(allLectures) {
        allLectures.groupBy { 
            if (it.isLocal) "Internal Storage" 
            else it.subject?.takeIf { s -> s.isNotBlank() } ?: "Cloud Service"
        }
    }

    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    
    // Auto-expand if there's only one group, or if search is active
    LaunchedEffect(groupedLectures.keys, isSearching) {
        if (isSearching || groupedLectures.keys.size == 1) {
            expandedSections = groupedLectures.keys
        }
    }

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

            if (allLectures.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(16.dp))
                            Text("No videos available", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Sync your library to see cloud videos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                groupedLectures.forEach { (source, sourceLectures) ->
                    val isExpanded = expandedSections.contains(source)
                    val activeSourceItems = sourceLectures.filter { activeDownloads.containsKey(it.id) }
                    val downloadedSourceItems = sourceLectures.filter { it.videoLocalPath?.isNotEmpty() == true }
                    val availableSourceItems = sourceLectures.filter { it.videoLocalPath.isNullOrEmpty() && !activeDownloads.containsKey(it.id) }

                    item(key = "header-$source") {
                        SourceHeader(
                            title = source,
                            count = sourceLectures.size,
                            downloadedCount = downloadedSourceItems.size,
                            isExpanded = isExpanded,
                            onClick = {
                                expandedSections = if (isExpanded) expandedSections - source else expandedSections + source
                            }
                        )
                    }

                    if (isExpanded) {
                        if (activeSourceItems.isNotEmpty()) {
                            item(key = "sub-active-$source") { SubSectionHeader("Active Downloads", Icons.Default.Downloading) }
                            items(activeSourceItems, key = { "active-${it.id}" }) { lecture ->
                                val progress = activeDownloads[lecture.id]
                                ActiveDownloadItem(
                                    lecture = lecture,
                                    progress = progress,
                                    onRetry = { viewModel.startDownload(lecture) },
                                    onCancel = { viewModel.cancelDownload(lecture.id) }
                                )
                            }
                        }

                        if (downloadedSourceItems.isNotEmpty()) {
                            item(key = "sub-dl-$source") { SubSectionHeader("Downloaded", Icons.Default.DownloadDone) }
                            items(downloadedSourceItems, key = { "dl-${it.id}" }) { lecture ->
                                DownloadedItem(
                                    lecture = lecture,
                                    onPlay = { onNavigateToLecture(lecture.id) },
                                    onDelete = { viewModel.deleteOffline(lecture.id) },
                                    onSaveToDevice = { viewModel.saveToDevice(lecture) }
                                )
                            }
                        }

                        if (availableSourceItems.isNotEmpty()) {
                            item(key = "sub-avail-$source") { SubSectionHeader("Available for Download", Icons.Default.CloudDownload) }
                            items(availableSourceItems, key = { "cloud-${it.id}" }) { lecture ->
                                AvailableItem(
                                    lecture = lecture,
                                    onDownload = { viewModel.startDownload(lecture) }
                                )
                            }
                        }
                    }
                }

                if (downloaded.isNotEmpty()) {
                    item {
                        Surface(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
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
private fun SourceHeader(
    title: String, 
    count: Int, 
    downloadedCount: Int, 
    isExpanded: Boolean, 
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "ExpandIconRotation")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isInternal = title == "Internal Storage"
            Icon(
                imageVector = if (isInternal) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                contentDescription = null, 
                tint = if (isInternal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$downloadedCount downloaded • $count total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(start = 32.dp, end = 20.dp, top = 12.dp, bottom = 8.dp), 
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Text(text = title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f), letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ActiveDownloadItem(lecture: Lecture, progress: DownloadStatus?, onRetry: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 4.dp).animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))) {
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

    Surface(onClick = onPlay, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
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
    Card(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
