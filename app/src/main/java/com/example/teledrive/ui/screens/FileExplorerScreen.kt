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
import com.example.teledrive.viewmodel.FileExplorerViewModel
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
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsState()
    val fileCount by viewModel.fileCount.collectAsState()
    val folderCount by viewModel.folderCount.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var selectedFileForMenu by remember { mutableStateOf<FileEntity?>(null) }
    var selectedFolderForMenu by remember { mutableStateOf<Folder?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Any?>(null) }
    var showMoveDialog by remember { mutableStateOf<FileEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileSize = try {
                context.contentResolver.openFileDescriptor(selectedUri, "r")?.use { fd ->
                    fd.statSize
                } ?: 0L
            } catch (e: Exception) { 0L }

            if (fileSize > 2L * 1024 * 1024 * 1024) {
                Toast.makeText(context, "File too large! Max 2GB. This file: ${formatSize(fileSize)}", Toast.LENGTH_LONG).show()
                return@let
            }
            Toast.makeText(context, "Uploading: ${formatSize(fileSize)}", Toast.LENGTH_SHORT).show()
            viewModel.uploadFile(context, selectedUri, shouldCompress)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                                text = { Text(order.name.lowercase().capitalize()) },
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.navigateToFolder(null) }) {
                    Text("Home")
                }
                breadcrumb.forEach { folder ->
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
                            onClick = { viewModel.navigateToFolder(folder) },
                            onLongClick = {
                                selectedFolderForMenu = folder
                            }
                        )
                    }
                    items(files) { file ->
                        FileItem(
                            file = file,
                            onClick = { onFileClick(file) },
                            onLongClick = {
                                selectedFileForMenu = file
                            }
                        )
                    }
                }
            }
        }
    }

    // Dropdown Menus
    if (selectedFolderForMenu != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { selectedFolderForMenu = null }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    renameInput = selectedFolderForMenu?.name ?: ""
                    showRenameDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Move") },
                onClick = {
                    showMoveDialog = selectedFileForMenu
                    selectedFileForMenu = null
                },
                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    selectedFileForMenu?.let { viewModel.shareFile(it, null) }
                    selectedFileForMenu = null
                },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showDeleteConfirmation = selectedFolderForMenu
                    selectedFolderForMenu = null
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }

    if (selectedFileForMenu != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { selectedFileForMenu = null }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    renameInput = selectedFileForMenu?.name ?: ""
                    showRenameDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showDeleteConfirmation = selectedFileForMenu
                    selectedFileForMenu = null
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
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
fun FolderItem(folder: Folder, onClick: () -> Unit, onLongClick: () -> Unit) {
    ElevatedCard(
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
fun FileItem(file: FileEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    ElevatedCard(
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
