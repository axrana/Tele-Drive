package com.example.teledrive.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import org.drinkless.td.libcore.telegram.TdApi
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class UploadWorker(
    context: Context,
    params: WorkerParameters,
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val path = inputData.getString("file_path") ?: return Result.failure()
        val folderId = inputData.getLong("folder_id", -1L)
        val file = File(path)
        if (!file.exists()) return Result.failure()

        return try {
            val session = repository.getUserSession().firstOrNull() ?: return Result.failure()

            // Fixed InputMessageDocument for TDLib 1.8.x
            val inputMessage = TdApi.InputMessageDocument(
                TdApi.InputFileLocal(file.absolutePath),
                null,
                false,
                null
            )

            val message = tdLibraryManager.execute<TdApi.Message>(
                TdApi.SendMessage(session.channelId, 0, null, null, null, inputMessage)
            )

            val docContent = message.content as TdApi.MessageDocument
            repository.createFile(FileEntity(
                name = file.name,
                size = file.length(),
                mimeType = docContent.document.mimeType,
                extension = file.extension,
                telegramMsgId = message.id,
                telegramFileId = docContent.document.document.remote.id,
                folderId = folderId,
                uploadDate = System.currentTimeMillis()
            ))
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
