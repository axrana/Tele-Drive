package com.example.teledrive.data.repository

import com.example.teledrive.data.local.dao.*
import com.example.teledrive.data.local.entity.*
import com.example.teledrive.domain.repository.TeleDriveRepository
import com.example.teledrive.telegram.TdLibraryManager
import kotlinx.coroutines.flow.Flow
import org.drinkless.tdlib.TdApi

class TeleDriveRepositoryImpl(
    private val userSessionDao: UserSessionDao,
    private val folderDao: FolderDao,
    private val fileDao: FileDao,
    private val shareTokenDao: ShareTokenDao,
    private val settingsDao: SettingsDao,
    private val transferDao: TransferDao
) : TeleDriveRepository {

    override fun getUserSession(): Flow<UserSession?> = userSessionDao.getUserSession()
    override suspend fun saveSession(session: UserSession) = userSessionDao.insertSession(session)
    override suspend fun clearSession() = userSessionDao.clearSession()

    override fun getFolders(parentId: Long?): Flow<List<Folder>> = folderDao.getFoldersInParent(parentId)
    override fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()
    override suspend fun createFolder(name: String, parentId: Long?, threadId: Long, parentThreadId: Long?): Long {
        val folder = Folder(
            name = name,
            parentFolderId = parentId,
            telegramThreadMsgId = threadId,
            telegramParentThreadId = parentThreadId,
            createdDate = System.currentTimeMillis()
        )
        return folderDao.createFolder(folder)
    }
    override suspend fun getFolderById(id: Long) = folderDao.getFolderById(id)
    override suspend fun renameFolder(id: Long, newName: String) = folderDao.renameFolder(id, newName)
    override suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)
    override fun getFolderCount(): Flow<Int> = folderDao.getFolderCount()

    override fun getFiles(folderId: Long?): Flow<List<FileEntity>> = fileDao.getFilesInFolder(folderId)
    override fun searchFiles(query: String): Flow<List<FileEntity>> = fileDao.searchFiles(query)
    override suspend fun createFile(file: FileEntity) = fileDao.createFile(file)
    override suspend fun getFileById(id: Long) = fileDao.getFileById(id)
    override suspend fun getFileByTelegramFileId(telegramFileId: String) = fileDao.getFileByTelegramFileId(telegramFileId)
    override suspend fun updateFile(file: FileEntity) = fileDao.updateFile(file)
    override suspend fun renameFile(id: Long, newName: String) = fileDao.renameFile(id, newName)
    override suspend fun moveFile(id: Long, folderId: Long?) = fileDao.moveFile(id, folderId)
    override suspend fun deleteFile(file: FileEntity) = fileDao.deleteFile(file)
    override fun getTotalStorageUsed(): Flow<Long?> = fileDao.getTotalStorageUsed()
    override fun getFileCount(): Flow<Int> = fileDao.getFileCount()

    override suspend fun deleteFileFromTelegram(tdLibraryManager: TdLibraryManager, chatId: Long, messageId: Long) {
        if (messageId != 0L) {
            tdLibraryManager.execute(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true))
        }
    }

    override suspend fun deleteFolderWithContents(tdLibraryManager: TdLibraryManager, chatId: Long, folder: Folder) {
        val filesInFolder = fileDao.getFilesInFolderSync(folder.id)
        val messageIds = filesInFolder.map { it.telegramMsgId }.filter { it != 0L }.toLongArray()
        if (messageIds.isNotEmpty()) {
            tdLibraryManager.execute(TdApi.DeleteMessages(chatId, messageIds, true))
        }
        fileDao.deleteFilesInFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    override suspend fun createShareToken(fileId: Long, token: String, userId: Long, password: String?): Long {
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
    override suspend fun getShareToken(token: String) = shareTokenDao.getShareToken(token)

    override fun getSettings(): Flow<Settings?> = settingsDao.getSettings()
    override suspend fun saveSettings(settings: Settings) = settingsDao.saveSettings(settings)

    override fun getAllTransfers(): Flow<List<TransferEntity>> = transferDao.getAllTransfers()
    override suspend fun getTransferById(id: Long): TransferEntity? = transferDao.getTransferById(id)
    override suspend fun insertTransfer(transfer: TransferEntity): Long = transferDao.insertTransfer(transfer)
    override suspend fun updateTransfer(transfer: TransferEntity) = transferDao.updateTransfer(transfer)
    override suspend fun deleteTransfer(transfer: TransferEntity) = transferDao.deleteTransfer(transfer)
    override suspend fun clearFinishedTransfers() = transferDao.clearFinishedTransfers()

    private suspend fun ensureRecoveredFolder(orphan: Folder) {
        val allFolders = folderDao.getAllFoldersSync()
        var recovered = allFolders.find { it.name == "Recovered" && it.parentFolderId == null }
        if (recovered == null) {
            val id = folderDao.createFolder(Folder(
                name = "Recovered",
                telegramThreadMsgId = -1L, // Synthetic ID
                createdDate = System.currentTimeMillis()
            ))
            recovered = folderDao.getFolderById(id)
        }
        if (recovered != null && orphan.id != recovered.id) {
            folderDao.updateParentId(orphan.id, recovered.id)
        }
    }

    override suspend fun syncFromTelegram(tdLibraryManager: TdLibraryManager, chatId: Long) {
        val limit = 100
        val allMessages = mutableListOf<TdApi.Message>()
        var lastMessageId = 0L

        // Pass 1: Fetch all chat history
        while (true) {
            val messages = tdLibraryManager.execute(TdApi.GetChatHistory(chatId, lastMessageId, 0, limit, false))
            if (messages.messages.isEmpty()) break
            allMessages.addAll(messages.messages)
            lastMessageId = messages.messages.last().id
            if (messages.messages.size < limit) break
        }

        // Pass 2: Extract metadata and topics
        val discoveredFolders = mutableListOf<Folder>()
        val discoveredFiles = mutableListOf<FileEntity>()

        // Get Forum Topics
        try {
            val topics = tdLibraryManager.execute(TdApi.GetForumTopics(chatId, "", 0, 0, 0, 100))
            topics.topics.forEach { topic ->
                discoveredFolders.add(Folder(
                    name = topic.info.name,
                    telegramThreadMsgId = topic.info.forumTopicId.toLong(),
                    createdDate = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) {}

        // Parse message metadata
        allMessages.forEach { msg ->
            when (val content = msg.content) {
                is TdApi.MessageText -> {
                    val metadata = com.example.teledrive.telegram.MetadataHelper.parseFolderMetadata(content.text.text)
                    if (metadata != null) {
                        discoveredFolders.add(Folder(
                            name = metadata.name,
                            telegramThreadMsgId = msg.id,
                            telegramParentThreadId = metadata.parentId,
                            createdDate = msg.date.toLong() * 1000
                        ))
                    } else if (content.text.text.startsWith("Folder:")) {
                        // Compatibility for old format
                        val name = content.text.text.substringAfter("Folder:").substringBefore(" Parent:").trim()
                        val parentKey = if (content.text.text.contains(" Parent:")) content.text.text.substringAfter(" Parent:").toLongOrNull() else null
                        discoveredFolders.add(Folder(
                            name = name,
                            telegramThreadMsgId = msg.id,
                            telegramParentThreadId = parentKey,
                            createdDate = msg.date.toLong() * 1000
                        ))
                    }
                }
                is TdApi.MessageDocument -> {
                    val caption = (content.caption as? TdApi.FormattedText)?.text ?: ""
                    val metadata = com.example.teledrive.telegram.MetadataHelper.parseFileMetadata(caption)
                    val folderId = metadata?.folderId ?: if (caption.startsWith("TeleDriveFolder:")) {
                        caption.removePrefix("TeleDriveFolder:").substringBefore(" ").toLongOrNull()
                    } else null

                    discoveredFiles.add(FileEntity(
                        name = metadata?.name ?: content.document.fileName,
                        size = content.document.document.size.toLong(),
                        mimeType = content.document.mimeType,
                        extension = content.document.fileName.substringAfterLast('.', ""),
                        telegramMsgId = msg.id,
                        telegramFileId = content.document.document.remote.id,
                        folderId = null, // Will be linked in Pass 4
                        uploadDate = msg.date.toLong() * 1000,
                        syncState = "SYNCED"
                    ).also {
                        // Temporary storage of remote folder ID in an unused field or separate map
                        // For simplicity in this implementation, we'll re-link by searching the DB
                    })
                }
            }
        }

        // Pass 3: Reconstruct Hierarchy
        val refinedFolders = com.example.teledrive.util.TreeBuilder.reconstructHierarchy(discoveredFolders)

        // Save folders to DB (deduplicate by telegramThreadMsgId)
        refinedFolders.forEach { folder ->
            val existing = folderDao.getFolderByThreadId(folder.telegramThreadMsgId)
            if (existing == null) {
                folderDao.createFolder(folder)
            } else {
                folderDao.updateFolder(existing.copy(
                    name = folder.name,
                    telegramParentThreadId = folder.telegramParentThreadId
                ))
            }
        }

        // Re-link parentFolderId based on telegramParentThreadId
        val allFoldersAfterSync = folderDao.getAllFoldersSync()
        allFoldersAfterSync.forEach { folder ->
            if (folder.telegramParentThreadId != null) {
                val parent = allFoldersAfterSync.find { it.telegramThreadMsgId == folder.telegramParentThreadId }
                if (parent != null) {
                    folderDao.updateParentId(folder.id, parent.id)
                } else {
                    // Orphaned folder: parent exists in metadata but not in current chat view
                    // Place it in "Recovered" root folder
                    ensureRecoveredFolder(folder)
                }
            }
        }

        // Pass 4: Reconcile Files
        discoveredFiles.forEach { file ->
            val existing = fileDao.getFileByMsgId(file.telegramMsgId)

            // Re-discover folder ID
            val msg = allMessages.find { it.id == file.telegramMsgId }
            val caption = ((msg?.content as? TdApi.MessageDocument)?.caption as? TdApi.FormattedText)?.text ?: ""
            val metadata = com.example.teledrive.telegram.MetadataHelper.parseFileMetadata(caption)
            val telegramFolderThreadId = metadata?.folderId ?: if (caption.startsWith("TeleDriveFolder:")) {
                caption.removePrefix("TeleDriveFolder:").substringBefore(" ").toLongOrNull()
            } else null

            val localFolderId = telegramFolderThreadId?.let { folderDao.getFolderByThreadId(it)?.id }

            if (existing == null) {
                fileDao.createFile(file.copy(folderId = localFolderId))
            } else {
                fileDao.updateFile(existing.copy(
                    name = file.name,
                    folderId = localFolderId,
                    telegramFileId = file.telegramFileId
                ))
            }
        }
    }
}
