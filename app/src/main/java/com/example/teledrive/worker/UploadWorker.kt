package com.example.teledrive.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ForegroundInfo
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import com.example.teledrive.util.ImageCompressor
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File

class UploadWorker(
    context: Context,
    params: WorkerParameters,
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "upload_channel"
    private val notificationId = params.id.hashCode()

    override suspend fun doWork(): Result {
        val filePath = inputData.getString("file_path") ?: return Result.failure()
        val folderId = inputData.getLong("folder_id", -1L)
        val shouldCompress = inputData.getBoolean("should_compress", false)

        if (folderId == -1L) return Result.failure()
        var file = File(filePath)
        if (!file.exists()) return Result.failure()

        val originalName = file.name
        val originalExtension = file.extension

        if (file.length() > 2L * 1024 * 1024 * 1024) return Result.failure()

        if (shouldCompress && isImage(file)) {
            file = ImageCompressor(applicationContext).compressImage(file)
        }

        createNotificationChannel()
        setForeground(createForegroundInfo(0f, "Preparing upload..."))

        return try {
            val session = repository.getUserSession().firstOrNull() ?: return Result.failure()
            val folder = repository.getFolderById(folderId) ?: return Result.failure()

            val inputMessage = TdApi.InputMessageDocument(TdApi.InputFileLocal(file.absolutePath))
            val message = tdLibraryManager.execute(
                TdApi.SendMessage(session.channelId, folder.telegramThreadMsgId, null, null, null, inputMessage)
            )

            val messageDocument = message.content as TdApi.MessageDocument
            val document = messageDocument.document
            val telegramFileId = document.document.remote.id

            repository.createFile(FileEntity(
                name = originalName,
                size = file.length(),
                mimeType = document.mimeType,
                extension = originalExtension,
                telegramMsgId = message.id,
                telegramFileId = telegramFileId,
                folderId = folderId,
                uploadDate = System.currentTimeMillis(),
                thumbnailPath = null
            ))

            if (file.path.contains(applicationContext.cacheDir.path)) file.delete()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun isImage(file: File) = listOf("jpg", "jpeg", "png", "webp").contains(file.extension.lowercase())

    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "File Uploads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun createForegroundInfo(progress: Float, status: String) = ForegroundInfo(
        notificationId,
        NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Uploading to Tele Drive")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .build()
    )
}
