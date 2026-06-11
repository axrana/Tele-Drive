package com.example.teledrive.data.repository

import com.example.teledrive.data.local.dao.FileDao
import com.example.teledrive.data.local.dao.FolderDao
import com.example.teledrive.data.local.dao.ShareTokenDao
import com.example.teledrive.data.local.dao.UserSessionDao
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.data.local.entity.ShareToken
import com.example.teledrive.data.local.entity.UserSession
import com.example.teledrive.telegram.MetadataHelper
import kotlinx.coroutines.flow.Flow
import org.drinkless.tdlib.TdApi

class TeleDriveRepository(
    private val userSessionDao: UserSessionDao,
    private val folderDao: FolderDao,
    private val fileDao: FileDao,
    private val shareTokenDao: ShareTokenDao,
    private val settingsDao: com.example.teledrive.data.local.dao.SettingsDao,
    private val transferDao: com.example.teledrive.data.local.dao.TransferDao
) {
    fun getUserSession(): Flow<UserSession?> = userSessionDao.getUserSession()
    suspend fun saveSession(session: UserSession) = userSessionDao.insertSession(session)
    suspend fun clearSession() = userSessionDao.clearSession()

    fun getFolders(parentId: Long?): Flow<List<Folder>> = folderDao.getFoldersInParent(parentId)
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()
    suspend fun createFolder(name: String, parentId: Long?, threadId: Long, parentThreadId: Long? = null): Long {
        val folder = Folder(
            name = name,
            parentFolderId = parentId,
            telegramThreadMsgId = threadId,
            telegramParentThreadId = parentThreadId,
            createdDate = System.currentTimeMillis()
        )
        return folderDao.createFolder(folder)
    }
    suspend fun getFolderById(id: Long) = folderDao.getFolderById(id)
    suspend fun renameFolder(id: Long, newName: String) = folderDao.renameFolder(id, newName)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    fun getFiles(folderId: Long?): Flow<List<FileEntity>> = fileDao.getFilesInFolder(folderId)
    fun searchFiles(query: String): Flow<List<FileEntity>> = fileDao.searchFiles(query)
    suspend fun createFile(file: FileEntity) = fileDao.createFile(file)
    suspend fun getFileById(id: Long) = fileDao.getFileById(id)
    suspend fun getFileByTelegramFileId(telegramFileId: String) = fileDao.getFileByTelegramFileId(telegramFileId)
    suspend fun updateFile(file: FileEntity) = fileDao.updateFile(file)
    suspend fun renameFile(id: Long, newName: String) = fileDao.renameFile(id, newName)
    suspend fun moveFile(id: Long, folderId: Long?) = fileDao.moveFile(id, folderId)
    suspend fun deleteFile(file: FileEntity) = fileDao.deleteFile(file)
    fun getTotalStorageUsed(): Flow<Long?> = fileDao.getTotalStorageUsed()
    fun getFileCount(): Flow<Int> = fileDao.getFileCount()
    fun getFolderCount(): Flow<Int> = folderDao.getFolderCount()

    suspend fun deleteFileFromTelegram(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        chatId: Long,
        messageId: Long
    ) {
        if (messageId != 0L) {
            val query = TdApi.DeleteMessages()
            query.chatId = chatId
            query.messageIds = longArrayOf(messageId)
            query.revoke = true
            tdLibraryManager.execute(query)
        }
    }

    suspend fun deleteFolderWithContents(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        chatId: Long,
        folder: Folder
    ) {
        val filesInFolder = fileDao.getFilesInFolderSync(folder.id)
        val messageIds = filesInFolder.map { it.telegramMsgId }.filter { it != 0L }.toLongArray()
        if (messageIds.isNotEmpty()) {
            val query = TdApi.DeleteMessages()
            query.chatId = chatId
            query.messageIds = messageIds
            query.revoke = true
            tdLibraryManager.execute(query)
        }
        fileDao.deleteFilesInFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    suspend fun createShareToken(fileId: Long, token: String, userId: Long, password: String? = null): Long {
        val shareToken = ShareToken(
            fileId = fileId,
            token = token,
            userId = userId,
            password = password,
            createdAt = System.currentTimeMillis(),
            expiresAt = null
        )
        return shareTokenDao.createShareToken(shareToken)
    }
    suspend fun getShareToken(token: String) = shareTokenDao.getShareToken(token)

    fun getSettings(): Flow<com.example.teledrive.data.local.entity.Settings?> = settingsDao.getSettings()
    suspend fun saveSettings(settings: com.example.teledrive.data.local.entity.Settings) = settingsDao.saveSettings(settings)

    fun getAllTransfers() = transferDao.getAllTransfers()
    fun getActiveTransfers() = transferDao.getActiveTransfers()
    suspend fun insertTransfer(transfer: com.example.teledrive.data.local.entity.TransferEntity) = transferDao.insertTransfer(transfer)
    suspend fun updateTransfer(transfer: com.example.teledrive.data.local.entity.TransferEntity) = transferDao.updateTransfer(transfer)
    suspend fun updateTransferProgress(id: Long, status: com.example.teledrive.data.local.entity.TransferStatus, progress: Float, transferredSize: Long) =
        transferDao.updateProgress(id, status, progress, transferredSize)
    suspend fun deleteTransfer(id: Long) = transferDao.deleteTransfer(id)
    suspend fun clearFinishedTransfers() = transferDao.clearFinishedTransfers()
    suspend fun getTransferById(id: Long) = transferDao.getTransferById(id)

    suspend fun syncFromTelegram(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        chatId: Long
    ) {
        val limit = 100
        val allMessages = mutableListOf<TdApi.Message>()
        var lastMessageId = 0L

        while (true) {
            val query = TdApi.GetChatHistory()
            query.chatId = chatId
            query.fromMessageId = lastMessageId
            query.offset = 0
            query.limit = limit
            query.onlyLocal = false

            val messages = tdLibraryManager.execute(query)
            if (messages.messages.isEmpty()) break
            allMessages.addAll(messages.messages)
            lastMessageId = messages.messages.last().id
            if (messages.messages.size < limit) break
        }

        // Process text markers for folders
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageText) {
                val text = (msg.content as TdApi.MessageText).text.text
                MetadataHelper.parseFolderMetadata(text)?.let { meta ->
                    val existing = folderDao.getFolderByThreadId(msg.id)
                    if (existing == null) {
                        folderDao.createFolder(
                            Folder(
                                name = meta.name,
                                telegramThreadMsgId = msg.id,
                                telegramParentThreadId = meta.parentId,
                                createdDate = msg.date.toLong() * 1000
                            )
                        )
                    } else {
                        folderDao.updateFolder(existing.copy(
                            name = meta.name,
                            telegramParentThreadId = meta.parentId
                        ))
                    }
                }
            }
        }

        // Rebuild folder hierarchy
        val allLocalFolders = folderDao.getAllFoldersSync()
        allLocalFolders.forEach { folder ->
            val parentThreadId = folder.telegramParentThreadId
            if (parentThreadId != null) {
                val parent = folderDao.getFolderByThreadId(parentThreadId)
                if (parent != null && folder.parentFolderId != parent.id) {
                    folderDao.updateParentId(folder.id, parent.id)
                }
            }
        }

        // Sync files from messages
        var recoveredFolderId: Long? = null
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageDocument) {
                val docContent = msg.content as TdApi.MessageDocument
                val doc = docContent.document
                val caption = docContent.caption?.text ?: ""

                val telegramFolderId = MetadataHelper.parseFileFolderId(caption)
                var localFolderId = telegramFolderId?.let { folderDao.getFolderByThreadId(it)?.id }

                if (telegramFolderId != null && localFolderId == null) {
                    if (recoveredFolderId == null) {
                        val existingRecovered = folderDao.getAllFoldersSync()
                            .find { it.name == "Recovered" && it.parentFolderId == null }
                        recoveredFolderId = existingRecovered?.id ?: folderDao.createFolder(
                            Folder(
                                name = "Recovered",
                                telegramThreadMsgId = -1L,
                                createdDate = System.currentTimeMillis()
                            )
                        )
                    }
                    localFolderId = recoveredFolderId
                }

                val existingFile = fileDao.getFileByTelegramMsgId(msg.id)
                if (existingFile == null) {
                    fileDao.createFile(
                        FileEntity(
                            name = doc.fileName ?: "unnamed",
                            size = doc.document.size,
                            mimeType = doc.mimeType,
                            extension = (doc.fileName ?: "").substringAfterLast('.', ""),
                            telegramMsgId = msg.id,
                            telegramFileId = doc.document.remote.id,
                            folderId = localFolderId,
                            uploadDate = msg.date.toLong() * 1000
                        )
                    )
                } else if (existingFile.folderId != localFolderId) {
                    fileDao.updateFile(existingFile.copy(folderId = localFolderId))
                }
            }
        }
    }
}
