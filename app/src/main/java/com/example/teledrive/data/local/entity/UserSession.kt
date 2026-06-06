package com.example.teledrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSession(
    @PrimaryKey val id: Int = 1, // Single user app (local metadata)
    val telegramUserId: Long,
    val phoneNumber: String,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val channelId: Long,
    val channelUsername: String?,
    val isPremium: Boolean = false,
    val loginDate: Long
)
