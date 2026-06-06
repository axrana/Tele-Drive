package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE folderId = :folderId")
    fun getFilesInFolder(folderId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createFile(file: FileEntity): Long

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Update
    suspend fun updateFile(file: FileEntity)
}
