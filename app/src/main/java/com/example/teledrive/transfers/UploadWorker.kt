package com.example.teledrive.transfers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.domain.repository.TeleDriveRepository
import com.example.teledrive.telegram.TdLibraryManager
import com.example.teledrive.telegram.MetadataHelper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

class UploadWorker(
    context: Context,
    params: WorkerParameters,
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val transferId = inputData.getLong("transferId", -1L)
        if (transferId == -1L) return@coroutineScope Result.failure()

        val transfer = repository.getTransferById(transferId) ?: return@coroutineScope Result.failure()
        val file = File(transfer.localPath)

        if (!file.exists()) {
            repository.updateTransfer(transfer.copy(status = "FAILED", error = "File not found", updatedAt = System.currentTimeMillis()))
            return@coroutineScope Result.failure()
        }

        try {
            val session = repository.getUserSession().firstOrNull() ?: throw Exception("Not logged in")
            repository.updateTransfer(transfer.copy(status = "ACTIVE", updatedAt = System.currentTimeMillis()))

            val progressJob = launch {
                tdLibraryManager.fileUpdates.collect { tdFile ->
                    if (tdFile.local.path == file.absolutePath) {
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

            val caption = MetadataHelper.formatFileMetadata(transfer.name, transfer.folderId?.let {
                repository.getFolderById(it)?.telegramThreadMsgId
            })

            val inputMessage = TdApi.InputMessageDocument(
                TdApi.InputFileLocal(file.absolutePath),
                null,
                false,
                TdApi.FormattedText(caption, null)
            )

            val message = tdLibraryManager.execute<TdApi.Message>(
                TdApi.SendMessage(session.channelId, null, null, null, null, inputMessage)
            )

            progressJob.cancel()

            val docContent = message.content as TdApi.MessageDocument
            repository.createFile(FileEntity(
                name = transfer.name,
                size = file.length(),
                mimeType = docContent.document.mimeType,
                extension = file.extension,
                telegramMsgId = message.id,
                telegramFileId = docContent.document.document.remote.id,
                folderId = transfer.folderId,
                uploadDate = System.currentTimeMillis(),
                syncState = "SYNCED"
            ))

            repository.updateTransfer(transfer.copy(
                status = "COMPLETED",
                progress = 1f,
                updatedAt = System.currentTimeMillis()
            ))

            Result.success()
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
