package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "share_tokens",
    indices = [Index(value = ["fileId"])],
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShareToken(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val token: String,
    val userId: Long,
    val password: String? = null,
    val expiresAt: Long? = null,
    val createdAt: Long
)
