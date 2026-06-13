package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    suspend fun getAllFoldersSync(): List<Folder>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?

    @Query("SELECT * FROM folders WHERE telegramThreadMsgId = :msgId")
    suspend fun getFolderByThreadId(msgId: Long): Folder?

    @Query("SELECT * FROM folders WHERE folderUuid = :uuid")
    suspend fun getFolderByUuid(uuid: String): Folder?

    @Query("SELECT * FROM folders WHERE parentFolderId IS :parentId AND isDeleted = 0")
    fun getFoldersInParent(parentId: Long?): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Query("UPDATE folders SET name = :newName WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, newName: String)

    @Query("UPDATE folders SET parentFolderId = :parentId WHERE id = :id")
    suspend fun updateParentId(id: Long, parentId: Long?)

    @Query("SELECT COUNT(*) FROM folders WHERE isDeleted = 0")
    fun getFolderCount(): Flow<Int>

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}
