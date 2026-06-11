package com.example.teledrive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.teledrive.data.local.dao.*
import com.example.teledrive.data.local.entity.*

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserSession::class,
        Folder::class,
        FileEntity::class,
        ShareToken::class,
        Settings::class,
        TransferEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun shareTokenDao(): ShareTokenDao
    abstract fun settingsDao(): SettingsDao
    abstract fun transferDao(): TransferDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update files table
                db.execSQL("ALTER TABLE files ADD COLUMN syncState INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE files ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE files ADD COLUMN localPath TEXT")

                // Update folders table
                db.execSQL("ALTER TABLE folders ADD COLUMN syncState INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN metadataVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN sortOrder TEXT NOT NULL DEFAULT 'DATE'")

                // Create transfers table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        fileId INTEGER,
                        fileName TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        remoteId TEXT,
                        folderId INTEGER,
                        status TEXT NOT NULL,
                        progress REAL NOT NULL,
                        totalSize INTEGER NOT NULL,
                        transferredSize INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        errorMessage TEXT
                    )
                """.trimIndent())
            }
        }
    }
}
