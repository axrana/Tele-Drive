package com.example.teledrive.data.repository

import com.example.teledrive.data.local.dao.FileDao
import com.example.teledrive.data.local.dao.FolderDao
import com.example.teledrive.data.local.dao.ShareTokenDao
import com.example.teledrive.data.local.dao.UserSessionDao
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.data.local.entity.ShareToken
import com.example.teledrive.data.local.entity.UserSession
import kotlinx.coroutines.flow.Flow
import org.drinkless.tdlib.TdApi

class TeleDriveRepository(
    private val userSessionDao: UserSessionDao,
    private val folderDao: FolderDao,
    private val fileDao: FileDao,
    private val shareTokenDao: ShareTokenDao,
    private val settingsDao: com.example.teledrive.data.local.dao.SettingsDao
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

    suspend fun syncFromTelegram(tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager, chatId: Long) {
        val limit = 100
        val allMessages = mutableListOf<TdApi.Message>()
        var lastMessageId = 0L

        // 1. Fetch all messages first
        while (true) {
            val messages = tdLibraryManager.execute(TdApi.GetChatHistory(chatId, lastMessageId, 0, limit, false))
            if (messages.messages.isEmpty()) break
            allMessages.addAll(messages.messages)
            lastMessageId = messages.messages.last().id
            if (messages.messages.size < limit) break
        }

        // 2. First Pass: Sync all folders (Forum Topics + Text Markers)
        try {
            val topics = tdLibraryManager.execute(TdApi.GetForumTopics(chatId, "", 0, 0, 0, 100))
            topics.topics.forEach { topic ->
                folderDao.createFolder(Folder(
                    name = topic.info.name,
                    telegramThreadMsgId = topic.info.forumTopicId.toLong(),
                    createdDate = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) {}

        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageText) {
                val text = (msg.content as TdApi.MessageText).text.text
                if (text.startsWith("Folder:")) {
                    // Modern format: Folder:Name Parent:Key
                    val name = text.substringAfter("Folder:").substringBefore(" Parent:").trim()
                    val parentKey = if (text.contains(" Parent:")) text.substringAfter(" Parent:").toLongOrNull() else null
                    folderDao.createFolder(Folder(
                        name = name,
                        telegramThreadMsgId = msg.id,
                        telegramParentThreadId = parentKey,
                        createdDate = msg.date.toLong() * 1000
                    ))
                } else if (text.startsWith("Folder: ")) {
                    // Legacy format: Folder: Name
                    val name = text.removePrefix("Folder: ")
                    folderDao.createFolder(Folder(
                        name = name,
                        telegramThreadMsgId = msg.id,
                        createdDate = msg.date.toLong() * 1000
                    ))
                }
            }
        }

        // 3. Second Pass: Rebuild local hierarchy links
        val allLocalFolders = folderDao.getAllFoldersSync()
        allLocalFolders.forEach { folder ->
            if (folder.telegramParentThreadId != null) {
                val parent = folderDao.getFolderByThreadId(folder.telegramParentThreadId)
                if (parent != null) {
                    folderDao.updateParentId(folder.id, parent.id)
                }
            }
        }

        // 4. Create "Recovered" folder if not exists
        var recoveredFolderId: Long? = null
        val existingRecovered = allLocalFolders.find { it.name == "Recovered" && it.parentFolderId == null }
        if (existingRecovered != null) {
            recoveredFolderId = existingRecovered.id
        }

        // 5. Third Pass: Sync Files
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageDocument) {
                val docContent = msg.content as TdApi.MessageDocument
                val doc = docContent.document
                val caption = (docContent.caption as? TdApi.FormattedText)?.text ?: ""

                val telegramFolderId = if (caption.startsWith("TeleDriveFolder:")) {
                    caption.removePrefix("TeleDriveFolder:").substringBefore(" ").toLongOrNull()
                } else null

                var localFolderId = telegramFolderId?.let { folderDao.getFolderByThreadId(it)?.id }

                // If folder reference exists but cannot be found, move to Recovered
                if (telegramFolderId != null && localFolderId == null) {
                    if (recoveredFolderId == null) {
                        recoveredFolderId = folderDao.createFolder(Folder(
                            name = "Recovered",
                            telegramThreadMsgId = -1L,
                            createdDate = System.currentTimeMillis()
                        ))
                    }
                    localFolderId = recoveredFolderId
                }

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
            }
        }
    }
}
