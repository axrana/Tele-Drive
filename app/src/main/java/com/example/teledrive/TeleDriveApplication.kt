package com.example.teledrive

import android.app.Application
import androidx.work.Configuration
import com.example.teledrive.data.local.TeleDriveDatabase
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import com.example.teledrive.worker.TeleDriveWorkerFactory
import androidx.room.Room

class TeleDriveApplication : Application(), Configuration.Provider {

    lateinit var database: TeleDriveDatabase
    lateinit var repository: TeleDriveRepository
    lateinit var tdLibraryManager: TdLibraryManager

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, TeleDriveDatabase::class.java, "teledrive.db").build()
        repository = TeleDriveRepository(
            database.userSessionDao(),
            database.folderDao(),
            database.fileDao(),
            database.shareTokenDao()
        )
        tdLibraryManager = TdLibraryManager(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(TeleDriveWorkerFactory(tdLibraryManager, repository))
            .build()
}
