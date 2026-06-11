package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["telegramMsgId"], unique = true),
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
    val name: String,
    val size: Long,
    val mimeType: String?,
    val extension: String?,
    val telegramMsgId: Long,
    val telegramFileId: String,
    val folderId: Long?,
    val uploadDate: Long,
    val isSynced: Boolean = true,
    val thumbnailPath: String? = null,
    val syncState: String = "SYNCED", // SYNCED, PENDING, ERROR
    val metadataVersion: Int = 1,
    val pendingOperation: String? = null, // UPLOAD, DELETE, MOVE, RENAME
    val isDirty: Boolean = false
)
