package com.example.teledrive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.teledrive.data.local.entity.Settings
import com.example.teledrive.viewmodel.FileExplorerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FileExplorerViewModel,
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val totalBytes by viewModel.totalStorageUsed.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Text("Storage Used", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val progress = totalBytes / (50L * 1024 * 1024 * 1024).toFloat()
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${formatBytes(totalBytes)} used of 50 GB",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Compress images before upload")
                Switch(
                    checked = settings.shouldCompress,
                    onCheckedChange = { onSettingsChange(settings.copy(shouldCompress = it)) }
                )
            }
            Text(
                "Reduces file size by resizing images to 1024px max and 85% quality.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dark Mode")
                Switch(
                    checked = settings.isDarkMode,
                    onCheckedChange = { onSettingsChange(settings.copy(isDarkMode = it)) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onLogout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1] + "B"
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
