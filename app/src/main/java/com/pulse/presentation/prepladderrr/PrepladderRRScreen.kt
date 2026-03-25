package com.pulse.presentation.prepladderrr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.presentation.components.LectureCard
import com.pulse.presentation.components.DrivePdfPicker
import com.pulse.presentation.customlist.CustomListViewModel
import com.pulse.presentation.library.LectureActionBottomSheet
import com.pulse.core.data.db.Lecture
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepladderRRScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLecture: (String, String?) -> Unit,
    viewModel: PrepladderRRViewModel = koinViewModel(),
    customListViewModel: CustomListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val customLists by customListViewModel.customLists.collectAsState()
    var selectedLongPressLecture by remember { mutableStateOf<Lecture?>(null) }
    var showDrivePdfPicker by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Handle back press when inside a subfolder
    if (uiState.currentFolder != null) {
        androidx.activity.compose.BackHandler {
            viewModel.goBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentFolder?.name ?: "PREPLADDER RR",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentFolder != null) {
                            viewModel.goBack()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isLoadingFolders || uiState.isLoadingVideos) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.currentFolder == null) {
                        IconButton(onClick = { viewModel.loadSubfolders() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.currentFolder != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        viewModel.loadDrivePdfs(uiState.currentFolder!!.id)
                        showDrivePdfPicker = true 
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("PDF from Drive")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.error != null -> {
                    PrepladderRRErrorState(
                        error = uiState.error ?: "",
                        onRetryClick = {
                            if (uiState.currentFolder != null) {
                                viewModel.openFolder(uiState.currentFolder!!)
                            } else {
                                viewModel.loadSubfolders()
                            }
                        }
                    )
                }

                uiState.currentFolder != null -> {
                    PrepladderRRVideoList(
                        lectures = lectures,
                        isLoadingVideos = uiState.isLoadingVideos,
                        searchQuery = searchQuery,
                        currentFolder = uiState.currentFolder,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onNavigateToLecture = onNavigateToLecture,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDelete = { viewModel.deleteLecture(it) },
                        onLongPress = { selectedLongPressLecture = it }
                    )
                }

                uiState.subfolders.isEmpty() && !uiState.isLoadingFolders -> {
                    PrepladderRREmptyFolderState(
                        onRefreshClick = { viewModel.loadSubfolders() }
                    )
                }

                else -> {
                    PrepladderRRFolderList(
                        subfolders = uiState.subfolders,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onFolderClick = { folder ->
                            viewModel.updateSearchQuery("")
                            viewModel.openFolder(folder)
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
            onDownloadClick = { viewModel.startDownload(lecture) },
            onAddToListClick = { targetListId ->
                customListViewModel.addLectureToList(targetListId, lecture.id)
            },
            onCreateNewListClick = { listName ->
                customListViewModel.createList(listName) { newListId ->
                    customListViewModel.addLectureToList(newListId, lecture.id)
                }
            },
            onToggleFavoriteClick = { viewModel.toggleFavorite(lecture.id) },
            onMarkAsCompletedClick = { viewModel.markAsCompleted(lecture.id) }
        )
    }

    if (showDrivePdfPicker) {
        DrivePdfPicker(
            pdfs = uiState.drivePdfs,
            onPdfSelected = { pdf ->
                showDrivePdfPicker = false
                viewModel.addDrivePdf(pdf)
            },
            onDismissRequest = { showDrivePdfPicker = false },
            isLoading = uiState.isLoadingVideos,
            folderPdf = uiState.folderPdf
        )
    }
}
