package com.example.teledrive.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

class UploadWorker(
    context: Context,
    params: WorkerParameters,
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val path = inputData.getString("filepath") ?: return@coroutineScope Result.failure()
        val folderId = inputData.getLong("folderid", -1L)
        val file = File(path)
        if (!file.exists()) return@coroutineScope Result.failure()

        try {
            val session = repository.getUserSession().firstOrNull() ?: return@coroutineScope Result.failure()

            // Track progress via TdLibraryManager.fileUpdates
            val progressJob = launch {
                tdLibraryManager.fileUpdates.collect { tdFile ->
                    if (tdFile.local.path == file.absolutePath) {
                        val progress = if (tdFile.size > 0) {
                            tdFile.local.downloadedSize.toFloat() / tdFile.size
                        } else 0f
                        setProgress(workDataOf("progress" to progress))
                    }
                }
            }

            val caption = if (folderId != -1L) {
                TdApi.FormattedText("TeleDriveFolder:$folderId ${file.name}", null)
            } else {
                null
            }

            val inputMessage = TdApi.InputMessageDocument(
                TdApi.InputFileLocal(file.absolutePath),
                null,
                false,
                caption
            )

            val message = tdLibraryManager.execute<TdApi.Message>(
                TdApi.SendMessage(session.channelId, null, null, null, null, inputMessage)
            )

            progressJob.cancel()

            val docContent = message.content as TdApi.MessageDocument
            repository.createFile(FileEntity(
                name = file.name,
                size = file.length(),
                mimeType = docContent.document.mimeType,
                extension = file.extension,
                telegramMsgId = message.id,
                telegramFileId = docContent.document.document.remote.id,
                folderId = if (folderId == -1L) null else folderId,
                uploadDate = System.currentTimeMillis()
            ))
            Result.success()
        } catch (e: Exception) {
            tdLibraryManager.errorFlow.emit("Upload failed for ${file.name}: ${e.message}")
            Result.failure()
        }
    }
}
