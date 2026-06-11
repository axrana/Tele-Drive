package com.example.teledrive.telegram

import org.json.JSONObject

object MetadataHelper {
    private const val FOLDER_PREFIX = "TD_FOLDER:"
    private const val LEGACY_FOLDER_PREFIX = "Folder:"
    private const val FILE_CAPTION_PREFIX = "TeleDriveFolder:"

    data class FolderMetadata(
        val name: String,
        val parentId: Long?,
        val version: Int = 1
    )

    fun formatFolderMetadata(name: String, parentId: Long?): String {
        val json = JSONObject().apply {
            put("name", name)
            put("parentId", parentId ?: JSONObject.NULL)
            put("v", 1)
        }
        return "$FOLDER_PREFIX$json"
    }

    fun parseFolderMetadata(text: String): FolderMetadata? {
        if (text.startsWith(FOLDER_PREFIX)) {
            return try {
                val jsonStr = text.removePrefix(FOLDER_PREFIX)
                val json = JSONObject(jsonStr)
                FolderMetadata(
                    name = json.getString("name"),
                    parentId = if (json.isNull("parentId")) null else json.getLong("parentId"),
                    version = json.optInt("v", 1)
                )
            } catch (e: Exception) {
                null
            }
        }

        if (text.startsWith(LEGACY_FOLDER_PREFIX)) {
            return if (text.contains(" Parent:")) {
                val name = text.substringAfter("Folder:").substringBefore(" Parent:").trim()
                val parentKey = text.substringAfter(" Parent:").toLongOrNull()
                FolderMetadata(name, parentKey)
            } else if (text.startsWith("Folder: ")) {
                 val name = text.removePrefix("Folder: ").trim()
                 FolderMetadata(name, null)
            } else {
                val name = text.removePrefix("Folder:").trim()
                FolderMetadata(name, null)
            }
        }

        return null
    }

    fun formatFileCaption(folderThreadId: Long?, fileName: String): String {
        return if (folderThreadId != null) {
            "$FILE_CAPTION_PREFIX$folderThreadId $fileName"
        } else {
            fileName
        }
    }

    fun parseFileFolderId(caption: String): Long? {
        if (caption.startsWith(FILE_CAPTION_PREFIX)) {
            return caption.removePrefix(FILE_CAPTION_PREFIX).substringBefore(" ").toLongOrNull()
        }
        return null
    }
}
