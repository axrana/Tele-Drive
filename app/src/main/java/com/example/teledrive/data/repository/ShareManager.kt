package com.example.teledrive.data.repository

import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.UserSession
import java.util.*

class ShareManager(private val repository: TeleDriveRepository) {
    suspend fun generateShareLink(file: FileEntity, password: String? = null): String {
        val token = UUID.randomUUID().toString()
        val session = repository.getUserSession().firstOrNull() ?: throw Exception("User not logged in")
        repository.createShareToken(file.id, token, session.telegramUserId, password)
        return "teledrive://share/$token"
    }

    suspend fun resolveShareToken(token: String): FileEntity? {
        val shareToken = repository.getShareToken(token) ?: return null
        return repository.getFileById(shareToken.fileId)
    }
}
