package com.pulse.presentation.library

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.presentation.components.LectureCard
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = koinViewModel(),
    onLectureSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSubjects: () -> Unit = {}
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val btrLectures by viewModel.btrLectures.collectAsState()
    val localLectures by viewModel.localLectures.collectAsState()
    val error by viewModel.error.collectAsState()
    val authIntent by viewModel.authIntent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isBtrViewActive by viewModel.isBtrViewActive.collectAsState()
    val isOnline by viewModel.connectivityStatus.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val recentLecture by viewModel.recentLecture.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (isBtrViewActive && currentTab == LibraryTab.SERVICES) {
        androidx.activity.compose.BackHandler {
            viewModel.setBtrViewActive(false)
        }
    }

    val currentLectures = if (currentTab == LibraryTab.HOME) localLectures else btrLectures

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.syncBtr()
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val name = getFileName(context, it) ?: "Local Video"
            viewModel.addLocalLecture(name, it.toString(), null)
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val name = getFileName(context, it) ?: "Local PDF"
            viewModel.addLocalLecture(name, null, it.toString())
        }
    }

    val manageFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                videoPickerLauncher.launch(arrayOf("video/*"))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            videoPickerLauncher.launch(arrayOf("video/*"))
        }
    }

    fun requestLocalAccess(isPdf: Boolean = false) {
        val launcher = if (isPdf) pdfPickerLauncher else videoPickerLauncher
        val mimeTypes = if (isPdf) arrayOf("application/pdf") else arrayOf("video/*")

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    launcher.launch(mimeTypes)
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        manageFilesLauncher.launch(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageFilesLauncher.launch(intent)
                    }
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                permissionLauncher.launch(if (isPdf) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_MEDIA_VIDEO)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
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
                            placeholder = { Text("Search lectures...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    isSearching = false
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        val title = if (isBtrViewActive && currentTab == LibraryTab.SERVICES) "BTR" else "PULSE"
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    if (isBtrViewActive && currentTab == LibraryTab.SERVICES) {
                        IconButton(onClick = { viewModel.setBtrViewActive(false) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Services")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { viewModel.toggleFavoritesFilter() }) {
                            Icon(
                                if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Show Favorites",
                                tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (currentTab == LibraryTab.SERVICES && isBtrViewActive) {
                        IconButton(onClick = { viewModel.syncBtr() }, enabled = !isLoading) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                    Box {
                        val authManager: IBtrAuthManager = koinInject()
                        IconButton(onClick = { showMenu = true }) {
                            if (authManager.isSignedIn) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                        )
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (authManager.displayName?.firstOrNull() ?: 'P').toString().uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (authManager.isSignedIn) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(text = authManager.displayName ?: "User", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = authManager.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = { showMenu = false; onNavigateToSettings() }
                            )
                            DropdownMenuItem(
                                text = { Text("Downloads") },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = { showMenu = false; onNavigateToDownloads() }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Video Cache") },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = { viewModel.clearCache(); showMenu = false }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var showFabMenu by remember { mutableStateOf(false) }

                if (currentTab == LibraryTab.HOME && showFabMenu) {
                    FloatingActionButton(
                        onClick = { showFabMenu = false; requestLocalAccess(isPdf = true) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) { Icon(Icons.Default.PictureAsPdf, contentDescription = "Add PDF") }
                    
                    FloatingActionButton(
                        onClick = { showFabMenu = false; requestLocalAccess(isPdf = false) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) { Icon(Icons.Default.VideoLibrary, contentDescription = "Add Video") }
                }

                // Resume Playback FAB with inline Tooltip
                recentLecture?.let { lecture ->
                    var showTooltip by remember { mutableStateOf(true) }

                    LaunchedEffect(lecture.id) {
                        showTooltip = true
                        kotlinx.coroutines.delay(4000)
                        showTooltip = false
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showTooltip) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface
                            ) {
                                Text(
                                    "Resume: ${lecture.name.take(20)}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { viewModel.syncAndResume(onLectureSelected) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume ${lecture.name}")
                        }
                    }
                }

                if (currentTab == LibraryTab.HOME) {
                    LargeFloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = "Expand menu", modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(if (tab == LibraryTab.HOME) "Home" else "Services", style = MaterialTheme.typography.titleSmall) }
                    )
                }
            }

            if (currentTab == LibraryTab.SERVICES && !isOnline) {
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Offline Mode • Cloud content may be unavailable", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = error ?: "", style = MaterialTheme.typography.bodyMedium)
                            authIntent?.let { intent ->
                                TextButton(onClick = { authLauncher.launch(intent) }) { Text("Grant Permission") }
                            }
                        }
                        TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                    }
                }
            }

            if (currentTab == LibraryTab.SERVICES && !isBtrViewActive) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "CORE SERVICES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.padding(start = 8.dp, bottom = 4.dp), fontWeight = FontWeight.Bold)
                    Card(modifier = Modifier.fillMaxWidth().height(110.dp).clickable { viewModel.setBtrViewActive(true) }, shape = MaterialTheme.shapes.extraLarge, elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Surface(modifier = Modifier.size(64.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary, shadowElevation = 12.dp) {
                                Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Stream, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
                            }
                            Text("BTR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Card(modifier = Modifier.fillMaxWidth().height(110.dp).clickable { onNavigateToSubjects() }, shape = MaterialTheme.shapes.extraLarge, elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Surface(modifier = Modifier.size(64.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondary, shadowElevation = 12.dp) {
                                Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(32.dp)) }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SUBJECTS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("19 Subjects NEET PG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.7f), fontWeight = FontWeight.Bold)
                            }
                            Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Card(modifier = Modifier.fillMaxWidth().height(100.dp).alpha(0.6f), shape = MaterialTheme.shapes.extraLarge, border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MARROW RR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Coming soon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) { Text("DISABLED", modifier = Modifier.padding(4.dp)) }
                        }
                    }
                }
            } else if (currentLectures.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = if (currentTab == LibraryTab.HOME) Icons.Default.Home else Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text(text = if (currentTab == LibraryTab.HOME) "No local videos" else "Cloud Library empty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(text = if (currentTab == LibraryTab.HOME) "Tap + to add videos from your device" else "Tap refresh to sync from Cloud", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                val groupedLectures = remember(currentLectures, currentTab) {
                    if (currentTab != LibraryTab.HOME) {
                        val digitRegex = Regex("\\d+")
                        currentLectures.groupBy { lecture ->
                            digitRegex.find(lecture.name)?.value ?: "Misc"
                        }
                    } else {
                        emptyMap()
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(240.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentTab == LibraryTab.HOME) {
                        items(currentLectures, key = { it.id }) { lecture ->
                            LectureCard(
                                lecture = lecture, 
                                isLibraryHome = currentTab == LibraryTab.HOME, 
                                onLectureSelected = onLectureSelected, 
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onDelete = { viewModel.deleteLecture(it) }
                            )
                        }
                    } else {
                        groupedLectures.forEach { (moduleNumber, lectures) ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = if (moduleNumber == "Misc") "Other Files" else "Module $moduleNumber",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
                                )
                            }
                            items(lectures, key = { it.id }) { lecture ->
                                LectureCard(
                                    lecture = lecture, 
                                    isLibraryHome = currentTab == LibraryTab.HOME, 
                                    onLectureSelected = onLectureSelected, 
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteLecture(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}
