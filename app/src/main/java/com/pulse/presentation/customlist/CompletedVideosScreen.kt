package com.pulse.presentation.customlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.presentation.components.LectureCard
import com.pulse.core.data.db.Lecture
import com.pulse.presentation.library.LectureActionBottomSheet
import com.pulse.core.data.db.CustomList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedVideosScreen(
    viewModel: CustomListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLecture: (String) -> Unit
) {
    val lectures by viewModel.completedLectures.collectAsState()
    val customLists by viewModel.customLists.collectAsState()
    var selectedLongPressLecture by remember { mutableStateOf<Lecture?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Completed Videos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (lectures.isEmpty()) {
                item {
                    Text(
                        "No completed videos yet. Keep watching!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(lectures, key = { it.id }) { lecture ->
                    LectureCard(
                        lecture = lecture,
                        onLectureSelected = { onNavigateToLecture(lecture.id) },
                        onToggleFavorite = { /* viewModel.toggleFavorite(it) */ },
                        onLongPress = {
                            selectedLongPressLecture = it
                        }
                    )
                }
            }
        }
    }

    selectedLongPressLecture?.let { lecture ->
        LectureActionBottomSheet(
            lecture = lecture,
            customLists = customLists,
            onDismissRequest = { selectedLongPressLecture = null },
            onDownloadClick = { /* repository directly or add to VM */ },
            onAddToListClick = { targetListId ->
                viewModel.addLectureToList(targetListId, lecture.id)
            },
            onCreateNewListClick = { listName ->
                viewModel.createList(listName) { newListId ->
                    viewModel.addLectureToList(newListId, lecture.id)
                }
            },
            onToggleFavoriteClick = { /*viewModel.toggleFavorite(lecture.id)*/ },
            onMarkAsCompletedClick = { viewModel.markAsCompleted(lecture.id) },
            onRemoveFromListClick = { viewModel.resetProgress(lecture.id) },
            removeFromListLabel = "Reset Progress (Remove from Completed)"
        )
    }
}
