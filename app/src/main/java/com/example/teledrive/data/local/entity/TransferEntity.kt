package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // UPLOAD, DOWNLOAD
    val fileId: Long? = null,
    val folderId: Long? = null,
    val localPath: String,
    val remoteId: String? = null,
    val status: String, // PENDING, ACTIVE, COMPLETED, FAILED, CANCELLED
    val progress: Float = 0f,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val name: String,
    val size: Long,
    val mimeType: String? = null
)
