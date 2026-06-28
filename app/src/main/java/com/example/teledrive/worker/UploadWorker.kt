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
import kotlinx.coroutines.delay
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
    val currentSession = repository.getUserSession().firstOrNull() ?: return@coroutineScope Result.failure()
    if (currentSession.storageChannelId == 0L) return@coroutineScope Result.failure()
    val folderId = inputData.getLong("folderid", -1L)
    val file = File(path)
    if (!file.exists()) return@coroutineScope Result.failure()

    setForeground(createForegroundInfo(file.name, 0))

    // Ensure TDLib has both chats loaded locally before sending anything.
    // Without this, SendMessage/GetMessage can intermittently fail with
    // "404: Not Found" if the chat hasn't been hydrated into TDLib's cache yet.
    try {
        val onlineOption = TdApi.SetOption()
        onlineOption.name = "online"
        val onlineValue = TdApi.OptionValueBoolean()
        onlineValue.value = true
        onlineOption.value = onlineValue
        tdLibraryManager.send(onlineOption)

        try {
            val loadChats = TdApi.GetChats()
            loadChats.chatList = TdApi.ChatListMain()
            loadChats.limit = 200
            tdLibraryManager.execute(loadChats)
        } catch (e: Exception) {
            // Non-fatal - fall through to direct GetChat attempts below.
        }

        val getStorageChat = TdApi.GetChat()
        getStorageChat.chatId = currentSession.storageChannelId
        tdLibraryManager.execute(getStorageChat)

        if (currentSession.journalChannelId != 0L) {
            var journalHydrated = false
            var hydrateAttempts = 0
            var lastError: Exception? = null
            while (!journalHydrated && hydrateAttempts < 3) {
                try {
                    val getJournalChat = TdApi.GetChat()
                    getJournalChat.chatId = currentSession.journalChannelId
                    tdLibraryManager.execute(getJournalChat)
                    journalHydrated = true
                } catch (e: Exception) {
                    lastError = e
                    hydrateAttempts += 1
                    delay(400L * hydrateAttempts)
                }
            }
            if (!journalHydrated) {
                tdLibraryManager.errorFlow.emit(
                    "DEBUG-HYDRATE: journal chat ${currentSession.journalChannelId} never hydrated after $hydrateAttempts attempts: ${lastError?.message}"
                )
            }
        }
    } catch (e: Exception) {
        // If even GetChat fails, the upload would fail anyway - let it proceed
        // and surface the real error below rather than masking it here.
    }

    try {
            // session obtained

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

            var parentFolderForJournal: com.example.teledrive.data.local.entity.Folder? = null
val captionText = if (folderId != -1L) {
    parentFolderForJournal = try {
        repository.getFolderById(folderId)
    } catch (e: Exception) {
        tdLibraryManager.errorFlow.emit("Folder lookup failed: ${e.message}")
        null
    }
    val folderKey = parentFolderForJournal?.telegramThreadMsgId ?: folderId
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
            query.chatId = currentSession.storageChannelId
            val replyTo = TdApi.InputMessageReplyToMessage()
            replyTo.messageId = 0L
            query.replyTo = replyTo
            query.inputMessageContent = inputMessage

            var message = tdLibraryManager.execute(query)

            // IMPORTANT: TDLib's SendMessage returns a message with a TEMPORARY
            // local message ID. Once the send completes, TDLib swaps it for a
            // permanent ID and the old temp ID becomes invalid - a GetMessage
            // call using the stale temp ID then fails with "404: Not Found".
            // Polling GetMessage with the original ID is racy: if the send
            // finishes between polls, the next poll 404s.
            //
            // Instead, listen for UpdateMessageSendSucceeded, which carries
            // both the old temp ID and the finished message with its real ID.
            if (message.sendingState != null) {
                val originalTempId = message.id
                val sentMessage = withTimeoutOrNull(15_000L) {
                    var result: TdApi.Message? = null
                    val job = launch {
                        tdLibraryManager.updateFlow.collect { obj ->
                            if (obj is TdApi.UpdateMessageSendSucceeded && obj.oldMessageId == originalTempId) {
                                result = obj.message
                                throw kotlinx.coroutines.CancellationException("done")
                            }
                            if (obj is TdApi.UpdateMessageSendFailed && obj.oldMessageId == originalTempId) {
                                throw Exception("Send failed: ${obj.error?.message}")
                            }
                        }
                    }
                    job.join()
                    result
                }
                if (sentMessage != null) {
                    message = sentMessage
                }
            }

            progressJob.cancel()

val docContent = message.content as? TdApi.MessageDocument
    ?: return@coroutineScope Result.failure()
val remoteId = docContent.document.document.remote.id

tdLibraryManager.errorFlow.emit("DEBUG1: message.id=${message.id} journalChannelId=${currentSession.journalChannelId}")

val existing = repository.getFileByTelegramMsgId(message.id)
tdLibraryManager.errorFlow.emit("DEBUG2: existing=${existing != null}")

if (existing == null) {
    val fileUuid = java.util.UUID.randomUUID().toString()
    val parentFolder = parentFolderForJournal

    if (currentSession.journalChannelId != 0L) {
        tdLibraryManager.errorFlow.emit("DEBUG3: entering journal write block")
        val remoteIdActual = (message.content as? TdApi.MessageDocument)?.document?.document?.remote?.id
        try {
            val journalMsgId = repository.appendJournalEvent(
                tdLibraryManager = tdLibraryManager,
                journalChannelId = currentSession.journalChannelId,
                op = "CREATE_FILE",
                objectType = "file",
                objectId = fileUuid,
                version = 1,
                payload = mapOf(
                    "name" to file.name,
                    "size" to file.length(),
                    "mimeType" to docContent.document.mimeType,
                    "storageMessageId" to message.id,
                    "remoteFileId" to remoteIdActual,
                    "parentFolderUuid" to parentFolder?.folderUuid
                )
            )
            tdLibraryManager.errorFlow.emit("DEBUG4: journal write SUCCEEDED, journalMsgId=$journalMsgId")
        } catch (e: Exception) {
            tdLibraryManager.errorFlow.emit("DEBUG4: journal write FAILED: ${e.javaClass.simpleName}: ${e.message}")
        }
    } else {
        tdLibraryManager.errorFlow.emit("DEBUG3: SKIPPED - journalChannelId is 0")
    }

    try {
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
        tdLibraryManager.errorFlow.emit("DEBUG5: createFile SUCCEEDED")
    } catch (e: Exception) {
        tdLibraryManager.errorFlow.emit("DEBUG5: createFile FAILED: ${e.javaClass.simpleName}: ${e.message}")
    }
} else {
    tdLibraryManager.errorFlow.emit("DEBUG: file already exists, skipping (this is the bug if unexpected!)")
}
Result.success()
        } catch (e: Exception) {
            tdLibraryManager.errorFlow.emit("Upload failed for ${file.name}: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
