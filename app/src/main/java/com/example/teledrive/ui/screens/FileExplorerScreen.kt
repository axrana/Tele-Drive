package com.example.teledrive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.teledrive.viewmodel.FileExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(viewModel: FileExplorerViewModel, onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Alignment.CenterVertically used for Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tele Drive")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Text("File Explorer Content")
        }
    }
}
