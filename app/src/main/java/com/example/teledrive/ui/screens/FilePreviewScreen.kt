package com.example.teledrive.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.ui.theme.*
import com.example.teledrive.viewmodel.FileExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    fileId: Long,
    viewModel: FileExplorerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val file by viewModel.getFileById(fileId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.shareLink.collect { link ->
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Share Link", link))
            android.widget.Toast.makeText(context, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file?.name ?: "Details", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        val currentFile = file
        if (currentFile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {

                // Preview area
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentFile.mimeType?.startsWith("image/") == true && currentFile.thumbnailPath != null) {
                        var scale by remember { mutableStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        AsyncImage(
                            model = currentFile.thumbnailPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        offset = if (scale == 1f) Offset.Zero else offset + pan
                                    }
                                }
                                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                            contentScale = ContentScale.Fit
                        )
                        if (scale > 1f) {
                            FilledTonalButton(onClick = { scale = 1f; offset = Offset.Zero }, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                                Text("Reset Zoom")
                            }
                        }
                    } else {
                        val fileColor = getFileColor(currentFile.mimeType, currentFile.extension)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)).background(fileColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(getFileIcon(currentFile.mimeType, currentFile.extension), contentDescription = null, modifier = Modifier.size(56.dp), tint = fileColor)
                            }
                            Spacer(Modifier.height(16.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = fileColor.copy(alpha = 0.15f)) {
                                Text(
                                    currentFile.extension?.uppercase() ?: "FILE",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = fileColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(currentFile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(formatSize(currentFile.size), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("•", color = MaterialTheme.colorScheme.outline)
                        Text(formatDate(currentFile.uploadDate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(32.dp))

                    // Primary Actions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { viewModel.downloadFile(context, currentFile) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.shareFile(currentFile, null) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share")
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Details Section
                    Text("File Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DetailItem(label = "Type", value = currentFile.mimeType ?: "Unknown", icon = Icons.Default.Description)
                            DetailItem(label = "Size", value = formatSize(currentFile.size), icon = Icons.Default.DataUsage)
                            DetailItem(label = "Uploaded", value = formatDate(currentFile.uploadDate), icon = Icons.Default.Event)
                            DetailItem(label = "Message ID", value = currentFile.telegramMsgId.toString(), icon = Icons.Default.Fingerprint)
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete \"${file?.name}\"?") },
            confirmButton = {
                Button(onClick = { file?.let { viewModel.deleteFile(it) }; showDeleteDialog = false; onBack() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
