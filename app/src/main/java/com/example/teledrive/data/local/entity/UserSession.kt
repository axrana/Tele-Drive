package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSession(
    @PrimaryKey val id: Int = 1,
    val telegramUserId: Long,
    val phoneNumber: String,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val channelId: Long, // Deprecated but keeping for migration
    val channelUsername: String?, // Deprecated but keeping for migration
    val storageChannelId: Long,
    val storageChannelUsername: String?,
    val journalChannelId: Long,
    val journalChannelUsername: String?,
    val isPremium: Boolean,
    val loginDate: Long
)
