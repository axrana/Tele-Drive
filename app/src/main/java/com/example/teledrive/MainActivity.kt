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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teledrive.ui.screens.LoginScreen
import com.example.teledrive.ui.screens.SettingsScreen
import com.example.teledrive.ui.theme.TeleDriveTheme
import com.example.teledrive.viewmodel.FileExplorerViewModel
import com.example.teledrive.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun LoginEffect(viewModel: LoginViewModel, tdManager: com.example.teledrive.tdlib.TdLibraryManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) {
        tdManager.errorFlow.collect { Toast.makeText(context, "Telegram: $it", Toast.LENGTH_LONG).show() }
    }
}

@Composable
fun ExplorerEffect(viewModel: FileExplorerViewModel, tdManager: com.example.teledrive.tdlib.TdLibraryManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) {
        tdManager.errorFlow.collect { Toast.makeText(context, "Telegram: $it", Toast.LENGTH_LONG).show() }
    }
}

class TeleDriveViewModelFactory(
    private val tdManager: com.example.teledrive.tdlib.TdLibraryManager,
    private val repository: com.example.teledrive.data.repository.TeleDriveRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(tdManager, repository) as T
            modelClass.isAssignableFrom(FileExplorerViewModel::class.java) -> FileExplorerViewModel(tdManager, repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as TeleDriveApplication
        val repository = app.repository
        val tdLibraryManager = app.tdLibraryManager
        val viewModelFactory = TeleDriveViewModelFactory(tdLibraryManager, repository)

        setContent {
            val persistentSettings by repository.getSettings().collectAsState(initial = com.example.teledrive.data.local.entity.Settings())
            val isDarkMode = persistentSettings?.isDarkMode ?: false

            TeleDriveTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val userSession by repository.getUserSession().collectAsState(initial = null)

                NavHost(navController = navController, startDestination = if (userSession != null) "explorer" else "login") {
                    composable("login") {
                        val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                        LoginEffect(loginViewModel, tdLibraryManager)
                        LoginScreen(loginViewModel)
                    }
                    composable("explorer") {
                        val explorerViewModel: FileExplorerViewModel = viewModel(factory = viewModelFactory)
                        ExplorerEffect(explorerViewModel, tdLibraryManager)
                        FileExplorerScreen(
                            viewModel = explorerViewModel,
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        val scope = rememberCoroutineScope()
                        SettingsScreen(
                            settings = persistentSettings ?: com.example.teledrive.data.local.entity.Settings(),
                            onSettingsChange = { newSettings ->
                                scope.launch {
                                    repository.saveSettings(newSettings)
                                }
                            },
                            onLogout = {
                                scope.launch {
                                    tdLibraryManager.logOut()
                                    repository.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("explorer") { inclusive = true }
                                    }
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
