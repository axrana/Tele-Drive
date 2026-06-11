package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransferType,
    val fileId: Long? = null,
    val fileName: String,
    val localPath: String,
    val remoteId: String? = null,
    val folderId: Long? = null,
    val status: TransferStatus = TransferStatus.PENDING,
    val progress: Float = 0f,
    val totalSize: Long = 0,
    val transferredSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

enum class TransferType { UPLOAD, DOWNLOAD }
enum class TransferStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED }
