package com.example.teledrive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    shouldCompress: Boolean,
    onCompressToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Compress images before upload")
                Switch(checked = shouldCompress, onCheckedChange = onCompressToggle)
            }
            Text(
                "Reduces file size by resizing images to 1024px max and 85% quality.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
