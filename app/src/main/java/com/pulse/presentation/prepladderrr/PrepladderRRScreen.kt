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
                // Error state
                uiState.error != null -> {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                uiState.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = {
                                if (uiState.currentFolder != null) {
                                    viewModel.openFolder(uiState.currentFolder!!)
                                } else {
                                    viewModel.loadSubfolders()
                                }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                // Inside a subfolder — show videos
                uiState.currentFolder != null -> {
                    if (lectures.isEmpty() && !uiState.isLoadingVideos && searchQuery.isBlank()) {
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
                                onValueChange = viewModel::updateSearchQuery,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = { Text("Search files...") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            
                            if (lectures.isEmpty() && searchQuery.isNotBlank()) {
                                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                     Text("No files match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                onNavigateToLecture(id, uiState.currentFolder?.id)
                                            },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onDelete = { viewModel.deleteLecture(it) },
                                            onLongPress = { selectedLongPressLecture = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Root — show subfolders list
                uiState.subfolders.isEmpty() && !uiState.isLoadingFolders -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No folders found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.loadSubfolders() }) {
                                Text("Refresh")
                            }
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search folders...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        val filteredFolders = if (searchQuery.isBlank()) {
                            uiState.subfolders
                        } else {
                            uiState.subfolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
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
                                                viewModel.updateSearchQuery("") // Clear text on folder open
                                                viewModel.openFolder(folder) 
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
