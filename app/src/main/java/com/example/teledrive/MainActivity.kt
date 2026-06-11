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
import com.example.teledrive.ui.screens.FilePreviewScreen
import com.example.teledrive.ui.theme.TeleDriveTheme
import com.example.teledrive.ui.viewmodel.FileExplorerViewModel
import com.example.teledrive.ui.viewmodel.LoginViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun LoginEffect(viewModel: LoginViewModel, tdManager: com.example.teledrive.telegram.TdLibraryManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) {
        tdManager.errorFlow.collect { Toast.makeText(context, "Telegram: $it", Toast.LENGTH_LONG).show() }
    }
}

@Composable
fun ExplorerEffect(viewModel: FileExplorerViewModel, tdManager: com.example.teledrive.telegram.TdLibraryManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.errorFlow.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) {
        tdManager.errorFlow.collect { Toast.makeText(context, "Telegram: $it", Toast.LENGTH_LONG).show() }
    }
}

class TeleDriveViewModelFactory(
    private val tdManager: com.example.teledrive.telegram.TdLibraryManager,
    private val repository: com.example.teledrive.domain.repository.TeleDriveRepository,
    private val transferManager: com.example.teledrive.transfers.TransferManager,
    owner: androidx.savedstate.SavedStateRegistryOwner,
    defaultArgs: android.os.Bundle? = null
) : androidx.lifecycle.AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: androidx.lifecycle.SavedStateHandle
    ): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(tdManager, repository) as T
            modelClass.isAssignableFrom(FileExplorerViewModel::class.java) -> FileExplorerViewModel(tdManager, repository, transferManager, handle) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class MainActivity : ComponentActivity() {
    private fun handleIntent(intent: android.content.Intent?, transferManager: com.example.teledrive.transfers.TransferManager) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (android.content.Intent.ACTION_SEND == action && type != null) {
            intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)?.let { uri ->
                enqueueUploadFromUri(uri, transferManager)
            }
        } else if (android.content.Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            intent.getParcelableArrayListExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)?.let { uris ->
                uris.forEach { uri -> enqueueUploadFromUri(uri, transferManager) }
            }
        }
    }

    private fun enqueueUploadFromUri(uri: android.net.Uri, transferManager: com.example.teledrive.transfers.TransferManager) {
        val context = this
        lifecycleScope.launch {
            val file = com.example.teledrive.storage.UriUtils.getFileFromUri(context, uri)
            if (file != null) {
                transferManager.enqueueUpload(
                    localPath = file.absolutePath,
                    folderId = null,
                    name = file.name,
                    size = file.length(),
                    mimeType = com.example.teledrive.storage.UriUtils.getMimeType(file.name)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as TeleDriveApplication
        val repository = app.repository
        val tdLibraryManager = app.tdLibraryManager
        val transferManager = com.example.teledrive.transfers.TransferManager(this, repository)
        val viewModelFactory = TeleDriveViewModelFactory(tdLibraryManager, repository, transferManager, this)

        handleIntent(intent, transferManager)

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
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val explorerViewModel: FileExplorerViewModel = viewModel(factory = viewModelFactory)
                        LaunchedEffect(Unit) { explorerViewModel.initDownloadObserver(context) }
                        ExplorerEffect(explorerViewModel, tdLibraryManager)
                        FileExplorerScreen(
                            viewModel = explorerViewModel,
                            shouldCompress = persistentSettings?.shouldCompress ?: false,
                            onOpenSettings = { navController.navigate("settings") },
                            onFileClick = { file ->
                                navController.navigate("preview/${file.id}")
                            }
                        )
                    }
                    composable("preview/{fileId}") { backStackEntry ->
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val fileId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull() ?: 0L
                        val explorerViewModel: FileExplorerViewModel = viewModel(factory = viewModelFactory)
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
