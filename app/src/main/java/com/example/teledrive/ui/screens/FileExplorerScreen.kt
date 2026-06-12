package com.example.teledrive.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var showMoveDialog by remember { mutableStateOf<FileEntity?>(null) }
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
                        if (searchQuery.isEmpty()) {
                            Text("Tele Drive", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        } else {
                            Text("Search Results", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncFromTelegram() }, enabled = !isSyncing) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sync")
                            }
                        }
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            FileExplorerViewModel.SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { viewModel.setSortOrder(order); showSortMenu = false },
                                    leadingIcon = { if (sortOrder == order) Icon(Icons.Default.Check, null, tint = TeleBluePrimary) }
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Upload/Download progress bar
                val allProgress = uploadProgress.values + downloadProgress.values
                if (allProgress.isNotEmpty()) {
                    val avg = allProgress.average().toFloat()
                    LinearProgressIndicator(
                        progress = { avg },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = TeleBluePrimary,
                        trackColor = TeleBlueContainer
                    )
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search in Tele Drive") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = TeleBluePrimary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        TextButton(onClick = { viewModel.navigateToFolder(null) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Home", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    items(breadcrumb) { folder ->
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { viewModel.navigateToFolder(folder) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(folder.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
                HorizontalDivider()
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
                LazyVerticalGrid(
                    columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Section header for folders
                    if (folders.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = "Folders", count = folders.size)
                        }
                        items(folders, key = { it.id }) { folder ->
                            FolderCard(
                                folder = folder,
                                isGrid = isGridView,
                                onClick = { viewModel.navigateToFolder(folder) },
                                onLongClick = { selectedFolder = folder }
                            )
                        }
                    }
                    // Section header for files
                    if (files.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = "Files", count = files.size)
                        }
                        items(files, key = { it.id }) { file ->
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
            onMove = null
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
            onMove = { showMoveDialog = selectedFile; selectedFile = null }
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
                    FolderMoveItem(name = "Home (Root)", icon = Icons.Default.Home, onClick = { viewModel.moveFile(showMoveDialog!!, null); showMoveDialog = null })
                    allFolders.filter { it.id != showMoveDialog?.folderId }.forEach { folder ->
                        FolderMoveItem(name = folder.name, icon = Icons.Default.Folder, onClick = { viewModel.moveFile(showMoveDialog!!, folder.id); showMoveDialog = null })
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
fun StorageSummaryCard(totalStorageUsed: Long, fileCount: Int, folderCount: Int) {
    val storageLimit = 50L * 1024 * 1024 * 1024
    val progress = (totalStorageUsed.toFloat() / storageLimit).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TeleBlueContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = TeleBluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Storage", style = MaterialTheme.typography.titleSmall, color = TeleBlueDark, fontWeight = FontWeight.SemiBold)
                }
                Text("$folderCount folders • $fileCount files", style = MaterialTheme.typography.labelSmall, color = TeleBlueDark.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TeleBluePrimary,
                trackColor = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text("${formatSize(totalStorageUsed)} used of 50 GB", style = MaterialTheme.typography.bodySmall, color = TeleBlueDark)
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(folder: Folder, isGrid: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(ColorFolder.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(32.dp), tint = ColorFolder)
                }
                Spacer(Modifier.height(10.dp))
                Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(formatDate(folder.createdDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(ColorFolder.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(26.dp), tint = ColorFolder)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(folder.createdDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(fileColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(fileIcon, contentDescription = null, modifier = Modifier.size(32.dp), tint = fileColor)
                }
                Spacer(Modifier.height(10.dp))
                Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(fileColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(fileIcon, contentDescription = null, modifier = Modifier.size(26.dp), tint = fileColor)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDate(file.uploadDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemContextMenu(title: String, icon: ImageVector, iconTint: Color, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onMove: (() -> Unit)?) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconTint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            ContextMenuItem(icon = Icons.Default.Edit, label = "Rename", onClick = { onRename(); onDismiss() })
            if (onMove != null) {
                ContextMenuItem(icon = Icons.Default.DriveFileMove, label = "Move", onClick = { onMove(); onDismiss() })
            }
            ContextMenuItem(icon = Icons.Default.Delete, label = "Delete", tint = MaterialTheme.colorScheme.error, onClick = { onDelete(); onDismiss() })
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
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(TeleBlueContainer), contentAlignment = Alignment.Center) {
            Icon(if (isSearch) Icons.Default.SearchOff else Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(52.dp), tint = TeleBluePrimary)
        }
        Spacer(Modifier.height(24.dp))
        Text(if (isSearch) "No results found" else "Nothing here yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (isSearch) "Try different keywords" else "Upload your first file to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (!isSearch) {
            Spacer(Modifier.height(28.dp))
            Button(onClick = onUpload, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Upload File")
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
