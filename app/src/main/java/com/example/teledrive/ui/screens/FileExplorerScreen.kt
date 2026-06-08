package com.example.teledrive.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.viewmodel.FileExplorerViewModel
import com.example.teledrive.util.UriUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(viewModel: FileExplorerViewModel, shouldCompress: Boolean, onOpenSettings: () -> Unit) {
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val breadcrumbs by viewModel.breadcrumb.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showUploadOption by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            UriUtils.getFileFromUri(context, it)?.let { file ->
                viewModel.uploadFile(context, file.absolutePath, shouldCompress)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Home", modifier = Modifier.clickable { viewModel.navigateToFolder(null) })
                            breadcrumbs.forEach { folder ->
                                Text(" > ")
                                Text(folder.name, modifier = Modifier.clickable { viewModel.navigateToFolder(folder) })
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search files...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showUploadOption) {
                    SmallFloatingActionButton(
                        onClick = { filePickerLauncher.launch("*/*"); showUploadOption = false },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload File")
                    }
                    SmallFloatingActionButton(
                        onClick = { showCreateFolderDialog = true; showUploadOption = false },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                    }
                }
                FloatingActionButton(onClick = { showUploadOption = !showUploadOption }) {
                    Icon(if (showUploadOption) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(folders) { folder ->
                FolderItem(
                    folder = folder,
                    onClick = { viewModel.navigateToFolder(folder) },
                    onRename = { newName -> viewModel.renameFolder(folder, newName) }
                )
            }
            items(files) { file ->
                FileItem(
                    file = file,
                    onDownload = { viewModel.downloadFile(file) },
                    onShare = { password -> viewModel.shareFile(file, password) },
                    onDelete = { viewModel.deleteFile(file) },
                    onRename = { newName -> viewModel.renameFile(file, newName) }
                )
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(folder: Folder, onClick: () -> Unit, onRename: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameDialog(
            currentName = folder.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.padding(8.dp).combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true }
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(folder.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { showRenameDialog = true; showMenu = false })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(file: FileEntity, onDownload: () -> Unit, onShare: (String?) -> Unit, onDelete: () -> Unit, onRename: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        ShareFileDialog(
            onDismiss = { showShareDialog = false },
            onConfirm = { password ->
                onShare(password)
                showShareDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete File?") },
            text = { Text("This will permanently delete ${file.name} from Telegram and cannot be undone.") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = file.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.padding(8.dp).combinedClickable(
            onClick = { onDownload() },
            onLongClick = { showMenu = true }
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (file.thumbnailPath != null) {
            AsyncImage(
                model = file.thumbnailPath,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
        } else {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(64.dp))
        }
        Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Download") }, onClick = { onDownload(); showMenu = false })
            DropdownMenuItem(text = { Text("Share") }, onClick = { showShareDialog = true; showMenu = false })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { showRenameDialog = true; showMenu = false })
            DropdownMenuItem(
                text = { Text("Delete", color = Color.Red) },
                onClick = { showDeleteConfirm = true; showMenu = false }
            )
        }
    }
}

@Composable
fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ShareFileDialog(onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var passwordEnabled by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share File") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = passwordEnabled, onCheckedChange = { passwordEnabled = it })
                    Text("Add password protection")
                }
                if (passwordEnabled) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(if (passwordEnabled) password else null) }) { Text("Generate Link") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Folder Name") })
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
