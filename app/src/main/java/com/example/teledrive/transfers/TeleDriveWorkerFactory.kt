package com.example.teledrive.transfers

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import android.content.Context
import com.example.teledrive.domain.repository.TeleDriveRepository
import com.example.teledrive.telegram.TdLibraryManager

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
            UploadWorker::class.java.name -> UploadWorker(appContext, workerParameters, tdLibraryManager, repository)
            else -> null
        }
    }
}
