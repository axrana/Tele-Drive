package com.example.teledrive.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
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
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var selectedFileForMenu by remember { mutableStateOf<FileEntity?>(null) }
    var selectedFolderForMenu by remember { mutableStateOf<Folder?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
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
            TopAppBar(
                title = { Text("Tele Drive") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (downloadProgress.isNotEmpty()) {
                    val progress = downloadProgress.values.average().toFloat()
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { progress }
                    )
                }
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Storage used: ${formatSize(totalStorageUsed)} used",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
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

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
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
                        onLongClick = {
                            selectedFileForMenu = file
                        }
                    )
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
                    selectedFileForMenu?.let { viewModel.deleteFile(it) }
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
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(folder: Folder, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
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
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(file: FileEntity, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Could implement preview/download */ },
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
