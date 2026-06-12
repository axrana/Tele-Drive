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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<FileEntity?>(null) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Any?>(null) }
    var showMoveDialog by remember { mutableStateOf<FileEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            val fileSize = try {
                context.contentResolver.openFileDescriptor(selectedUri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) { 0L }
            if (fileSize > 2L * 1024 * 1024 * 1024) {
                Toast.makeText(context, "File too large (max 2GB)", Toast.LENGTH_LONG).show()
                return@let
            }
            viewModel.uploadFile(context, selectedUri, shouldCompress)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        if (breadcrumb.isEmpty()) "Tele Drive" else breadcrumb.last().name,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.syncFromTelegram() }, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    ExtendedFloatingActionButton(
                        onClick = { folderNameInput = ""; showCreateFolderDialog = true; showFabMenu = false },
                        icon = { Icon(Icons.Default.CreateNewFolder, null) },
                        text = { Text("New Folder") },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { filePickerLauncher.launch("*/*"); showFabMenu = false },
                        icon = { Icon(Icons.Default.UploadFile, null) },
                        text = { Text("Upload File") },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search your drive") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
            }

            Crossfade(targetState = (isSyncing && folders.isEmpty() && files.isEmpty())) { syncing ->
                if (syncing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Crossfade(targetState = (folders.isEmpty() && files.isEmpty())) { empty ->
                        if (empty) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyState(isSearch = searchQuery.isNotEmpty()) { filePickerLauncher.launch("*/*") }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (breadcrumb.isEmpty() && searchQuery.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        StorageSummaryCard(totalStorageUsed, fileCount, folderCount)
                                    }
                                }

                                if (breadcrumb.isNotEmpty() || searchQuery.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        BreadcrumbChips(breadcrumb, searchQuery) { viewModel.navigateToFolder(it) }
                                    }
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    QuickActionStrip(
                                        isGridView = isGridView,
                                        onToggleView = { viewModel.toggleViewMode() },
                                        sortOrder = sortOrder,
                                        onSortClick = { viewModel.setSortOrder(it) }
                                    )
                                }

                                if (folders.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text("Folders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                    items(folders, key = { "folder_${it.id}" }) { folder ->
                                        FolderCard(folder, isGridView, onClick = { viewModel.navigateToFolder(folder) }, onLongClick = { selectedFolder = folder })
                                    }
                                }
                                if (files.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text("Files", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                    }
                                    items(files, key = { "file_${it.id}" }) { file ->
                                        FileCard(file, isGridView, onClick = { onFileClick(file) }, onLongClick = { selectedFile = file })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedFolder != null) {
        ItemActionSheet(
            title = selectedFolder!!.name,
            icon = Icons.Default.Folder,
            iconTint = ColorFolder,
            onDismiss = { selectedFolder = null },
            onRename = { renameInput = selectedFolder!!.name; showRenameDialog = true },
            onDelete = { showDeleteDialog = selectedFolder; selectedFolder = null }
        )
    }

    if (selectedFile != null) {
        ItemActionSheet(
            title = selectedFile!!.name,
            icon = getFileIcon(selectedFile!!.mimeType, selectedFile!!.extension),
            iconTint = getFileColor(selectedFile!!.mimeType, selectedFile!!.extension),
            onDismiss = { selectedFile = null },
            onRename = { renameInput = selectedFile!!.name; showRenameDialog = true },
            onDelete = { showDeleteDialog = selectedFile; selectedFile = null },
            onMove = { showMoveDialog = selectedFile; selectedFile = null }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { if (folderNameInput.isNotBlank()) { viewModel.createFolder(folderNameInput); showCreateFolderDialog = false } }) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false; selectedFile = null; selectedFolder = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteDialog != null) {
        val name = when (val item = showDeleteDialog) { is FileEntity -> item.name; is Folder -> item.name; else -> "" }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
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
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    if (showMoveDialog != null) {
        val allFolders by viewModel.allFolders.collectAsState()
        AlertDialog(
            onDismissRequest = { showMoveDialog = null },
            title = { Text("Move to...") },
            text = {
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    FolderMoveItem(name = "Home", icon = Icons.Default.Home, onClick = { viewModel.moveFile(showMoveDialog!!, null); showMoveDialog = null })
                    allFolders.filter { it.id != showMoveDialog?.folderId }.forEach { folder ->
                        FolderMoveItem(name = folder.name, icon = Icons.Default.Folder, onClick = { viewModel.moveFile(showMoveDialog!!, folder.id); showMoveDialog = null })
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMoveDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun StorageSummaryCard(totalStorageUsed: Long, fileCount: Int, folderCount: Int) {
    val storageLimit = 50L * 1024 * 1024 * 1024
    val progress = (totalStorageUsed.toFloat() / storageLimit).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Cloud Storage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("$folderCount folders • $fileCount files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(8.dp))
            Text("${formatSize(totalStorageUsed)} of 50 GB used", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreadcrumbChips(breadcrumb: List<Folder>, query: String, onNavigate: (Folder?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (query.isNotEmpty()) {
            item {
                InputChip(
                    selected = true,
                    onClick = { },
                    label = { Text("Search: $query") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) }
                )
            }
        } else {
            item {
                InputChip(
                    selected = breadcrumb.isEmpty(),
                    onClick = { onNavigate(null) },
                    label = { Text("Home") },
                    leadingIcon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp)) }
                )
            }
            items(breadcrumb) { folder ->
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                InputChip(
                    selected = folder == breadcrumb.last(),
                    onClick = { onNavigate(folder) },
                    label = { Text(folder.name) }
                )
            }
        }
    }
}

@Composable
fun QuickActionStrip(isGridView: Boolean, onToggleView: () -> Unit, sortOrder: FileExplorerViewModel.SortOrder, onSortClick: (FileExplorerViewModel.SortOrder) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("All Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            var showSortMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                FileExplorerViewModel.SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = { onSortClick(order); showSortMenu = false },
                        leadingIcon = { if (sortOrder == order) Icon(Icons.Default.Check, null) }
                    )
                }
            }
            IconButton(onClick = onToggleView) {
                Icon(if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(folder: Folder, isGrid: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Folder, null, tint = ColorFolder, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatDate(folder.createdDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = ColorFolder, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(folder.createdDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (isGrid) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(fileColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(fileIcon, null, tint = fileColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(fileColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(fileIcon, null, tint = fileColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDate(file.uploadDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemActionSheet(title: String, icon: ImageVector, iconTint: Color, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onMove: (() -> Unit)? = null) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HorizontalDivider(modifier = Modifier.alpha(0.5f))
            ListItem(
                headlineContent = { Text("Rename") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.combinedClickable { onRename(); onDismiss() }
            )
            if (onMove != null) {
                ListItem(
                    headlineContent = { Text("Move") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                    modifier = Modifier.combinedClickable { onMove(); onDismiss() }
                )
            }
            ListItem(
                headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.combinedClickable { onDelete(); onDismiss() }
            )
        }
    }
}

@Composable
fun EmptyState(isSearch: Boolean, onUpload: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(if (isSearch) Icons.Default.SearchOff else Icons.Default.CloudQueue, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text(if (isSearch) "No results found" else "Your drive is empty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (isSearch) "Try a different search term" else "Upload files to see them here", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!isSearch) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUpload) { Text("Upload Now") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderMoveItem(name: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        leadingContent = { Icon(icon, null, tint = ColorFolder) },
        modifier = Modifier.combinedClickable { onClick() }
    )
}

// Helpers moved back to screen for simplicity and build fix
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
