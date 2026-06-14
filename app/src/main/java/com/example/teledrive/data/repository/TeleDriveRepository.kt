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
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.drinkless.tdlib.TdApi

class TeleDriveRepository(
    private val userSessionDao: UserSessionDao,
    private val folderDao: FolderDao,
    private val fileDao: FileDao,
    private val shareTokenDao: ShareTokenDao,
    private val settingsDao: com.example.teledrive.data.local.dao.SettingsDao,
    private val transferDao: com.example.teledrive.data.local.dao.TransferDao,
    private val journalEventDao: com.example.teledrive.data.local.dao.JournalEventDao
) {
    fun getUserSession(): Flow<UserSession?> = userSessionDao.getUserSession()
    suspend fun saveSession(session: UserSession) = userSessionDao.insertSession(session)
    suspend fun clearSession() = userSessionDao.clearSession()

    fun getFolders(parentId: Long?): Flow<List<Folder>> = folderDao.getFoldersInParent(parentId)
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()
    suspend fun createFolder(name: String, parentId: Long?, threadId: Long, parentThreadId: Long? = null): Long {
        val folder = Folder(
            folderUuid = java.util.UUID.randomUUID().toString(),
            name = name,
            parentFolderId = parentId,
            telegramThreadMsgId = threadId,
            telegramParentThreadId = parentThreadId,
            createdDate = System.currentTimeMillis()
        )
        return folderDao.createFolder(folder)
    }
    suspend fun getFolderById(id: Long) = folderDao.getFolderById(id)
    suspend fun getFolderByUuid(uuid: String) = folderDao.getFolderByUuid(uuid)
    suspend fun renameFolder(folder: Folder) = folderDao.updateFolder(folder)
    suspend fun moveFolder(folder: Folder) = folderDao.updateFolder(folder)
    suspend fun deleteFolder(folder: Folder) = folderDao.updateFolder(folder)

    fun getFiles(folderId: Long?): Flow<List<FileEntity>> = fileDao.getFilesInFolder(folderId)
    fun searchFiles(query: String): Flow<List<FileEntity>> = fileDao.searchFiles(query)
    suspend fun createFile(file: FileEntity) = fileDao.createFile(file)
    suspend fun getFileById(id: Long) = fileDao.getFileById(id)
    suspend fun getFileByUuid(uuid: String) = fileDao.getFileByUuid(uuid)
    suspend fun getFileByTelegramFileId(telegramFileId: String) = fileDao.getFileByTelegramFileId(telegramFileId)
    suspend fun updateFile(file: FileEntity) = fileDao.updateFile(file)
    suspend fun renameFile(file: FileEntity) = fileDao.updateFile(file)
    suspend fun moveFile(file: FileEntity) = fileDao.updateFile(file)

    suspend fun copyFile(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        session: UserSession,
        file: FileEntity,
        targetFolderId: Long?
    ) {
        val targetFolder = targetFolderId?.let { folderDao.getFolderById(it) }
        val newFileUuid = java.util.UUID.randomUUID().toString()

        // Append COPY_FILE event
        appendJournalEvent(
            tdLibraryManager = tdLibraryManager,
            journalChannelId = session.journalChannelId,
            op = "COPY_FILE",
            objectType = "file",
            objectId = newFileUuid,
            version = 1,
            payload = mapOf(
                "sourceFileUuid" to file.fileUuid,
                "name" to file.name,
                "size" to file.size,
                "storageMessageId" to file.storageMessageId,
                "toFolderUuid" to targetFolder?.folderUuid
            )
        )

        // For now, we create a local entry that points to the same storage message.
        // In a full implementation, we might want to resend the message in Telegram to ensure durability.
        val copy = file.copy(
            id = 0,
            fileUuid = newFileUuid,
            folderId = targetFolderId,
            sourceFileUuid = file.fileUuid,
            uploadDate = System.currentTimeMillis(),
            version = 1
        )
        fileDao.createFile(copy)
    }
    suspend fun deleteFile(file: FileEntity) = fileDao.updateFile(file)
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

    suspend fun appendJournalEvent(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        journalChannelId: Long,
        op: String,
        objectType: String,
        objectId: String,
        version: Long,
        payload: Map<String, Any?>
    ): Long {
        val eventUuid = java.util.UUID.randomUUID().toString()
        val text = MetadataHelper.formatJournalEvent(
            eventId = eventUuid,
            op = op,
            objectType = objectType,
            objectId = objectId,
            version = version,
            payload = payload
        )

        val formattedText = TdApi.FormattedText()
        formattedText.text = text
        val content = TdApi.InputMessageText()
        content.text = formattedText

        val query = TdApi.SendMessage()
        query.chatId = journalChannelId
        query.inputMessageContent = content

        val message = tdLibraryManager.execute(query)

        val jsonObj = JSONObject()
        payload.forEach { (key, value) ->
            when (value) {
                null -> jsonObj.put(key, JSONObject.NULL)
                is String -> jsonObj.put(key, value)
                is Long -> jsonObj.put(key, value)
                is Int -> jsonObj.put(key, value)
                is Boolean -> jsonObj.put(key, value)
                is Double -> jsonObj.put(key, value)
                else -> jsonObj.put(key, value.toString())
            }
        }

        val journalEvent = com.example.teledrive.data.local.entity.JournalEvent(
            eventUuid = eventUuid,
            objectType = objectType,
            objectUuid = objectId,
            op = op,
            version = version,
            ts = System.currentTimeMillis(),
            telegramJournalMessageId = message.id,
            payloadJson = jsonObj.toString()
        )
        journalEventDao.insertEvent(journalEvent)
        return message.id
    }

    suspend fun syncFromJournalAndStorage(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        session: UserSession
    ) {
        // 1. Fetch journal events
        val journalMessages = fetchAllMessages(tdLibraryManager, session.journalChannelId)
        journalMessages.forEach { msg ->
            if (msg.content is TdApi.MessageText) {
                val text = (msg.content as TdApi.MessageText).text.text
                MetadataHelper.parseJournalEvent(text)?.let { parsed ->
                    val event = com.example.teledrive.data.local.entity.JournalEvent(
                        eventUuid = parsed.eventId,
                        objectType = parsed.objectType,
                        objectUuid = parsed.objectId,
                        op = parsed.op,
                        version = parsed.version,
                        ts = parsed.ts,
                        telegramJournalMessageId = msg.id,
                        payloadJson = parsed.payload.toString()
                    )
                    journalEventDao.insertEvent(event)
                }
            }
        }

        // 2. Load storage messages
        val storageMessages = fetchAllMessages(tdLibraryManager, session.storageChannelId)
        val storageMap = storageMessages.filter { it.content is TdApi.MessageDocument }
            .associateBy { it.id }

        // 3. Rebuild state from journal events
        val allEvents = journalEventDao.getAllEventsSync()

        // Maps for canonical state
        val foldersMap = mutableMapOf<String, Folder>()
        val filesMap = mutableMapOf<String, FileEntity>()

        allEvents.forEach { event ->
            val payload = JSONObject(event.payloadJson)
            when (event.op) {
                "CREATE_FOLDER" -> {
                    foldersMap[event.objectUuid] = Folder(
                        folderUuid = event.objectUuid,
                        name = payload.optString("name", "Unnamed"),
                        parentFolderId = null,
                        createdDate = event.ts,
                        version = event.version,
                        telegramThreadMsgId = 0
                    )
                }
                "RENAME_FOLDER" -> {
                    foldersMap[event.objectUuid]?.let {
                        foldersMap[event.objectUuid] = it.copy(
                            name = payload.optString("name", it.name),
                            version = event.version
                        )
                    }
                }
                "MOVE_FOLDER" -> {
                    foldersMap[event.objectUuid]?.let {
                        foldersMap[event.objectUuid] = it.copy(
                            version = event.version
                        )
                    }
                }
                "DELETE_FOLDER" -> {
                    foldersMap[event.objectUuid]?.let {
                        foldersMap[event.objectUuid] = it.copy(isDeleted = true, version = event.version)
                    }
                }
                "CREATE_FILE" -> {
                    val storageMsgId = payload.optLong("storageMessageId", 0L)
                    val msg = storageMap[storageMsgId]
                    if (msg != null && msg.content is TdApi.MessageDocument) {
                        val doc = (msg.content as TdApi.MessageDocument).document
                        val name = payload.optString("name", "unnamed")
                        filesMap[event.objectUuid] = FileEntity(
                            fileUuid = event.objectUuid,
                            name = name,
                            displayName = name,
                            size = payload.optLong("size", 0L),
                            mimeType = payload.optString("mimeType"),
                            extension = name.substringAfterLast('.', ""),
                            telegramMsgId = storageMsgId,
                            storageMessageId = storageMsgId,
                            telegramFileId = doc.document.remote.id,
                            folderId = null,
                            uploadDate = event.ts,
                            version = event.version
                        )
                    }
                }
                "RENAME_FILE" -> {
                    filesMap[event.objectUuid]?.let {
                        val name = payload.optString("name", it.name)
                        filesMap[event.objectUuid] = it.copy(
                            name = name,
                            displayName = name,
                            version = event.version
                        )
                    }
                }
                "MOVE_FILE" -> {
                    filesMap[event.objectUuid]?.let {
                        filesMap[event.objectUuid] = it.copy(version = event.version)
                    }
                }
                "COPY_FILE" -> {
                    val storageMsgId = payload.optLong("storageMessageId", 0L)
                    val msg = storageMap[storageMsgId]
                    if (msg != null && msg.content is TdApi.MessageDocument) {
                        val doc = (msg.content as TdApi.MessageDocument).document
                        val name = payload.optString("name", "unnamed")
                        filesMap[event.objectUuid] = FileEntity(
                            fileUuid = event.objectUuid,
                            name = name,
                            displayName = name,
                            size = payload.optLong("size", 0L),
                            mimeType = doc.mimeType,
                            extension = name.substringAfterLast('.', ""),
                            telegramMsgId = storageMsgId,
                            storageMessageId = storageMsgId,
                            telegramFileId = doc.document.remote.id,
                            folderId = null,
                            uploadDate = event.ts,
                            version = event.version,
                            sourceFileUuid = payload.optString("sourceFileUuid", "").takeIf { it.isNotEmpty() }
                        )
                    }
                }
                "DELETE_FILE" -> {
                    filesMap[event.objectUuid]?.let {
                        filesMap[event.objectUuid] = it.copy(isDeleted = true, version = event.version)
                    }
                }
            }
        }

        // 4. Resolve hierarchies and Apply to DB (Upsert approach)
        val uuidToRoomId = mutableMapOf<String, Long>()
        foldersMap.values.forEach { folder ->
            val existing = folderDao.getFolderByUuid(folder.folderUuid)
            if (existing == null) {
                if (!folder.isDeleted) {
                    val id = folderDao.createFolder(folder)
                    uuidToRoomId[folder.folderUuid] = id
                }
            } else {
                folderDao.updateFolder(existing.copy(
                    name = folder.name,
                    isDeleted = folder.isDeleted,
                    version = folder.version
                ))
                uuidToRoomId[folder.folderUuid] = existing.id
            }
        }

        // Second pass: resolve parent relationships for folders
        allEvents.forEach { event ->
            if (event.op == "CREATE_FOLDER" || event.op == "MOVE_FOLDER") {
                val payload = JSONObject(event.payloadJson)
                val parentUuid = if (payload.isNull("parentId")) null else payload.optString("parentId", "")
                val targetUuid = if (payload.has("toFolderUuid")) payload.optString("toFolderUuid", "") else parentUuid

                if (targetUuid != null) {
                    val folderId = uuidToRoomId[event.objectUuid]
                    val parentId = uuidToRoomId[targetUuid]
                    if (folderId != null && parentId != null) {
                        folderDao.updateParentId(folderId, parentId)
                    }
                }
            }
        }

        // Resolve file folders and Insert/Update files
        filesMap.values.forEach { file ->
            // Find the latest folderUuid for this file from journal
            val latestFolderUuid = allEvents.filter { it.objectUuid == file.fileUuid && (it.op == "CREATE_FILE" || it.op == "MOVE_FILE") }
                .lastOrNull()?.let { ev ->
                    val p = JSONObject(ev.payloadJson)
                    val key = if (ev.op == "CREATE_FILE") "parentFolderUuid" else "toFolderUuid"
                    if (p.isNull(key)) null else p.optString(key, "")
                }

            // Special case for copied files
            val finalFolderUuid = latestFolderUuid ?: allEvents.filter { it.objectUuid == file.fileUuid && it.op == "COPY_FILE" }
                .lastOrNull()?.let { ev ->
                    val p = JSONObject(ev.payloadJson)
                    if (p.has("toFolderUuid") && !p.isNull("toFolderUuid")) p.optString("toFolderUuid", "") else null
                }

            val localFolderId = if (finalFolderUuid != null) uuidToRoomId[finalFolderUuid] else null
            val existing = fileDao.getFileByUuid(file.fileUuid)
            if (existing == null) {
                if (!file.isDeleted) {
                    fileDao.createFile(file.copy(folderId = localFolderId))
                }
            } else {
                fileDao.updateFile(existing.copy(
                    name = file.name,
                    displayName = file.displayName,
                    isDeleted = file.isDeleted,
                    folderId = localFolderId,
                    version = file.version
                ))
            }
        }

        // 5. Cleanup
        // Handle Recovered bucket for orphaned storage messages not in journal
        val storageInJournal = allEvents.filter { it.objectType == "file" && (it.op == "CREATE_FILE" || it.op == "COPY_FILE") }
            .map { JSONObject(it.payloadJson).optLong("storageMessageId", 0L) }
            .toSet()

        val orphans = storageMap.keys - storageInJournal
        if (orphans.isNotEmpty()) {
            val recoveredId = folderDao.createFolder(Folder(
                folderUuid = "recovered",
                name = "Recovered",
                createdDate = System.currentTimeMillis(),
                telegramThreadMsgId = -1
            ))
            orphans.forEach { msgId ->
                val msg = storageMap[msgId]!!
                val doc = (msg.content as TdApi.MessageDocument).document
                fileDao.createFile(FileEntity(
                    fileUuid = java.util.UUID.randomUUID().toString(),
                    name = doc.fileName ?: "orphan_$msgId",
                    displayName = doc.fileName ?: "orphan_$msgId",
                    size = doc.document.size,
                    mimeType = doc.mimeType,
                    extension = doc.fileName?.substringAfterLast('.', ""),
                    telegramMsgId = msgId,
                    storageMessageId = msgId,
                    telegramFileId = doc.document.remote.id,
                    folderId = recoveredId,
                    uploadDate = msg.date.toLong() * 1000
                ))
            }
        }
    }

    private suspend fun fetchAllMessages(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        chatId: Long
    ): List<TdApi.Message> {
        if (chatId == 0L) return emptyList()
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
        return allMessages
    }

    suspend fun syncFromTelegram(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        chatId: Long
    ) {
        val session = userSessionDao.getUserSession().firstOrNull() ?: return

        // If we have journal, use it.
        if (session.journalChannelId != 0L) {
            syncFromJournalAndStorage(tdLibraryManager, session)

            // One-time legacy import if journal is empty
            val eventCount = journalEventDao.getAllEventsSync().size
            if (eventCount == 0) {
                importLegacyData(tdLibraryManager, session)
                syncFromJournalAndStorage(tdLibraryManager, session)
            }
        }
    }

    private suspend fun importLegacyData(
        tdLibraryManager: com.example.teledrive.tdlib.TdLibraryManager,
        session: UserSession
    ) {
        com.example.teledrive.util.TeleDriveLogger.d("Import", "Starting legacy import from channel ${session.channelId}")
        val allMessages = fetchAllMessages(tdLibraryManager, session.channelId)

        // 1. Process Folders
        val oldThreadToNewUuid = mutableMapOf<Long, String>()
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageText) {
                val text = (msg.content as TdApi.MessageText).text.text
                MetadataHelper.parseFolderMetadata(text)?.let { meta ->
                    val newUuid = java.util.UUID.nameUUIDFromBytes("folder_${msg.id}".toByteArray()).toString()
                    oldThreadToNewUuid[msg.id] = newUuid
                    appendJournalEvent(
                        tdLibraryManager, session.journalChannelId,
                        op = "CREATE_FOLDER", objectType = "folder",
                        objectId = newUuid,
                        version = 1,
                        payload = mapOf("name" to meta.name, "parentId" to null)
                    )
                }
            }
        }

        // Second pass for folder hierarchy
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageText) {
                val text = (msg.content as TdApi.MessageText).text.text
                MetadataHelper.parseFolderMetadata(text)?.let { meta ->
                    if (meta.parentId != null && meta.parentId != 0L) {
                        val folderUuid = oldThreadToNewUuid[msg.id]
                        val parentUuid = oldThreadToNewUuid[meta.parentId]
                        if (folderUuid != null && parentUuid != null) {
                            appendJournalEvent(
                                tdLibraryManager, session.journalChannelId,
                                op = "MOVE_FOLDER", objectType = "folder",
                                objectId = folderUuid,
                                version = 2,
                                payload = mapOf("toFolderUuid" to parentUuid)
                            )
                        }
                    }
                }
            }
        }

        // 2. Process Files
        allMessages.forEach { msg ->
            if (msg.content is TdApi.MessageDocument) {
                val doc = (msg.content as TdApi.MessageDocument).document
                val caption = (msg.content as TdApi.MessageDocument).caption?.text ?: ""
                val oldParentThreadId = MetadataHelper.parseFileFolderId(caption)
                val parentUuid = oldParentThreadId?.let { oldThreadToNewUuid[it] }

                appendJournalEvent(
                    tdLibraryManager, session.journalChannelId,
                    op = "CREATE_FILE", objectType = "file",
                    objectId = java.util.UUID.nameUUIDFromBytes("file_${msg.id}".toByteArray()).toString(),
                    version = 1,
                    payload = mapOf(
                        "name" to (doc.fileName ?: "imported"),
                        "size" to doc.document.size,
                        "storageMessageId" to msg.id,
                        "parentFolderUuid" to parentUuid
                    )
                )
            }
        }
    }
}
