package com.example.teledrive.worker

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import android.content.Context
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager

class TeleDriveWorkerFactory(
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UploadWorker::class.java.name ->
                UploadWorker(appContext, workerParameters, tdLibraryManager, repository)
            DownloadWorker::class.java.name ->
                DownloadWorker(appContext, workerParameters, tdLibraryManager, repository)
            else -> null
        }
    }
}
