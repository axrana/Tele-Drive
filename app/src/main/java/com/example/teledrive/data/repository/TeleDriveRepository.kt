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
    // User Session
    fun getUserSession(): Flow<UserSession?> = userSessionDao.getUserSession()
    suspend fun saveSession(session: UserSession) = userSessionDao.insertSession(session)
    suspend fun clearSession() = userSessionDao.clearSession()

    // Folders
    fun getFolders(parentId: Long?): Flow<List<Folder>> = folderDao.getFoldersInParent(parentId)
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()
    suspend fun createFolder(name: String, parentId: Long?, threadId: Long, parentThreadId: Long? = null): Long {
        val folder = Folder(name = name, parentFolderId = parentId, telegramThreadMsgId = threadId, telegramParentThreadId = parentThreadId, createdDate = System.currentTimeMillis())
        return folderDao.createFolder(folder)
    }
    suspend fun getFolderById(id: Long) = folderDao.getFolderById(id)
    suspend fun renameFolder(id: Long, newName: String) = folderDao.renameFolder(id, newName)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    // Files
    fun getFiles(folderId: Long?): Flow<List<FileEntity>> = fileDao.getFilesInFolder(folderId)
    fun searchFiles(query: String): Flow<List<FileEntity>> = fileDao.searchFiles(query)
    suspend fun createFile(file: FileEntity) = fileDao.createFile(file)
    suspend fun getFileById(id: Long) = fileDao.getFileById(id)
    suspend fun getFileByTelegramFileId(telegramFileId: String) = fileDao.getFileByTelegramFileId(telegramFileId)
    suspend fun updateFile(file: FileEntity) = fileDao.updateFile(file)
    suspend fun renameFile(id: Long, newName: String) = fileDao.renameFile(id, newName)
    suspend fun moveFile(id: Long, folderId: Long?) = fileDao.moveFile(id, folderId)

    suspend fun updateFileInTelegram(tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager, chatId: Long, file: FileEntity, newName: String? = null, newFolderId: Long? = null) {
        val folder = (newFolderId ?: file.folderId)?.let { folderDao.getFolderById(it) }
        val folderKey = folder?.telegramThreadMsgId
        val name = newName ?: file.name
        val caption = MetadataHelper.formatFileCaption(folderKey, name)

        tdLibraryManager.execute<TdApi.Message>(TdApi.EditMessageCaption(
            chatId,
            file.telegramMsgId,
            null,
            TdApi.FormattedText(caption, null),
            false
        ))
    }

    suspend fun updateFolderInTelegram(tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager, chatId: Long, folder: Folder, newName: String? = null, newParentId: Long? = null) {
        val name = newName ?: folder.name
        val parentFolder = (newParentId ?: folder.parentFolderId)?.let { folderDao.getFolderById(it) }
        val parentKey = parentFolder?.telegramThreadMsgId

        val meta = MetadataHelper.formatFolderMetadata(name, parentKey)
        try {
            tdLibraryManager.execute<TdApi.Message>(TdApi.EditMessageText(
                chatId,
                folder.telegramThreadMsgId,
                null,
                TdApi.InputMessageText(TdApi.FormattedText(meta, null), null, false)
            ))
        } catch (e: Exception) {
            // Revert to something safer or fix based on error
        }
    }
    suspend fun deleteFile(file: FileEntity) = fileDao.deleteFile(file)
    fun getTotalStorageUsed(): Flow<Long?> = fileDao.getTotalStorageUsed()
    fun getFileCount(): Flow<Int> = fileDao.getFileCount()
    fun getFolderCount(): Flow<Int> = folderDao.getFolderCount()

    // Telegram-side delete
    suspend fun deleteFileFromTelegram(tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager, chatId: Long, messageId: Long) {
        if (messageId != 0L) {
            tdLibraryManager.execute(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true))
        }
    }

    suspend fun deleteFolderWithContents(tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager, chatId: Long, folder: Folder) {
        val filesInFolder = fileDao.getFilesInFolderSync(folder.id)
        val messageIds = filesInFolder.map { it.telegramMsgId }.filter { it != 0L }.toLongArray()
        if (messageIds.isNotEmpty()) {
            tdLibraryManager.execute(TdApi.DeleteMessages(chatId, messageIds, true))
        }
        fileDao.deleteFilesInFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    // Sharing
    suspend fun createShareToken(fileId: Long, token: String, userId: Long, password: String? = null): Long {
        val shareToken = ShareToken(fileId = fileId, token = token, userId = userId, password = password, createdAt = System.currentTimeMillis(), expiresAt = null)
        return shareTokenDao.createShareToken(shareToken)
    }
    suspend fun getShareToken(token: String) = shareTokenDao.getShareToken(token)

    // Settings
    fun getSettings(): Flow<com.example.teledrive.data.local.entity.Settings?> = settingsDao.getSettings()
    suspend fun saveSettings(settings: com.example.teledrive.data.local.entity.Settings) = settingsDao.saveSettings(settings)

    // Transfers
    fun getAllTransfers() = transferDao.getAllTransfers()
    fun getActiveTransfers() = transferDao.getActiveTransfers()
    suspend fun insertTransfer(transfer: com.example.teledrive.data.local.entity.TransferEntity) = transferDao.insertTransfer(transfer)
    suspend fun updateTransfer(transfer: com.example.teledrive.data.local.entity.TransferEntity) = transferDao.updateTransfer(transfer)
    suspend fun updateTransferProgress(id: Long, status: com.example.teledrive.data.local.entity.TransferStatus, progress: Float, transferredSize: Long) = transferDao.updateProgress(id, status, progress, transferredSize)
    suspend fun deleteTransfer(id: Long) = transferDao.deleteTransfer(id)
    suspend fun clearFinishedTransfers() = transferDao.clearFinishedTransfers()
    suspend fun getTransferById(id: Long) = transferDao.getTransferById(id)

    suspend fun syncFromTelegram(tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager, chatId: Long) {
        val limit = 100
        val allMessages = mutableListOf<TdApi.Message>()
        var lastMessageId = 0L

        // 1. Fetch all messages
        while (true) {
            val messages = tdLibraryManager.execute(TdApi.GetChatHistory(chatId, lastMessageId, 0, limit, false))
            if (messages.messages.isEmpty()) break
            allMessages.addAll(messages.messages)
            lastMessageId = messages.messages.last().id
            if (messages.messages.size < limit) break
        }

        // 2. Fetch Forum Topics
        try {
            val topics = tdLibraryManager.execute(TdApi.GetForumTopics(chatId, "", 0, 0, 0, 100))
            topics.topics.forEach { topic ->
                val existing = folderDao.getFolderByThreadId(topic.info.forumTopicId.toLong())
                if (existing == null) {
                    folderDao.createFolder(Folder(
                        name = topic.info.name,
                        telegramThreadMsgId = topic.info.forumTopicId.toLong(),
                        createdDate = System.currentTimeMillis()
                    ))
                } else if (existing.name != topic.info.name) {
                    folderDao.updateFolder(existing.copy(name = topic.info.name))
                }
            }
        } catch (e: Exception) {}

        // 3. Process Text Markers for Folders
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageText) {
                val text = (msg.content as TdApi.MessageText).text.text
                MetadataHelper.parseFolderMetadata(text)?.let { meta ->
                    val existing = folderDao.getFolderByThreadId(msg.id)
                    if (existing == null) {
                        folderDao.createFolder(Folder(
                            name = meta.name,
                            telegramThreadMsgId = msg.id,
                            telegramParentThreadId = meta.parentId,
                            createdDate = msg.date.toLong() * 1000
                        ))
                    } else {
                        folderDao.updateFolder(existing.copy(
                            name = meta.name,
                            telegramParentThreadId = meta.parentId
                        ))
                    }
                }
            }
        }

        // 4. Rebuild Hierarchy (Idempotent)
        val allLocalFolders = folderDao.getAllFoldersSync()
        allLocalFolders.forEach { folder ->
            val parentThreadId = folder.telegramParentThreadId
            if (parentThreadId != null) {
                val parent = folderDao.getFolderByThreadId(parentThreadId)
                if (parent != null && folder.parentFolderId != parent.id) {
                    folderDao.updateParentId(folder.id, parent.id)
                }
            } else if (folder.parentFolderId != null && folder.name != "Recovered") {
                // If it should be root but isn't
                folderDao.updateParentId(folder.id, null)
            }
        }

        // 5. Sync Files
        var recoveredFolderId: Long? = null

        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageDocument) {
                val docContent = msg.content as TdApi.MessageDocument
                val doc = docContent.document
                val caption = (docContent.caption as? TdApi.FormattedText)?.text ?: ""

                val telegramFolderId = MetadataHelper.parseFileFolderId(caption)
                var localFolderId = telegramFolderId?.let { folderDao.getFolderByThreadId(it)?.id }

                if (telegramFolderId != null && localFolderId == null) {
                    if (recoveredFolderId == null) {
                        val existingRecovered = folderDao.getAllFoldersSync().find { it.name == "Recovered" && it.parentFolderId == null }
                        recoveredFolderId = existingRecovered?.id ?: folderDao.createFolder(Folder(
                            name = "Recovered",
                            telegramThreadMsgId = -1L,
                            createdDate = System.currentTimeMillis()
                        ))
                    }
                    localFolderId = recoveredFolderId
                }

                val existingFile = fileDao.getFileByTelegramMsgId(msg.id)
                if (existingFile == null) {
                    fileDao.createFile(FileEntity(
                        name = doc.fileName,
                        size = doc.document.size.toLong(),
                        mimeType = doc.mimeType,
                        extension = doc.fileName.substringAfterLast('.', ""),
                        telegramMsgId = msg.id,
                        telegramFileId = doc.document.remote.id,
                        folderId = localFolderId,
                        uploadDate = msg.date.toLong() * 1000
                    ))
                } else if (existingFile.folderId != localFolderId || existingFile.name != doc.fileName) {
                    fileDao.updateFile(existingFile.copy(
                        folderId = localFolderId,
                        name = doc.fileName
                    ))
                }
            }
        }
    }
}
