package com.example.teledrive

import android.app.Application
import android.widget.Toast
import androidx.work.Configuration
import com.example.teledrive.data.local.TeleDriveDatabase
import com.example.teledrive.domain.repository.TeleDriveRepository
import com.example.teledrive.data.repository.TeleDriveRepositoryImpl
import com.example.teledrive.telegram.TdLibraryManager
import com.example.teledrive.transfers.TeleDriveWorkerFactory
import androidx.room.Room
import com.example.teledrive.R

class TeleDriveApplication : Application(), Configuration.Provider {

    lateinit var database: TeleDriveDatabase
    lateinit var repository: TeleDriveRepository
    lateinit var tdLibraryManager: TdLibraryManager

    override fun onCreate() {
        super.onCreate()

        val apiId = getString(R.string.telegram_api_id)
        val apiHash = getString(R.string.telegram_api_hash)
        if (apiId == "YOUR_API_ID_HERE" || apiHash == "YOUR_API_HASH_HERE" || apiId == "0" || apiHash == "none") {
            Toast.makeText(this, "API ID or HASH not set in strings.xml!", Toast.LENGTH_LONG).show()
            return
        }

        database = Room.databaseBuilder(this, TeleDriveDatabase::class.java, "teledrive.db")
            .fallbackToDestructiveMigration()
            .build()
        repository = TeleDriveRepositoryImpl(
            database.userSessionDao(),
            database.folderDao(),
            database.fileDao(),
            database.shareTokenDao(),
            database.settingsDao(),
            database.transferDao()
        )
        tdLibraryManager = TdLibraryManager(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(TeleDriveWorkerFactory(tdLibraryManager, repository))
            .build()
}
