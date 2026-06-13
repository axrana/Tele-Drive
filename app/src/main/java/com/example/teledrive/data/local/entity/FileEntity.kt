package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["fileUuid"], unique = true),
        Index(value = ["telegramMsgId"]),
        Index(value = ["telegramFileId"]),
        Index(value = ["folderId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileUuid: String,
    val name: String,
    val displayName: String,
    val size: Long,
    val mimeType: String?,
    val extension: String?,
    val telegramMsgId: Long, // Deprecated, mapping to storageMessageId
    val storageMessageId: Long,
    val telegramFileId: String,
    val folderId: Long?,
    val uploadDate: Long,
    val version: Long = 1,
    val isDeleted: Boolean = false,
    val sourceFileUuid: String? = null,
    val isSynced: Boolean = true,
    val thumbnailPath: String? = null,
    val syncState: Int = 0,
    val isDirty: Boolean = false,
    val localPath: String? = null
)
