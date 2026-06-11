package com.example.teledrive.transfers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.teledrive.domain.repository.TeleDriveRepository
import com.example.teledrive.telegram.TdLibraryManager
import com.example.teledrive.storage.UriUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

class DownloadWorker(
    context: Context,
    params: WorkerParameters,
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val transferId = inputData.getLong("transferId", -1L)
        if (transferId == -1L) return@coroutineScope Result.failure()

        val transfer = repository.getTransferById(transferId) ?: return@coroutineScope Result.failure()

        try {
            repository.updateTransfer(transfer.copy(status = "ACTIVE", updatedAt = System.currentTimeMillis()))

            val progressJob = launch {
                tdLibraryManager.fileUpdates.collect { tdFile ->
                    if (tdFile.remote.id == transfer.remoteId) {
                        val progress = if (tdFile.size > 0) {
                            tdFile.local.downloadedSize.toFloat() / tdFile.size
                        } else 0f
                        repository.updateTransfer(transfer.copy(
                            status = "ACTIVE",
                            progress = progress,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }
            }

            val file = if (!transfer.remoteId.isNullOrEmpty()) {
                tdLibraryManager.execute<TdApi.File>(TdApi.GetRemoteFile(transfer.remoteId, null))
            } else {
                throw Exception("Missing remote file ID")
            }

            tdLibraryManager.execute<TdApi.File>(TdApi.DownloadFile(file.id, 1, 0, 0, false))

            // Wait for completion (via fileUpdates)
            var downloadedFile: TdApi.File? = null
            try {
                tdLibraryManager.fileUpdates.collect { tdFile ->
                    if (tdFile.remote.id == transfer.remoteId && tdFile.local.isDownloadingCompleted) {
                        downloadedFile = tdFile
                        throw Exception("DOWNLOAD_SUCCESS")
                    }
                }
            } catch (e: Exception) {
                if (e.message != "DOWNLOAD_SUCCESS") throw e
            }

            progressJob.cancel()

            if (downloadedFile != null) {
                val success = UriUtils.saveToDownloads(
                    applicationContext,
                    File(downloadedFile!!.local.path),
                    transfer.name,
                    transfer.mimeType
                )
                if (success) {
                    repository.updateTransfer(transfer.copy(
                        status = "COMPLETED",
                        progress = 1f,
                        updatedAt = System.currentTimeMillis()
                    ))
                    Result.success()
                } else {
                    throw Exception("Failed to save to Downloads")
                }
            } else {
                throw Exception("Download failed to complete")
            }
        } catch (e: Exception) {
            repository.updateTransfer(transfer.copy(
                status = "FAILED",
                error = e.message,
                updatedAt = System.currentTimeMillis()
            ))
            Result.failure()
        }
    }
}
