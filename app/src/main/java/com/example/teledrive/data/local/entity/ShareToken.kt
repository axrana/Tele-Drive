package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "share_tokens",
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
    val token: String, // UUID
    val userId: Long,
    val password: String?,
    val expiresAt: Long?,
    val createdAt: Long
)
