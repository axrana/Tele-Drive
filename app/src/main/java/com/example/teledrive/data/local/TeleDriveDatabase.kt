package com.example.teledrive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.teledrive.data.local.dao.*
import com.example.teledrive.data.local.entity.*

@Database(
    entities = [UserSession::class, Folder::class, FileEntity::class, ShareToken::class, Settings::class],
    version = 2,
    exportSchema = false
)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun shareTokenDao(): ShareTokenDao
    abstract fun settingsDao(): SettingsDao
}
