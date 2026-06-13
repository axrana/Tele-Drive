package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = false,
    val shouldCompress: Boolean = false,
    val autoUpload: Boolean = false,
    val sortOrder: String = "DATE",
    val journalLastSyncMessageId: Long = 0,
    val storageLastSyncMessageId: Long = 0
)
