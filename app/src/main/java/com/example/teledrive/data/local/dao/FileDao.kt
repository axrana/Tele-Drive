package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE folderId IS :folderId")
    fun getFilesInFolder(folderId: Long?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE telegramFileId = :telegramFileId LIMIT 1")
    suspend fun getFileByTelegramFileId(telegramFileId: String): FileEntity?

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%'")
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

    @Query("SELECT SUM(size) FROM files")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("DELETE FROM files WHERE folderId = :folderId")
    suspend fun deleteFilesInFolder(folderId: Long)

    @Query("SELECT COUNT(*) FROM files")
    fun getFileCount(): Flow<Int>

    @Query("SELECT * FROM files WHERE folderId = :folderId")
    suspend fun getFilesInFolderSync(folderId: Long?): List<FileEntity>
}
