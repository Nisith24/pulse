package com.pulse.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.CustomList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureActionBottomSheet(
    lecture: Lecture,
    customLists: List<CustomList>,
    onDismissRequest: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToListClick: (Long) -> Unit,
    onCreateNewListClick: (String) -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onMarkAsCompletedClick: () -> Unit,
    onRemoveFromListClick: (() -> Unit)? = null,
    removeFromListLabel: String = "Remove from Playlist"
) {
    var showListSelection by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = lecture.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            HorizontalDivider()

            if (!showListSelection) {
                // Main Options
                if (!lecture.isLocal && !lecture.isPdfDownloaded && lecture.videoLocalPath.isNullOrEmpty()) {
                    ListItem(
                        headlineContent = { Text("Download Video") },
                        leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onDownloadClick()
                            onDismissRequest()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(if (lecture.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                    leadingContent = { Icon(Icons.Default.Favorite, contentDescription = null, tint = if (lecture.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current) },
                    modifier = Modifier.clickable {
                        onToggleFavoriteClick()
                        onDismissRequest()
                    }
                )

                val isCompleted = lecture.videoDuration > 0 && lecture.lastPosition >= lecture.videoDuration * 0.9
                if (!isCompleted) {
                    ListItem(
                        headlineContent = { Text("Mark as Completed") },
                        leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onMarkAsCompletedClick()
                            onDismissRequest()
                        }
                    )
                }

                if (onRemoveFromListClick != null) {
                    ListItem(
                        headlineContent = { Text(removeFromListLabel, color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            onRemoveFromListClick()
                            onDismissRequest()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("Add to Custom Playlist") },
                    leadingContent = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showListSelection = true
                    }
                )
            } else {
                // List Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Playlist",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showListSelection = false }) {
                        Text("Back")
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text("Create New Playlist", color = MaterialTheme.colorScheme.primary) },
                            leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                showCreateListDialog = true
                            }
                        )
                    }

                    items(customLists) { list ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            modifier = Modifier.clickable {
                                onAddToListClick(list.id)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            onCreateNewListClick(newListName.trim())
                            showCreateListDialog = false
                            onDismissRequest()
                        }
                    }
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
