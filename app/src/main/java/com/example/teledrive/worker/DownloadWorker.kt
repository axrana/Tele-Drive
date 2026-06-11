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
import kotlinx.coroutines.delay
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

        val transferId = repository.insertTransfer(
            TransferEntity(
                type = TransferType.DOWNLOAD,
                fileId = fileEntity.id,
                fileName = fileEntity.name,
                localPath = "",
                remoteId = fileEntity.telegramFileId,
                totalSize = fileEntity.size
            )
        )

        try {
            repository.updateTransferProgress(transferId, TransferStatus.IN_PROGRESS, 0f, 0)

            val getRemoteFileQuery = TdApi.GetRemoteFile()
            getRemoteFileQuery.remoteFileId = fileEntity.telegramFileId

            val tdFile = tdLibraryManager.execute(getRemoteFileQuery)

            val progressJob = launch {
                tdLibraryManager.fileUpdates.collect { updatedFile ->
                    if (updatedFile.id == tdFile.id && updatedFile.size > 0) {
                        val progress = updatedFile.local.downloadedSize.toFloat() / updatedFile.size
                        repository.updateTransferProgress(
                            transferId,
                            TransferStatus.IN_PROGRESS,
                            progress,
                            updatedFile.local.downloadedSize
                        )
                        setProgress(workDataOf("progress" to progress))
                    }
                }
            }

            val downloadFileQuery = TdApi.DownloadFile()
            downloadFileQuery.fileId = tdFile.id
            downloadFileQuery.priority = 1
            downloadFileQuery.offset = 0
            downloadFileQuery.limit = 0
            downloadFileQuery.synchronous = false

            tdLibraryManager.send(downloadFileQuery)

            // Poll for completion
            var downloadedFile: TdApi.File = tdFile
            var attempts = 0
            while (!downloadedFile.local.isDownloadingCompleted && attempts < 120) {
                delay(1000)
                val getFileQuery = TdApi.GetFile()
                getFileQuery.fileId = tdFile.id
                downloadedFile = tdLibraryManager.execute(getFileQuery)
                attempts++
            }

            progressJob.cancel()

            if (!downloadedFile.local.isDownloadingCompleted) {
                throw Exception("Download timed out")
            }

            val localFile = File(downloadedFile.local.path)
            val success = UriUtils.saveToDownloads(context, localFile, fileEntity.name, fileEntity.mimeType)

            val currentTransfer = repository.getTransferById(transferId)
            if (currentTransfer != null) {
                repository.updateTransfer(
                    currentTransfer.copy(
                        status = if (success) TransferStatus.COMPLETED else TransferStatus.FAILED,
                        progress = if (success) 1f else 0f,
                        transferredSize = if (success) fileEntity.size else 0,
                        localPath = downloadedFile.local.path
                    )
                )
            }

            if (success) Result.success() else Result.failure()
        } catch (e: Exception) {
            val currentTransfer = repository.getTransferById(transferId)
            if (currentTransfer != null) {
                repository.updateTransfer(
                    currentTransfer.copy(status = TransferStatus.FAILED, errorMessage = e.message)
                )
            }
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
