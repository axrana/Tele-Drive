package com.example.teledrive.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
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

    private fun createForegroundInfo(fileName: String, progress: Int): ForegroundInfo {
        val channelId = "upload_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "File Uploads", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Uploading: $fileName")
            .setContentText("$progress% uploaded")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1001, notification)
    }

    override suspend fun doWork(): Result = coroutineScope {
        val path = inputData.getString("filepath") ?: return@coroutineScope Result.failure()
        val folderId = inputData.getLong("folderid", -1L)
        val file = File(path)
        if (!file.exists()) return@coroutineScope Result.failure()

        setForeground(createForegroundInfo(file.name, 0))

        try {
            val session = repository.getUserSession().firstOrNull()
                ?: return@coroutineScope Result.failure()

            // Guard against unconfigured storage channel
            if (session.storageChannelId == 0L) {
                return@coroutineScope Result.failure()
            }

            val progressJob = launch {
                tdLibraryManager.fileUpdates.collect { tdFile ->
                    if (tdFile.local.path == file.absolutePath && tdFile.size > 0) {
                        val progress = tdFile.local.downloadedSize.toFloat() / tdFile.size
                        val progressInt = (progress * 100).toInt()
                        setForeground(createForegroundInfo(file.name, progressInt))
                        setProgress(workDataOf("progress" to progress))
                    }
                }
            }

            val captionText = if (folderId != -1L) {
                val folder = repository.getFolderById(folderId)
                val folderKey = folder?.telegramThreadMsgId ?: folderId
                "TeleDriveFolder:$folderKey ${file.name}"
            } else {
                file.name
            }

            val formattedCaption = TdApi.FormattedText()
            formattedCaption.text = captionText

            val inputFile = TdApi.InputFileLocal()
            inputFile.path = file.absolutePath

            val inputMessage = TdApi.InputMessageDocument()
            inputMessage.document = inputFile
            inputMessage.caption = formattedCaption

            val query = TdApi.SendMessage()
            query.chatId = session.storageChannelId
            query.inputMessageContent = inputMessage

            val message = tdLibraryManager.execute(query)

            progressJob.cancel()

            val docContent = message.content as TdApi.MessageDocument
            val remoteId = docContent.document.document.remote.id
            val existing = repository.getFileByTelegramFileId(remoteId)
            if (existing == null) {
                val fileUuid = java.util.UUID.randomUUID().toString()
                val parentFolder = if (folderId != -1L) repository.getFolderById(folderId) else null

                if (session.journalChannelId != 0L) {
                    repository.appendJournalEvent(
                        tdLibraryManager = tdLibraryManager,
                        journalChannelId = session.journalChannelId,
                        op = "CREATE_FILE",
                        objectType = "file",
                        objectId = fileUuid,
                        version = 1,
                        payload = mapOf(
                            "name" to file.name,
                            "size" to file.length(),
                            "mimeType" to docContent.document.mimeType,
                            "storageMessageId" to message.id,
                            "parentFolderUuid" to parentFolder?.folderUuid
                        )
                    )
                }

                repository.createFile(
                    FileEntity(
                        fileUuid = fileUuid,
                        name = file.name,
                        displayName = file.name,
                        size = file.length(),
                        mimeType = docContent.document.mimeType,
                        extension = file.extension,
                        telegramMsgId = message.id,
                        storageMessageId = message.id,
                        telegramFileId = remoteId,
                        folderId = if (folderId == -1L) null else folderId,
                        uploadDate = System.currentTimeMillis()
                    )
                )
            }
            Result.success()
        } catch (e: Exception) {
            tdLibraryManager.errorFlow.emit("Upload failed for ${file.name}: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
