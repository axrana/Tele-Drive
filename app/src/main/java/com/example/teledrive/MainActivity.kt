package com.example.teledrive

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            val persistentSettings by repository.getSettings().collectAsState(initial = null)
            val isDarkMode = persistentSettings?.isDarkMode ?: false

            TeleDriveTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val userSession by repository.getUserSession().collectAsState(initial = null)
                val shouldCompress = persistentSettings?.shouldCompress ?: false

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
                        val explorerViewModel = FileExplorerViewModel(tdLibraryManager, repository)
                        SettingsScreen(
                            settings = persistentSettings ?: com.example.teledrive.data.local.entity.Settings(),
                            onSettingsChange = { newSettings ->
                                kotlinx.coroutines.MainScope().launch {
                                    repository.saveSettings(newSettings)
                                }
                            },
                            onLogout = {
                                explorerViewModel.logOut()
                                navController.navigate("login") {
                                    popUpTo("explorer") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
