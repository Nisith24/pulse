package com.pulse.presentation.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.core.data.db.Lecture
import com.pulse.core.domain.util.Constants
import com.pulse.presentation.components.LectureCard
import com.pulse.presentation.components.DrivePdfPicker
import com.pulse.presentation.customlist.CustomListViewModel
import com.pulse.presentation.library.LectureActionBottomSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToLecture: (String, String?) -> Unit = { _, _ -> },
    viewModel: SubjectDetailViewModel = koinViewModel(),
    customListViewModel: CustomListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lectures by viewModel.lectures.collectAsState()
    val customLists by customListViewModel.customLists.collectAsState()
    var selectedLongPressLecture by remember { mutableStateOf<Lecture?>(null) }
    var showingFolder by remember { mutableStateOf(false) }
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var showDrivePdfPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subjectName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                subjectName == "Microbiology" && !showingFolder -> {
                    FolderSection(onFolderClick = {
                        showingFolder = true
                        currentFolderId = Constants.PREPLADDER_FOLDER_ID
                        viewModel.loadFolder(Constants.PREPLADDER_FOLDER_ID, subjectName)
                    })
                }
                showingFolder -> {
                    FolderContent(
                        uiState = uiState,
                        lectures = lectures,
                        onBack = { 
                            showingFolder = false
                            currentFolderId = null
                        },
                        currentFolderId = currentFolderId,
                        onLectureSelected = onNavigateToLecture,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDelete = { viewModel.deleteLecture(it) },
                        onLongPress = { selectedLongPressLecture = it }
                    )
                }
                else -> {
                    EmptySubjectPlaceholder(subjectName)
                }
            }

            if (showingFolder && currentFolderId != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        viewModel.loadDrivePdfs(currentFolderId!!)
                        showDrivePdfPicker = true 
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("PDF from Drive")
                }
            }
        }
    }

    if (showDrivePdfPicker) {
        DrivePdfPicker(
            pdfs = uiState.drivePdfs,
            onPdfSelected = { pdf ->
                showDrivePdfPicker = false
                viewModel.addDrivePdf(pdf, subjectName)
            },
            onDismissRequest = { showDrivePdfPicker = false },
            isLoading = uiState.isLoading,
            folderPdf = uiState.folderPdf
        )
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
}

@Composable
private fun FolderSection(onFolderClick: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFolderClick),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            ListItem(
                headlineContent = { Text("PREPLADDER - Dr.Preeti Sharma", fontWeight = FontWeight.SemiBold) },
                leadingContent = {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@Composable
private fun FolderContent(
    uiState: SubjectDetailUiState,
    lectures: List<Lecture>,
    onBack: () -> Unit,
    currentFolderId: String?,
    onLectureSelected: (String, String?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (Lecture) -> Unit,
    onLongPress: (Lecture) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Folders")
            }
            Text("PREPLADDER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.weight(1f))
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        when {
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
            lectures.isEmpty() && !uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Folder is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
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
                            onLectureSelected = { id -> onLectureSelected(id, currentFolderId) },
                            onToggleFavorite = onToggleFavorite,
                            onDelete = onDelete,
                            onLongPress = onLongPress
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySubjectPlaceholder(subjectName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Videos for $subjectName will be uploaded in the future.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
