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
        TransferEntity::class,
        JournalEvent::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun shareTokenDao(): ShareTokenDao
    abstract fun settingsDao(): SettingsDao
    abstract fun transferDao(): TransferDao
    abstract fun journalEventDao(): JournalEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN syncState INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE files ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE files ADD COLUMN localPath TEXT")

                db.execSQL("ALTER TABLE folders ADD COLUMN syncState INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN metadataVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN sortOrder TEXT NOT NULL DEFAULT 'DATE'")

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // UserSession update
                db.execSQL("ALTER TABLE user_session ADD COLUMN storageChannelId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_session ADD COLUMN storageChannelUsername TEXT")
                db.execSQL("ALTER TABLE user_session ADD COLUMN journalChannelId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_session ADD COLUMN journalChannelUsername TEXT")

                // Journal Event table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS journal_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventUuid TEXT NOT NULL,
                        objectType TEXT NOT NULL,
                        objectUuid TEXT NOT NULL,
                        op TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        ts INTEGER NOT NULL,
                        telegramJournalMessageId INTEGER NOT NULL,
                        payloadJson TEXT NOT NULL
                    )
                """.trimIndent())

                // Folders update
                db.execSQL("ALTER TABLE folders ADD COLUMN folderUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE folders ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE folders ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_folders_folderUuid ON folders (folderUuid)")

                // Files update
                db.execSQL("ALTER TABLE files ADD COLUMN fileUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE files ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE files ADD COLUMN storageMessageId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE files ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE files ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE files ADD COLUMN sourceFileUuid TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_files_fileUuid ON files (fileUuid)")

                // Settings update for sync tracking
                db.execSQL("ALTER TABLE settings ADD COLUMN journalLastSyncMessageId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN storageLastSyncMessageId INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
