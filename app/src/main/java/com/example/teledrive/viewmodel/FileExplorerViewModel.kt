package com.example.teledrive.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
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
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.TimeUnit

class FileExplorerViewModel(
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : ViewModel() {

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

    private val _shareLink = MutableSharedFlow<String>()
    val shareLink: SharedFlow<String> = _shareLink.asSharedFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _breadcrumb = MutableStateFlow<List<Folder>>(emptyList())
    val breadcrumb: StateFlow<List<Folder>> = _breadcrumb.asStateFlow()

    enum class SortOrder { NAME, DATE, SIZE, OLDEST }
    private val _sortOrder = MutableStateFlow(SortOrder.DATE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders = combine(_currentFolderId, _sortOrder) { id, sort ->
        id to sort
    }.flatMapLatest { (id, sort) ->
        repository.getFolders(id).map { list ->
            when (sort) {
                SortOrder.NAME -> list.sortedBy { it.name }
                SortOrder.DATE -> list.sortedByDescending { it.createdDate }
                SortOrder.OLDEST -> list.sortedBy { it.createdDate }
                SortOrder.SIZE -> list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val files = combine(_currentFolderId, _searchQuery, _sortOrder) { folderId, query, sort ->
        Triple(folderId, query, sort)
    }.flatMapLatest { (folderId, query, sort) ->
        val flow = if (query.isNotEmpty()) {
            repository.searchFiles(query)
        } else {
            repository.getFiles(folderId)
        }
        flow.map { list ->
            when (sort) {
                SortOrder.NAME -> list.sortedBy { it.name }
                SortOrder.DATE -> list.sortedByDescending { it.uploadDate }
                SortOrder.OLDEST -> list.sortedBy { it.uploadDate }
                SortOrder.SIZE -> list.sortedByDescending { it.size }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _pendingDownloads = MutableStateFlow<Set<String>>(emptySet())

    private val _uploadProgress = MutableStateFlow<Map<java.util.UUID, Float>>(emptyMap())
    val uploadProgress: StateFlow<Map<java.util.UUID, Float>> = _uploadProgress.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val allFolders: StateFlow<List<Folder>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageUsed: StateFlow<Long> = repository.getTotalStorageUsed()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val fileCount: StateFlow<Int> = repository.getFileCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val folderCount: StateFlow<Int> = repository.getFolderCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.getSettings().firstOrNull()?.sortOrder?.let { savedSort ->
                try { _sortOrder.value = SortOrder.valueOf(savedSort) } catch (e: Exception) {}
            }
        }
    }

    fun initDownloadObserver(context: Context) {
        viewModelScope.launch {
            tdLibraryManager.fileUpdates.collect { tdFile ->
                if (tdFile.local.downloadedSize > 0 && tdFile.size > 0) {
                    val progress = tdFile.local.downloadedSize.toFloat() / tdFile.size
                    _downloadProgress.value = _downloadProgress.value + (tdFile.remote.id to progress)
                }
                if (tdFile.local.isDownloadingCompleted) {
                    _downloadProgress.value = _downloadProgress.value - tdFile.remote.id
                    val entity = repository.getFileByTelegramFileId(tdFile.remote.id)
                    if (entity != null) {
                        repository.updateFile(entity.copy(thumbnailPath = tdFile.local.path))

                        if (_pendingDownloads.value.contains(tdFile.remote.id)) {
                            _pendingDownloads.value = _pendingDownloads.value - tdFile.remote.id
                            val success = com.example.teledrive.util.UriUtils.saveToDownloads(
                                context,
                                File(tdFile.local.path),
                                entity.name,
                                entity.mimeType
                            )
                            if (success) {
                                _errorFlow.emit("Saved to Downloads: ${entity.name}")
                            } else {
                                _errorFlow.emit("Failed to save ${entity.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun moveFile(file: FileEntity, newFolderId: Long?) {
        viewModelScope.launch {
            try {
                repository.moveFile(file.id, newFolderId)
                _errorFlow.emit("File moved")
            } catch (e: Exception) {
                _errorFlow.emit("Move failed: ${e.message}")
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
                val currentParentId = _currentFolderId.value
                val parentFolder = currentParentId?.let { repository.getFolderById(it) }
                val parentKey = parentFolder?.telegramThreadMsgId

                val folderMarker = if (parentKey != null && parentKey != 0L) {
                    "Folder:$name Parent:$parentKey"
                } else {
                    "Folder: $name"
                }
                val formattedText = TdApi.FormattedText()
                formattedText.text = folderMarker

                val content = TdApi.InputMessageText()
                content.text = formattedText
                content.clearDraft = true

                val query = TdApi.SendMessage()
                query.chatId = session.channelId
                query.inputMessageContent = content

                val message = tdLibraryManager.execute(query)
                val parentThreadId = if (parentKey != null && parentKey != 0L) parentKey else null
                repository.createFolder(name, currentParentId, message.id, parentThreadId)
            } catch (e: Exception) {
                _errorFlow.emit("Failed to create folder: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun uploadFile(context: Context, uri: android.net.Uri, shouldCompress: Boolean) {
        viewModelScope.launch {
            try {
                val folderId = _currentFolderId.value
                val realFile = com.example.teledrive.util.UriUtils.getFileFromUri(context, uri)
                    ?: run {
                        _errorFlow.emit("Could not read file")
                        return@launch
                    }
                if (realFile.length() > 2L * 1024 * 1024 * 1024) {
                    _errorFlow.emit("File too large! Max: 2GB")
                    return@launch
                }
                val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                    .setInputData(
                        workDataOf(
                            "filepath" to realFile.absolutePath,
                            "folderid" to (folderId ?: -1L),
                            "shouldcompress" to shouldCompress
                        )
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .addTag("upload")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)

                // Observe progress
                WorkManager.getInstance(context)
                    .getWorkInfoByIdFlow(workRequest.id)
                    .collect { workInfo ->
                        if (workInfo != null) {
                            when (workInfo.state) {
                                WorkInfo.State.RUNNING -> {
                                    val progress = workInfo.progress.getFloat("progress", 0f)
                                    _uploadProgress.value = _uploadProgress.value + (workRequest.id to progress)
                                }
                                WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                                    _uploadProgress.value = _uploadProgress.value - workRequest.id
                                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                        _errorFlow.emit("Upload successful: ${realFile.name}")
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
            } catch (e: Exception) {
                _errorFlow.emit("Upload failed: ${e.message}")
            }
        }
    }

    fun downloadFile(context: Context, fileEntity: FileEntity) {
        viewModelScope.launch {
            try {
                val workRequest = OneTimeWorkRequestBuilder<com.example.teledrive.worker.DownloadWorker>()
                    .setInputData(workDataOf("file_entity_id" to fileEntity.id))
                    .addTag("download")
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
                _errorFlow.emit("Download queued: ${fileEntity.name}")
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
                _shareLink.emit(link)
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

    fun resolveToken(context: Context, token: String, passwordEntry: String?) {
        viewModelScope.launch {
            try {
                val shareToken = repository.getShareToken(token) ?: throw Exception("Invalid token")
                if (shareToken.password != null && shareToken.password != passwordEntry) {
                    _errorFlow.emit("Incorrect password")
                    return@launch
                }
                val file = repository.getFileById(shareToken.fileId) ?: throw Exception("File not found")
                downloadFile(context, file)
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

    fun getFileById(id: Long): Flow<FileEntity?> = flow {
        emit(repository.getFileById(id))
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

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                val session = repository.getUserSession().firstOrNull() ?: return@launch
                repository.deleteFolderWithContents(tdLibraryManager, session.channelId, folder)
            } catch (e: Exception) {
                _errorFlow.emit("Delete folder failed: ${e.message}")
            }
        }
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            val current = repository.getSettings().firstOrNull() ?: com.example.teledrive.data.local.entity.Settings()
            repository.saveSettings(current.copy(sortOrder = order.name))
        }
    }

    fun syncFromTelegram() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val session = repository.getUserSession().firstOrNull() ?: return@launch
                repository.syncFromTelegram(tdLibraryManager, session.channelId)
                _errorFlow.emit("Sync completed")
            } catch (e: Exception) {
                _errorFlow.emit("Sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
