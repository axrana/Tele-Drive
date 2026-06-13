package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["folderUuid"], unique = true),
        Index(value = ["telegramThreadMsgId"]),
        Index(value = ["parentFolderId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderUuid: String,
    val name: String,
    val parentFolderId: Long? = null,
    val telegramThreadMsgId: Long = 0L,
    val telegramParentThreadId: Long? = null,
    val createdDate: Long,
    val version: Long = 1,
    val isDeleted: Boolean = false,
    val syncState: Int = 0,
    val isDirty: Boolean = false,
    val metadataVersion: Int = 1
)
