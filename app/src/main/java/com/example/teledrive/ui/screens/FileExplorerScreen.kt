package com.example.teledrive.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.ui.theme.*
import com.example.teledrive.viewmodel.FileExplorerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    shouldCompress: Boolean,
    onOpenSettings: () -> Unit,
    onFileClick: (FileEntity) -> Unit
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsState()
    val fileCount by viewModel.fileCount.collectAsState()
    val folderCount by viewModel.folderCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<FileEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Any?>(null) }
    var showMoveDialog by remember { mutableStateOf<Any?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showFab by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileSize = try {
                context.contentResolver.openFileDescriptor(selectedUri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) { 0L }
            if (fileSize > 2L * 1024 * 1024 * 1024) {
                Toast.makeText(context, "File too large (max 2GB). Size: ${formatSize(fileSize)}", Toast.LENGTH_LONG).show()
                return@let
            }
            Toast.makeText(context, "Starting upload: ${formatSize(fileSize)}", Toast.LENGTH_SHORT).show()
            viewModel.uploadFile(context, selectedUri, shouldCompress)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareLink.collect { link ->
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Share Link", link))
            Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            if (searchQuery.isEmpty()) "Tele Drive" else "Search",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncFromTelegram() }, enabled = !isSyncing) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TeleBluePrimary)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = TeleBluePrimary)
                            }
                        }
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            FileExplorerViewModel.SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { viewModel.setSortOrder(order); showSortMenu = false },
                                    leadingIcon = {
                                        if (sortOrder == order) {
                                            Icon(Icons.Default.Check, null, tint = TeleBluePrimary)
                                        } else {
                                            Spacer(Modifier.size(24.dp))
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.ViewModule,
                                contentDescription = "Toggle View"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Upload/Download progress bar
                val allProgress = uploadProgress.values + downloadProgress.values
                if (allProgress.isNotEmpty()) {
                    val avg = allProgress.average().toFloat()
                    LinearProgressIndicator(
                        progress = { avg },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(horizontal = 16.dp)
                            .clip(CircleShape),
                        color = TeleBluePrimary,
                        trackColor = TeleBlueContainer.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Search bar
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onClear = { viewModel.updateSearchQuery("") }
                    )
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(visible = showFab, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.inverseSurface, tonalElevation = 4.dp) {
                                Text("New Folder", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium)
                            }
                            SmallFloatingActionButton(
                                onClick = { folderNameInput = ""; showCreateFolderDialog = true; showFab = false },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.inverseSurface, tonalElevation = 4.dp) {
                                Text("Upload File", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium)
                            }
                            SmallFloatingActionButton(
                                onClick = { filePickerLauncher.launch("*/*"); showFab = false },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) { Icon(Icons.Default.UploadFile, contentDescription = null) }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showFab = !showFab },
                    containerColor = TeleBluePrimary,
                    contentColor = Color.White
                ) {
                    Icon(if (showFab) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Breadcrumb row
            if (breadcrumb.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.navigateToFolder(null) },
                            label = { Text("Home") },
                            leadingIcon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp)) },
                            shape = CircleShape
                        )
                    }
                    items(breadcrumb) { folder ->
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        FilterChip(
                            selected = folder == breadcrumb.last(),
                            onClick = { viewModel.navigateToFolder(folder) },
                            label = { Text(folder.name) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TeleBlueContainer,
                                selectedLabelColor = TeleBlueDark
                            ),

                        )
                    }
                }
            }

            // Storage summary bar (only at root)
            if (breadcrumb.isEmpty() && searchQuery.isEmpty()) {
                StorageSummaryCard(totalStorageUsed = totalStorageUsed, fileCount = fileCount, folderCount = folderCount)
            }

            // Content
            if (isSyncing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TeleBluePrimary)
                        Spacer(Modifier.height(16.dp))
                        Text("Syncing with Telegram...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (folders.isEmpty() && files.isEmpty()) {
                EmptyState(isSearch = searchQuery.isNotEmpty(), onUpload = { filePickerLauncher.launch("*/*") })
            } else {
                val safeFolders = folders.distinctBy { it.id }
                val safeFiles = files.distinctBy { it.id }
                LazyVerticalGrid(
                    columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section header for folders
                    if (safeFolders.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = "Folders", count = safeFolders.size)
                        }
                        items(safeFolders, key = { "folder_${it.id}" }) { folder ->
                            FolderCard(
                                folder = folder,
                                isGrid = isGridView,
                                onClick = { viewModel.navigateToFolder(folder) },
                                onLongClick = { selectedFolder = folder }
                            )
                        }
                    }
                    // Section header for files
                    if (safeFiles.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = "Files", count = safeFiles.size)
                        }
                        items(safeFiles, key = { "file_${it.id}" }) { file ->
                            FileCard(
                                file = file,
                                isGrid = isGridView,
                                onClick = { onFileClick(file) },
                                onLongClick = { selectedFile = file }
                            )
                        }
                    }
                }
            }
        }
    }

    // Context menus
    if (selectedFolder != null) {
        ItemContextMenu(
            title = selectedFolder!!.name,
            icon = Icons.Default.Folder,
            iconTint = ColorFolder,
            onDismiss = { selectedFolder = null },
            onRename = { renameInput = selectedFolder!!.name; showRenameDialog = true },
            onDelete = { showDeleteDialog = selectedFolder; selectedFolder = null },
            onMove = { showMoveDialog = selectedFolder; selectedFolder = null },
            onCopy = null
        )
    }

    if (selectedFile != null) {
        ItemContextMenu(
            title = selectedFile!!.name,
            icon = getFileIcon(selectedFile!!.mimeType, selectedFile!!.extension),
            iconTint = getFileColor(selectedFile!!.mimeType, selectedFile!!.extension),
            onDismiss = { selectedFile = null },
            onRename = { renameInput = selectedFile!!.name; showRenameDialog = true },
            onDelete = { showDeleteDialog = selectedFile; selectedFile = null },
            onMove = { showMoveDialog = selectedFile; selectedFile = null },
            onCopy = { viewModel.copyFile(selectedFile!!, currentFolderId); selectedFile = null }
        )
    }

    // Dialogs
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = TeleBluePrimary) },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = { if (folderNameInput.isNotBlank()) { viewModel.createFolder(folderNameInput); showCreateFolderDialog = false } }) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false; selectedFile = null; selectedFolder = null },
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TeleBluePrimary) },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotBlank()) {
                        selectedFile?.let { viewModel.renameFile(it, renameInput) }
                        selectedFolder?.let { viewModel.renameFolder(it, renameInput) }
                        showRenameDialog = false; selectedFile = null; selectedFolder = null
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false; selectedFile = null; selectedFolder = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteDialog != null) {
        val name = when (val item = showDeleteDialog) { is FileEntity -> item.name; is Folder -> item.name; else -> "" }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete") },
            text = { Text("Delete \"$name\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (val item = showDeleteDialog) { is FileEntity -> viewModel.deleteFile(item); is Folder -> viewModel.deleteFolder(item) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showMoveDialog != null) {
        val allFolders by viewModel.allFolders.collectAsState()
        AlertDialog(
            onDismissRequest = { showMoveDialog = null },
            title = { Text("Move to...") },
            text = {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    FolderMoveItem(name = "Home (Root)", icon = Icons.Default.Home, onClick = {
                        when (val item = showMoveDialog) {
                            is FileEntity -> viewModel.moveFile(item, null)
                            is Folder -> viewModel.moveFolder(item, null)
                        }
                        showMoveDialog = null
                    })
                    allFolders.filter { folder ->
                        when (val item = showMoveDialog) {
                            is FileEntity -> folder.id != item.folderId
                            is Folder -> folder.id != item.id && folder.id != item.parentFolderId
                            else -> true
                        }
                    }.forEach { folder ->
                        FolderMoveItem(name = folder.name, icon = Icons.Default.Folder, onClick = {
                            when (val item = showMoveDialog) {
                                is FileEntity -> viewModel.moveFile(item, folder.id)
                                is Folder -> viewModel.moveFolder(item, folder.id)
                            }
                            showMoveDialog = null
                        })
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMoveDialog = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        "Search in Tele Drive",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(TeleBluePrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StorageSummaryCard(totalStorageUsed: Long, fileCount: Int, folderCount: Int) {
    val storageLimit = 50L * 1024 * 1024 * 1024
    val progress = (totalStorageUsed.toFloat() / storageLimit).coerceIn(0f, 1f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, TeleBluePrimary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TeleBluePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = TeleBluePrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Cloud Storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = TeleBluePrimary,
                    shape = CircleShape
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = TeleBluePrimary,
                trackColor = TeleBluePrimary.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${formatSize(totalStorageUsed)} used of 50 GB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    "$folderCount folders • $fileCount files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(folder: Folder, isGrid: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ColorFolder.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(36.dp), tint = ColorFolder)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    formatDate(folder.createdDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorFolder.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(28.dp), tint = ColorFolder)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatDate(folder.createdDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(file: FileEntity, isGrid: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val fileColor = getFileColor(file.mimeType, file.extension)
    val fileIcon = getFileIcon(file.mimeType, file.extension)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(fileColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(fileIcon, contentDescription = null, modifier = Modifier.size(36.dp), tint = fileColor)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    formatSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(fileColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(fileIcon, contentDescription = null, modifier = Modifier.size(28.dp), tint = fileColor)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatSize(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                        Text(
                            formatDate(file.uploadDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemContextMenu(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: (() -> Unit)?,
    onCopy: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 40.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
            ContextMenuItem(icon = Icons.Default.Edit, label = "Rename", onClick = { onRename(); onDismiss() })
            if (onMove != null) {
                ContextMenuItem(icon = Icons.Default.FolderOpen, label = "Move to Folder", onClick = { onMove(); onDismiss() })
            }
            if (onCopy != null) {
                ContextMenuItem(icon = Icons.Default.ContentCopy, label = "Make a Copy", onClick = { onCopy(); onDismiss() })
            }
            Spacer(Modifier.height(8.dp))
            ContextMenuItem(
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = { onDelete(); onDismiss() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContextMenuItem(icon: ImageVector, label: String, tint: Color = LocalContentColor.current, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = tint) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        modifier = Modifier.combinedClickable(onClick = onClick)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderMoveItem(name: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        leadingContent = { Icon(icon, contentDescription = null, tint = ColorFolder) },
        modifier = Modifier.combinedClickable(onClick = onClick)
    )
}

@Composable
fun EmptyState(isSearch: Boolean, onUpload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(TeleBluePrimary.copy(alpha = 0.05f))
                .border(2.dp, TeleBluePrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSearch) Icons.Default.SearchOff else Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = TeleBluePrimary
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            if (isSearch) "No results found" else "Your cloud is empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (isSearch) "We couldn't find what you're looking for" else "Upload your first file to start using Tele Drive",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!isSearch) {
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onUpload,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Upload First File", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// Helpers
fun getFileIcon(mimeType: String?, extension: String?): ImageVector = when {
    mimeType?.startsWith("image/") == true -> Icons.Default.Image
    mimeType?.startsWith("video/") == true -> Icons.Default.VideoLibrary
    mimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
    mimeType == "application/pdf" || extension?.lowercase() == "pdf" -> Icons.Default.PictureAsPdf
    extension?.lowercase() in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.FolderZip
    extension?.lowercase() in listOf("doc", "docx", "txt") -> Icons.Default.Description
    extension?.lowercase() in listOf("xls", "xlsx", "csv") -> Icons.Default.TableChart
    extension?.lowercase() in listOf("ppt", "pptx") -> Icons.Default.Slideshow
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

fun getFileColor(mimeType: String?, extension: String?): Color = when {
    mimeType?.startsWith("image/") == true -> ColorImage
    mimeType?.startsWith("video/") == true -> ColorVideo
    mimeType?.startsWith("audio/") == true -> ColorAudio
    mimeType == "application/pdf" || extension?.lowercase() == "pdf" -> ColorPDF
    extension?.lowercase() in listOf("zip", "rar", "7z", "tar", "gz") -> ColorArchive
    else -> ColorDoc
}

fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val exp = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(3)
    return "%.1f %s".format(size / Math.pow(1024.0, exp.toDouble()), units[exp])
}
