package com.example.teledrive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.teledrive.data.local.dao.FileDao
import com.example.teledrive.data.local.dao.FolderDao
import com.example.teledrive.data.local.dao.ShareTokenDao
import com.example.teledrive.data.local.dao.UserSessionDao
import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder
import com.example.teledrive.data.local.entity.ShareToken
import com.example.teledrive.data.local.entity.UserSession

@Database(
    entities = [UserSession::class, Folder::class, FileEntity::class, ShareToken::class],
    version = 1,
    exportSchema = false
)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun shareTokenDao(): ShareTokenDao
}
