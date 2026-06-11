package com.example.teledrive.transfers

import android.content.Context
import androidx.work.*
import com.example.teledrive.data.local.entity.TransferEntity
import com.example.teledrive.domain.repository.TeleDriveRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class TransferManager(
    private val context: Context,
    private val repository: TeleDriveRepository
) {
    val allTransfers: Flow<List<TransferEntity>> = repository.getAllTransfers()

    suspend fun enqueueUpload(localPath: String, folderId: Long?, name: String, size: Long, mimeType: String?) {
        val transfer = TransferEntity(
            type = "UPLOAD",
            localPath = localPath,
            folderId = folderId,
            status = "PENDING",
            name = name,
            size = size,
            mimeType = mimeType
        )
        val id = repository.insertTransfer(transfer)

        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf("transferId" to id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("transfer")
            .addTag("upload")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "transfer_$id",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun enqueueDownload(fileId: Long, remoteId: String, name: String, size: Long, mimeType: String?) {
        val transfer = TransferEntity(
            type = "DOWNLOAD",
            localPath = "", // Will be determined by DownloadWorker/MediaStore
            fileId = fileId,
            remoteId = remoteId,
            status = "PENDING",
            name = name,
            size = size,
            mimeType = mimeType
        )
        val id = repository.insertTransfer(transfer)

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("transferId" to id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("transfer")
            .addTag("download")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "transfer_$id",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun cancelTransfer(transfer: TransferEntity) {
        WorkManager.getInstance(context).cancelUniqueWork("transfer_${transfer.id}")
        repository.updateTransfer(transfer.copy(status = "CANCELLED", updatedAt = System.currentTimeMillis()))
    }

    suspend fun clearFinishedTransfers() {
        repository.clearFinishedTransfers()
    }
}
