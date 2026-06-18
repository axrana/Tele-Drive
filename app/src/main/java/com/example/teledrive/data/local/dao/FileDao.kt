package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE folderId IS :folderId AND isDeleted = 0")
    fun getFilesInFolder(folderId: Long?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE telegramMsgId = :msgId")
    suspend fun getFileByTelegramMsgId(msgId: Long): FileEntity?

    @Query("SELECT * FROM files WHERE telegramFileId = :telegramFileId LIMIT 1")
    suspend fun getFileByTelegramFileId(telegramFileId: String): FileEntity?

    @Query("SELECT * FROM files WHERE fileUuid = :uuid")
    suspend fun getFileByUuid(uuid: String): FileEntity?

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' AND isDeleted = 0")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createFile(file: FileEntity): Long

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("UPDATE files SET name = :newName WHERE id = :fileId")
    suspend fun renameFile(fileId: Long, newName: String)

    @Query("UPDATE files SET folderId = :folderId WHERE id = :fileId")
    suspend fun moveFile(fileId: Long, folderId: Long?)

    @Query("SELECT SUM(size) FROM files WHERE isDeleted = 0")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("DELETE FROM files WHERE folderId = :folderId")
    suspend fun deleteFilesInFolder(folderId: Long)

    @Query("SELECT COUNT(*) FROM files WHERE isDeleted = 0")
    fun getFileCount(): Flow<Int>

    @Query("SELECT * FROM files WHERE folderId = :folderId")
    suspend fun getFilesInFolderSync(folderId: Long?): List<FileEntity>

    @Query("SELECT COUNT(*) FROM files WHERE storageMessageId = :storageMessageId AND isDeleted = 0 AND id != :excludeId")
    suspend fun countFilesWithStorageMessage(storageMessageId: Long, excludeId: Long): Int

    @Query("DELETE FROM files")
    suspend fun clearAll()
}
