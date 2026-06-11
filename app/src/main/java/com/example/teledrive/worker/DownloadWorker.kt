package com.example.teledrive.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.teledrive.data.local.entity.TransferEntity
import com.example.teledrive.data.local.entity.TransferStatus
import com.example.teledrive.data.local.entity.TransferType
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import com.example.teledrive.util.UriUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters,
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val fileId = inputData.getLong("file_entity_id", -1L)
        if (fileId == -1L) return@coroutineScope Result.failure()

        val fileEntity = repository.getFileById(fileId) ?: return@coroutineScope Result.failure()

        val transferId = repository.insertTransfer(TransferEntity(
            type = TransferType.DOWNLOAD,
            fileId = fileEntity.id,
            fileName = fileEntity.name,
            localPath = "", // To be filled after download
            remoteId = fileEntity.telegramFileId,
            totalSize = fileEntity.size
        ))

        try {
            repository.updateTransferProgress(transferId, TransferStatus.IN_PROGRESS, 0f, 0)

            val tdFile = tdLibraryManager.execute<TdApi.File>(TdApi.GetRemoteFile(fileEntity.telegramFileId, null))

            val progressJob = launch {
                tdLibraryManager.fileUpdates.collect { updatedFile ->
                    if (updatedFile.id == tdFile.id) {
                        val progress = if (updatedFile.size > 0) {
                            updatedFile.local.downloadedSize.toFloat() / updatedFile.size
                        } else 0f
                        repository.updateTransferProgress(transferId, TransferStatus.IN_PROGRESS, progress, updatedFile.local.downloadedSize.toLong())
                        setProgress(workDataOf("progress" to progress))
                    }
                }
            }

            tdLibraryManager.execute<TdApi.Ok>(TdApi.DownloadFile(tdFile.id, 1, 0, 0, true) as TdApi.Function<TdApi.Ok>)

            // Wait for completion
            var downloadedFile: TdApi.File = tdFile
            while (!downloadedFile.local.isDownloadingCompleted) {
                kotlinx.coroutines.delay(1000)
                downloadedFile = tdLibraryManager.execute<TdApi.File>(TdApi.GetFile(tdFile.id))
            }

            progressJob.cancel()

            val localFile = File(downloadedFile.local.path)
            val success = UriUtils.saveToDownloads(context, localFile, fileEntity.name, fileEntity.mimeType)

            if (success) {
                val currentTransfer = repository.getTransferById(transferId)
                if (currentTransfer != null) {
                    repository.updateTransfer(currentTransfer.copy(
                        status = TransferStatus.COMPLETED,
                        progress = 1f,
                        transferredSize = fileEntity.size,
                        localPath = downloadedFile.local.path
                    ))
                }
                Result.success()
            } else {
                throw Exception("Failed to save to MediaStore")
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            val currentTransfer = repository.getTransferById(transferId)
            if (currentTransfer != null) {
                repository.updateTransfer(currentTransfer.copy(status = TransferStatus.FAILED, errorMessage = errorMsg))
            }
            Result.failure()
        }
    }
}
