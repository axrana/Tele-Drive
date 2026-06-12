package com.example.teledrive

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.teledrive.ui.screens.FileExplorerScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teledrive.data.local.entity.TransferStatus
import com.example.teledrive.ui.screens.LoginScreen
import com.example.teledrive.ui.screens.SettingsScreen
import com.example.teledrive.ui.screens.FilePreviewScreen
import com.example.teledrive.ui.screens.TransfersScreen
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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Single instance for shared state across File Explorer, Transfers, and Preview
                val explorerViewModel: FileExplorerViewModel = viewModel(factory = viewModelFactory)

                val showBottomBar = currentDestination?.route in listOf("explorer", "transfers", "settings")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                    label = { Text("Files") },
                                    selected = currentDestination?.hierarchy?.any { it.route == "explorer" } == true,
                                    onClick = {
                                        navController.navigate("explorer") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = {
                                        val transfers by explorerViewModel.allTransfers.collectAsState()
                                        val activeCount = transfers.count { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.PENDING }
                                        BadgedBox(badge = {
                                            if (activeCount > 0) {
                                                Badge { Text(activeCount.toString()) }
                                            }
                                        }) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null)
                                        }
                                    },
                                    label = { Text("Transfers") },
                                    selected = currentDestination?.hierarchy?.any { it.route == "transfers" } == true,
                                    onClick = {
                                        navController.navigate("transfers") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text("Settings") },
                                    selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (userSession != null) "explorer" else "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                            LoginEffect(loginViewModel, tdLibraryManager)
                            LoginScreen(loginViewModel)
                        }
                        composable("explorer") {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            LaunchedEffect(Unit) { explorerViewModel.initDownloadObserver(context) }
                            ExplorerEffect(explorerViewModel, tdLibraryManager)
                            FileExplorerScreen(
                                viewModel = explorerViewModel,
                                shouldCompress = persistentSettings?.shouldCompress ?: false,
                                onFileClick = { file ->
                                    navController.navigate("preview/${file.id}")
                                }
                            )
                        }
                        composable("transfers") {
                            TransfersScreen(explorerViewModel)
                        }
                        composable("preview/{fileId}") { backStackEntry ->
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val fileId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull() ?: 0L
                            LaunchedEffect(Unit) { explorerViewModel.initDownloadObserver(context) }
                            FilePreviewScreen(
                                fileId = fileId,
                                viewModel = explorerViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            val scope = rememberCoroutineScope()
                            SettingsScreen(
                                settings = persistentSettings ?: com.example.teledrive.data.local.entity.Settings(),
                                channelId = userSession?.channelId,
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
}
