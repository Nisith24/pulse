package com.pulse.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.pulse.core.data.db.Lecture

@Composable
fun LectureCard(
    lecture: Lecture,
    isLibraryHome: Boolean,
    onLectureSelected: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (Lecture) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Confirm Deletion") },
            text = { Text(text = "Are you sure you want to delete this lecture? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(lecture)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onLectureSelected(lecture.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isLibraryHome) {
                    val isBtrDownloaded = !lecture.isLocal && (lecture.videoLocalPath?.isNotEmpty() == true)
                    if (isBtrDownloaded) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = CircleShape, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Downloaded BTR", modifier = Modifier.padding(6.dp).size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    } else if (lecture.isLocal) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f), shape = CircleShape, modifier = Modifier.padding(end = 4.dp)) {
                            Icon(if (lecture.videoLocalPath?.isNotEmpty() == true) Icons.Default.VideoLibrary else Icons.Default.Description, contentDescription = "Local File", modifier = Modifier.padding(6.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    IconButton(onClick = { onToggleFavorite(lecture.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = if (lecture.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (lecture.isFavorite) "Remove from favorites" else "Add to favorites", modifier = Modifier.size(20.dp), tint = if (lecture.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete lecture", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                } else {
                    IconButton(onClick = { onToggleFavorite(lecture.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = if (lecture.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (lecture.isFavorite) "Remove from favorites" else "Add to favorites", modifier = Modifier.size(20.dp), tint = if (lecture.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val hasVideo = lecture.videoId != null || (lecture.videoLocalPath?.isNotEmpty() == true)
                    if (hasVideo) Icon(Icons.Default.PlayCircle, contentDescription = "Video", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    val isPdfAvailable = if (lecture.isLocal) lecture.pdfLocalPath.isNotEmpty() else (lecture.isPdfDownloaded || lecture.pdfId != null)
                    if (isPdfAvailable) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(20.dp), tint = if (lecture.isPdfDownloaded || lecture.isLocal) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        if (!hasVideo) Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) { Text("STANDALONE PDF", modifier = Modifier.padding(2.dp), style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(text = lecture.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = if (isLibraryHome) 28.dp else 0.dp))
                Spacer(Modifier.height(8.dp))
                if (lecture.lastPosition > 0) {
                    Spacer(Modifier.height(8.dp))
                    val progress = if (lecture.videoDuration > 0) lecture.lastPosition.toFloat() / lecture.videoDuration.toFloat() else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${((progress * 100).toInt()).coerceIn(0, 100)}% viewed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            val minutes = (lecture.lastPosition / 1000 / 60)
                            val seconds = (lecture.lastPosition / 1000 % 60)
                            Text(text = "At ${minutes}:${seconds.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
