package com.example.teledrive.domain.repository

import com.example.teledrive.data.local.entity.*
import com.example.teledrive.telegram.TdLibraryManager
import kotlinx.coroutines.flow.Flow

interface TeleDriveRepository {
    // User Session
    fun getUserSession(): Flow<UserSession?>
    suspend fun saveSession(session: UserSession)
    suspend fun clearSession()

    // Folders
    fun getFolders(parentId: Long?): Flow<List<Folder>>
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun createFolder(name: String, parentId: Long?, threadId: Long, parentThreadId: Long? = null): Long
    suspend fun getFolderById(id: Long): Folder?
    suspend fun renameFolder(id: Long, newName: String)
    suspend fun deleteFolder(folder: Folder)
    fun getFolderCount(): Flow<Int>

    // Files
    fun getFiles(folderId: Long?): Flow<List<FileEntity>>
    fun searchFiles(query: String): Flow<List<FileEntity>>
    suspend fun createFile(file: FileEntity): Long
    suspend fun getFileById(id: Long): FileEntity?
    suspend fun getFileByTelegramFileId(telegramFileId: String): FileEntity?
    suspend fun updateFile(file: FileEntity)
    suspend fun renameFile(id: Long, newName: String)
    suspend fun moveFile(id: Long, folderId: Long?)
    suspend fun deleteFile(file: FileEntity)
    fun getTotalStorageUsed(): Flow<Long?>
    fun getFileCount(): Flow<Int>

    // Telegram Sync & Actions
    suspend fun deleteFileFromTelegram(tdLibraryManager: TdLibraryManager, chatId: Long, messageId: Long)
    suspend fun deleteFolderWithContents(tdLibraryManager: TdLibraryManager, chatId: Long, folder: Folder)
    suspend fun syncFromTelegram(tdLibraryManager: TdLibraryManager, chatId: Long)

    // Sharing
    suspend fun createShareToken(fileId: Long, token: String, userId: Long, password: String? = null): Long
    suspend fun getShareToken(token: String): ShareToken?

    // Settings
    fun getSettings(): Flow<Settings?>
    suspend fun saveSettings(settings: Settings)

    // Transfers
    fun getAllTransfers(): Flow<List<TransferEntity>>
    suspend fun getTransferById(id: Long): TransferEntity?
    suspend fun insertTransfer(transfer: TransferEntity): Long
    suspend fun updateTransfer(transfer: TransferEntity)
    suspend fun deleteTransfer(transfer: TransferEntity)
    suspend fun clearFinishedTransfers()
}
