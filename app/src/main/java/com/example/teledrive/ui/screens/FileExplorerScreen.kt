package com.example.teledrive.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.ui.viewmodel.FileExplorerViewModel
import kotlinx.coroutines.flow.collect
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
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val breadcrumb by viewModel.breadcrumb.collectAsState()

    androidx.activity.compose.BackHandler(enabled = currentFolderId != null) {
        val lastFolder = if (breadcrumb.size > 1) breadcrumb[breadcrumb.size - 2] else null
        viewModel.navigateToFolder(lastFolder)
    }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsState()
    val fileCount by viewModel.fileCount.collectAsState()
    val folderCount by viewModel.folderCount.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val selectedFolders by viewModel.selectedFolders.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var selectedFileForMenu by remember { mutableStateOf<FileEntity?>(null) }
    var selectedFolderForMenu by remember { mutableStateOf<Folder?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Any?>(null) }
    var showMoveDialog by remember { mutableStateOf<FileEntity?>(null) }
    var showTransferCenter by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(context, it, shouldCompress)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedFiles.size + selectedFolders.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.downloadSelected() }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Tele Drive") },
                    actions = {
                        IconButton(onClick = { viewModel.syncFromTelegram() }, enabled = !isSyncing) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = if (isSyncing) Modifier.size(24.dp) else Modifier)
                        }
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            FileExplorerViewModel.SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == order) Icon(Icons.Default.Check, null)
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.ViewModule,
                                contentDescription = "Toggle View"
                            )
                        }
                    IconButton(onClick = { showTransferCenter = true }) {
                        val activeCount = (downloadProgress.size + uploadProgress.size)
                        BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
                            Icon(Icons.Default.SyncAlt, contentDescription = "Transfers")
                        }
                    }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                if (downloadProgress.isNotEmpty() || uploadProgress.isNotEmpty()) {
                    val totalProgress = (downloadProgress.values + uploadProgress.values).average().toFloat()
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { totalProgress }
                    )
                }
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Storage: ${formatSize(totalStorageUsed)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$folderCount Folders, $fileCount Files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // Progress bar for storage (assuming 50GB limit as per memory)
                        val storageLimit = 50L * 1024 * 1024 * 1024
                        LinearProgressIndicator(
                            progress = { (totalStorageUsed.toFloat() / storageLimit).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search files") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Breadcrumb
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    TextButton(onClick = { viewModel.navigateToFolder(null) }) {
                        Text("Home")
                    }
                }
                items(
                    items = breadcrumb,
                    key = { it.id }
                ) { folder ->
                    Text(" > ", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { viewModel.navigateToFolder(folder) }) {
                        Text(folder.name)
                    }
                }
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload")
                }
                Button(
                    onClick = {
                        folderNameInput = ""
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Folder")
                }
            }

            if (isSyncing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Syncing with Telegram...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (folders.isEmpty() && files.isEmpty()) {
                EmptyFolderState(
                    isSearch = searchQuery.isNotEmpty(),
                    onUploadClick = { filePickerLauncher.launch("*/*") }
                )
            } else {
                LazyVerticalGrid(
                    columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(folders) { folder ->
                        FolderItem(
                            folder = folder,
                            isSelected = selectedFolders.contains(folder.id),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleFolderSelection(folder.id)
                                } else {
                                    viewModel.navigateToFolder(folder)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleFolderSelection(folder.id)
                            }
                        )
                    }
                    items(files) { file ->
                        FileItem(
                            file = file,
                            isSelected = selectedFiles.contains(file.id),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleFileSelection(file.id)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleFileSelection(file.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (selectedFolderForMenu != null) {
        ModalBottomSheet(onDismissRequest = { selectedFolderForMenu = null }) {
            ListItem(
                headlineContent = { Text("Rename") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    renameInput = selectedFolderForMenu?.name ?: ""
                    showRenameDialog = true
                })
            )
            ListItem(
                headlineContent = { Text("Delete") },
                leadingContent = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    showDeleteConfirmation = selectedFolderForMenu
                    selectedFolderForMenu = null
                })
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (selectedFileForMenu != null) {
        ModalBottomSheet(onDismissRequest = { selectedFileForMenu = null }) {
            ListItem(
                headlineContent = { Text("Open with...") },
                leadingContent = { Icon(Icons.Default.OpenInNew, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    viewModel.openFile(context, selectedFileForMenu!!)
                    selectedFileForMenu = null
                })
            )
            ListItem(
                headlineContent = { Text("Download") },
                leadingContent = { Icon(Icons.Default.Download, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    viewModel.downloadFile(selectedFileForMenu!!)
                    selectedFileForMenu = null
                })
            )
            ListItem(
                headlineContent = { Text("Rename") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    renameInput = selectedFileForMenu?.name ?: ""
                    showRenameDialog = true
                })
            )
            ListItem(
                headlineContent = { Text("Move") },
                leadingContent = { Icon(Icons.Default.DriveFileMove, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    showMoveDialog = selectedFileForMenu
                    selectedFileForMenu = null
                })
            )
            ListItem(
                headlineContent = { Text("Delete") },
                leadingContent = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.combinedClickable(onClick = {
                    showDeleteConfirmation = selectedFileForMenu
                    selectedFileForMenu = null
                })
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                TextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    placeholder = { Text("Enter folder name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (folderNameInput.isNotBlank()) {
                        viewModel.createFolder(folderNameInput)
                        showCreateFolderDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                selectedFileForMenu = null
                selectedFolderForMenu = null
            },
            title = { Text("Rename") },
            text = {
                TextField(
                    value = renameInput,
                    onValueChange = { renameInput = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameInput.isNotBlank()) {
                        selectedFileForMenu?.let { viewModel.renameFile(it, renameInput) }
                        selectedFolderForMenu?.let { viewModel.renameFolder(it, renameInput) }
                        showRenameDialog = false
                        selectedFileForMenu = null
                        selectedFolderForMenu = null
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    selectedFileForMenu = null
                    selectedFolderForMenu = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete") },
            text = {
                val name = when (val item = showDeleteConfirmation) {
                    is FileEntity -> item.name
                    is Folder -> item.name
                    else -> ""
                }
                Text("Are you sure you want to delete '$name'? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    when (val item = showDeleteConfirmation) {
                        is FileEntity -> viewModel.deleteFile(item)
                        is Folder -> viewModel.deleteFolder(item)
                    }
                    showDeleteConfirmation = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTransferCenter) {
        val transfers by viewModel.allTransfers.collectAsState(initial = emptyList())
        ModalBottomSheet(onDismissRequest = { showTransferCenter = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transfers", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { viewModel.clearFinishedTransfers() }) {
                        Text("Clear Finished")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (transfers.isEmpty()) {
                    Text("No active transfers", modifier = Modifier.padding(16.dp))
                } else {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(transfers) { transfer ->
                            ListItem(
                                headlineContent = { Text(transfer.name) },
                                supportingContent = {
                                    Column {
                                        Text("${transfer.type} • ${transfer.status}")
                                        if (transfer.status == "ACTIVE") {
                                            LinearProgressIndicator(
                                                progress = { transfer.progress },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    if (transfer.status == "ACTIVE" || transfer.status == "PENDING") {
                                        IconButton(onClick = { viewModel.cancelTransfer(transfer) }) {
                                            Icon(Icons.Default.Cancel, null)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMoveDialog != null) {
        val allFolders by viewModel.allFolders.collectAsState()
        AlertDialog(
            onDismissRequest = { showMoveDialog = null },
            title = { Text("Move to Folder") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    ListItem(
                        headlineContent = { Text("Home (Root)") },
                        leadingContent = { Icon(Icons.Default.Home, null) },
                        modifier = Modifier.combinedClickable(onClick = {
                            viewModel.moveFile(showMoveDialog!!, null)
                            showMoveDialog = null
                        })
                    )
                    allFolders.filter { it.id != showMoveDialog?.folderId }.forEach { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            leadingContent = { Icon(Icons.Default.Folder, null) },
                            modifier = Modifier.combinedClickable(onClick = {
                                viewModel.moveFile(showMoveDialog!!, folder.id)
                                showMoveDialog = null
                            })
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(folder: Folder, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    ElevatedCard(
        colors = if (isSelected) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.elevatedCardColors(),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                folder.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                formatDate(folder.createdDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(file: FileEntity, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    ElevatedCard(
        colors = if (isSelected) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.elevatedCardColors(),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                getFileIcon(file.mimeType, file.extension),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatDate(file.uploadDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyFolderState(isSearch: Boolean = false, onUploadClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearch) Icons.Default.SearchOff else Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (isSearch) "No matches found" else "No files in this folder yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
                if (isSearch) "Try a different search term" else "Upload or sync to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isSearch) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUploadClick) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload")
                }
            }
        }
    }
}

fun getFileIcon(mimeType: String?, extension: String?): ImageVector {
    return when {
        mimeType?.startsWith("image/") == true -> Icons.Default.Image
        mimeType?.startsWith("video/") == true -> Icons.Default.VideoLibrary
        mimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
        mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
        extension?.lowercase() == "pdf" -> Icons.Default.PictureAsPdf
        extension?.lowercase() in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.Inventory
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return format.format(date)
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
