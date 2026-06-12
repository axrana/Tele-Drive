package com.example.teledrive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.teledrive.data.local.entity.TransferEntity
import com.example.teledrive.data.local.entity.TransferStatus
import com.example.teledrive.data.local.entity.TransferType
import com.example.teledrive.viewmodel.FileExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(viewModel: FileExplorerViewModel) {
    val transfers by viewModel.allTransfers.collectAsState()
    val activeTransfers = transfers.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.PENDING }
    val completedTransfers = transfers.filter { it.status == TransferStatus.COMPLETED || it.status == TransferStatus.FAILED || it.status == TransferStatus.CANCELLED }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Transfers", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (transfers.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No transfers yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTransfers.isNotEmpty()) {
                    item { SectionHeader("Active") }
                    items(activeTransfers) { TransferItem(it) }
                }
                if (completedTransfers.isNotEmpty()) {
                    item { SectionHeader("History") }
                    items(completedTransfers) { TransferItem(it) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun TransferItem(transfer: TransferEntity) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = if (transfer.type == TransferType.UPLOAD) Icons.Default.Upload else Icons.Default.Download
            val color = if (transfer.type == TransferType.UPLOAD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (transfer.status == TransferStatus.IN_PROGRESS) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { transfer.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = color
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(transfer.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Text(formatSize(transfer.totalSize), style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        text = when (transfer.status) {
                            TransferStatus.COMPLETED -> "Completed"
                            TransferStatus.FAILED -> "Failed: ${transfer.errorMessage ?: "Unknown error"}"
                            TransferStatus.CANCELLED -> "Cancelled"
                            TransferStatus.PENDING -> "Pending..."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (transfer.status == TransferStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (transfer.status == TransferStatus.COMPLETED) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34C759), modifier = Modifier.size(24.dp))
            }
        }
    }
}
