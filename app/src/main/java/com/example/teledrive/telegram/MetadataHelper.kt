package com.example.teledrive.telegram

import org.drinkless.tdlib.TdApi

object MetadataHelper {
    private const val FOLDER_PREFIX = "TD_FOLDER:"
    private const val FILE_PREFIX = "TD_FILE:"

    data class FolderMetadata(
        val name: String,
        val parentId: Long?,
        val version: Int = 1
    )

    data class FileMetadata(
        val name: String,
        val folderId: Long?,
        val version: Int = 1
    )

    fun formatFolderMetadata(name: String, parentId: Long?): String {
        // Format: TD_FOLDER:{"name":"...","parentId":123,"v":1}
        val parentStr = if (parentId != null) "\"parentId\":$parentId," else ""
        return "$FOLDER_PREFIX{\"name\":\"$name\",$parentStr\"v\":1}"
    }

    fun formatFileMetadata(name: String, folderId: Long?): String {
        val folderStr = if (folderId != null) "\"folderId\":$folderId," else ""
        return "$FILE_PREFIX{\"name\":\"$name\",$folderStr\"v\":1}"
    }

    fun parseFolderMetadata(text: String): FolderMetadata? {
        if (!text.startsWith(FOLDER_PREFIX)) return null
        return try {
            val json = text.removePrefix(FOLDER_PREFIX)
            val name = json.substringAfter("\"name\":\"").substringBefore("\"")
            val parentId = if (json.contains("\"parentId\":")) {
                json.substringAfter("\"parentId\":").substringBefore(",").substringBefore("}").toLongOrNull()
            } else null
            FolderMetadata(name, parentId)
        } catch (e: Exception) {
            null
        }
    }

    fun parseFileMetadata(text: String): FileMetadata? {
        if (!text.startsWith(FILE_PREFIX)) return null
        return try {
            val json = text.removePrefix(FILE_PREFIX)
            val name = json.substringAfter("\"name\":\"").substringBefore("\"")
            val folderId = if (json.contains("\"folderId\":")) {
                json.substringAfter("\"folderId\":").substringBefore(",").substringBefore("}").toLongOrNull()
            } else null
            FileMetadata(name, folderId)
        } catch (e: Exception) {
            null
        }
    }
}
