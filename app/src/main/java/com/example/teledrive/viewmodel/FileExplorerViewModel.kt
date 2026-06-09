package com.example.teledrive.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import com.example.teledrive.worker.UploadWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.concurrent.TimeUnit

class FileExplorerViewModel(
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : ViewModel() {

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _breadcrumb = MutableStateFlow<List<Folder>>(emptyList())
    val breadcrumb: StateFlow<List<Folder>> = _breadcrumb.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders = _currentFolderId.flatMapLatest { id ->
        repository.getFolders(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val files = combine(_currentFolderId, _searchQuery) { folderId, query ->
        query to folderId
    }.flatMapLatest { (query, folderId) ->
        if (query.isNotEmpty()) {
            repository.searchFiles(query)
        } else {
            repository.getFiles(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    val totalStorageUsed: StateFlow<Long> = repository.getTotalStorageUsed()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        viewModelScope.launch {
            tdLibraryManager.fileUpdates.collect { tdFile ->
                if (tdFile.local.downloadedSize > 0) {
                    val progress = tdFile.local.downloadedSize.toFloat() / tdFile.size
                    // Remote ID is used to track progress
                    _downloadProgress.value = _downloadProgress.value + (tdFile.remote.id to progress)
                }
            }
        }
    }

    fun navigateToFolder(folder: Folder?) {
        _currentFolderId.value = folder?.id
        if (folder == null) {
            _breadcrumb.value = emptyList()
        } else {
            viewModelScope.launch {
                val list = mutableListOf<Folder>()
                var current: Folder? = folder
                while (current != null) {
                    list.add(0, current)
                    current = current.parentFolderId?.let { repository.getFolderById(it) }
                }
                _breadcrumb.value = list
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                val session = repository.getUserSession().firstOrNull() ?: return@launch
                try {
                    // Try creating as Forum Topic first
                    val topic = tdLibraryManager.execute(TdApi.CreateForumTopic(session.channelId, name, null))
                    repository.createFolder(name, _currentFolderId.value, topic.messageThreadId)
                } catch (e: Exception) {
                    // Fallback to regular message thread
                    val formattedText = TdApi.FormattedText()
                    formattedText.text = "Folder: $name"
                    val content = TdApi.InputMessageText(formattedText, false, true)
                    val message = tdLibraryManager.execute(TdApi.SendMessage(session.channelId, 0, null, null, null, content))
                    repository.createFolder(name, _currentFolderId.value, message.id)
                }
            } catch (e: Exception) {
                _errorFlow.emit("Failed to create folder: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun uploadFile(context: Context, path: String, shouldCompress: Boolean) {
        viewModelScope.launch {
            try {
                val folderId = _currentFolderId.value ?: return@launch
                val file = File(path)

                if (file.length() > 2L * 1024 * 1024 * 1024) {
                    _errorFlow.emit("File too large! Max: 2GB")
                    return@launch
                }

                val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                    .setInputData(workDataOf(
                        "file_path" to path,
                        "folder_id" to folderId,
                        "should_compress" to shouldCompress
                    ))
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (e: Exception) {
                _errorFlow.emit("Upload failed: ${e.message}")
            }
        }
    }

    fun downloadFile(fileEntity: FileEntity) {
        viewModelScope.launch {
            try {
                val file = tdLibraryManager.execute(TdApi.GetRemoteFile(fileEntity.telegramFileId))
                tdLibraryManager.send(TdApi.DownloadFile(file.id, 1, 0, 0, false))
            } catch (e: Exception) {
                _errorFlow.emit("Download failed: ${e.message}")
            }
        }
    }

    fun shareFile(file: FileEntity, password: String?) {
        viewModelScope.launch {
            try {
                val shareManager = com.example.teledrive.data.repository.ShareManager(repository)
                val link = shareManager.generateShareLink(file, password)
                _errorFlow.emit("Share link copied: $link")
            } catch (e: Exception) {
                _errorFlow.emit("Sharing failed: ${e.message}")
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            try {
                tdLibraryManager.logOut()
                repository.clearSession()
            } catch (e: Exception) {
                _errorFlow.emit("Logout failed: ${e.message}")
            }
        }
    }

    fun resolveToken(token: String, passwordEntry: String?) {
        viewModelScope.launch {
            try {
                val shareToken = repository.getShareToken(token) ?: throw Exception("Invalid token")
                if (shareToken.password != null && shareToken.password != passwordEntry) {
                    _errorFlow.emit("Incorrect password")
                    return@launch
                }
                val file = repository.getFileById(shareToken.fileId) ?: throw Exception("File not found")
                downloadFile(file)
            } catch (e: Exception) {
                _errorFlow.emit("Resolution failed: ${e.message}")
            }
        }
    }

    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            try {
                val session = repository.getUserSession().firstOrNull() ?: return@launch
                repository.deleteFileFromTelegram(tdLibraryManager, session.channelId, file.telegramMsgId)
                repository.deleteFile(file)
            } catch (e: Exception) {
                _errorFlow.emit("Delete failed: ${e.message}")
            }
        }
    }

    fun renameFile(file: FileEntity, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameFile(file.id, newName)
            } catch (e: Exception) {
                _errorFlow.emit("Rename failed: ${e.message}")
            }
        }
    }

    fun renameFolder(folder: Folder, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameFolder(folder.id, newName)
            } catch (e: Exception) {
                _errorFlow.emit("Rename failed: ${e.message}")
            }
        }
    }
}
