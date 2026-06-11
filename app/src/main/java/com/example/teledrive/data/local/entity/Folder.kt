package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["telegramThreadMsgId"], unique = true),
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
    val name: String,
    val parentFolderId: Long? = null,
    val telegramThreadMsgId: Long,
    val telegramParentThreadId: Long? = null,
    val createdDate: Long
)
