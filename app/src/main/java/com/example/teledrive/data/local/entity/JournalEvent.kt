package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_events")
data class JournalEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventUuid: String,
    val objectType: String,
    val objectUuid: String,
    val op: String,
    val version: Long,
    val ts: Long,
    val telegramJournalMessageId: Long,
    val payloadJson: String
)
