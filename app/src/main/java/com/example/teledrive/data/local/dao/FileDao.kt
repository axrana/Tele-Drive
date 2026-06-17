package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE folderId IS :folderId AND isDeleted = 0")
    fun getFilesInFolder(folderId: Long?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE folderId IS :folderId AND isDeleted = 0")
    suspend fun getFilesInFolderSync(folderId: Long?): List<FileEntity>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' AND isDeleted = 0")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createFile(file: FileEntity): Long

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE fileUuid = :uuid")
    suspend fun getFileByUuid(uuid: String): FileEntity?

    @Query("SELECT * FROM files WHERE telegramFileId = :telegramFileId")
    suspend fun getFileByTelegramFileId(telegramFileId: String): FileEntity?

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("DELETE FROM files WHERE folderId = :folderId")
    suspend fun deleteFilesInFolder(folderId: Long)

    @Query("SELECT SUM(size) FROM files WHERE isDeleted = 0")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM files WHERE isDeleted = 0")
    fun getFileCount(): Flow<Int>

    @Query("DELETE FROM files")
    suspend fun clearAll()

    @Query("SELECT * FROM files WHERE storageMessageId = :msgId AND isDeleted = 0")
    suspend fun getFilesByStorageMessageId(msgId: Long): List<FileEntity>

    @Query("SELECT COUNT(*) FROM files WHERE storageMessageId = :msgId AND isDeleted = 0")
    suspend fun countFilesWithStorageMessage(msgId: Long): Int
}
