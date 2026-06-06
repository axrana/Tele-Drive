package com.example.teledrive

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.teledrive.data.local.TeleDriveDatabase
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import com.example.teledrive.ui.screens.FileExplorerScreen
import com.example.teledrive.ui.screens.LoginScreen
import com.example.teledrive.ui.screens.SettingsScreen
import com.example.teledrive.ui.theme.TeleDriveTheme
import com.example.teledrive.viewmodel.FileExplorerViewModel
import com.example.teledrive.viewmodel.LoginViewModel
import com.example.teledrive.worker.TeleDriveWorkerFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as TeleDriveApplication
        val repository = app.repository
        val tdLibraryManager = app.tdLibraryManager

        setContent {
            TeleDriveTheme {
                val navController = rememberNavController()
                val userSession by repository.getUserSession().collectAsState(initial = null)
                var shouldCompress by remember { mutableStateOf(false) }

                NavHost(navController = navController, startDestination = if (userSession != null) "explorer" else "login") {
                    composable("login") {
                        val loginViewModel = LoginViewModel(tdLibraryManager, repository)

                        LaunchedEffect(Unit) {
                            loginViewModel.errorFlow.collect { message ->
                                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                            }
                        }

                        LoginScreen(loginViewModel)
                    }
                    composable("explorer") {
                        val explorerViewModel = FileExplorerViewModel(tdLibraryManager, repository)

                        LaunchedEffect(Unit) {
                            explorerViewModel.errorFlow.collect { message ->
                                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                            }
                        }

                        FileExplorerScreen(
                            viewModel = explorerViewModel,
                            shouldCompress = shouldCompress,
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            shouldCompress = shouldCompress,
                            onCompressToggle = { shouldCompress = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
