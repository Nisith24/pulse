package com.pulse.presentation.prepladderrr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.core.data.db.Lecture
import com.pulse.presentation.components.LectureCard

@Composable
fun PrepladderRRErrorState(
    error: String,
    onRetryClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun PrepladderRRVideoList(
    lectures: List<Lecture>,
    isLoadingVideos: Boolean,
    searchQuery: String,
    currentFolder: DriveFolder?,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToLecture: (String, String?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (Lecture) -> Unit,
    onLongPress: (Lecture) -> Unit
) {
    if (lectures.isEmpty() && !isLoadingVideos && searchQuery.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No videos in this folder yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search files...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (lectures.isEmpty() && searchQuery.isNotBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No files match your search.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(240.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lectures, key = { it.id }) { lecture ->
                        LectureCard(
                            lecture = lecture,
                            isLibraryHome = false,
                            onLectureSelected = { id ->
                                onNavigateToLecture(id, currentFolder?.id)
                            },
                            onToggleFavorite = { onToggleFavorite(it) },
                            onDelete = { onDelete(it) },
                            onLongPress = { onLongPress(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrepladderRREmptyFolderState(
    onRefreshClick: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No folders found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRefreshClick) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun PrepladderRRFolderList(
    subfolders: List<DriveFolder>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFolderClick: (DriveFolder) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search folders...") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        val filteredFolders = if (searchQuery.isBlank()) {
            subfolders
        } else {
            subfolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        if (filteredFolders.isEmpty() && searchQuery.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No folders match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFolders, key = { it.id }) { folder ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFolderClick(folder)
                            },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    folder.name,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            leadingContent = {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}
