package com.example.teledrive.util

import com.example.teledrive.data.local.entity.Folder

object TreeBuilder {
    /**
     * Reconstructs the folder hierarchy and detects cycles.
     * Returns a list of folders with updated parentId based on telegramParentThreadId.
     */
    fun reconstructHierarchy(allFolders: List<Folder>): List<Folder> {
        val folderMap = allFolders.associateBy { it.telegramThreadMsgId }
        val result = mutableListOf<Folder>()

        allFolders.forEach { folder ->
            if (folder.telegramParentThreadId != null) {
                val parent = folderMap[folder.telegramParentThreadId]
                if (parent != null) {
                    if (wouldCreateCycle(folder, parent.telegramThreadMsgId, folderMap)) {
                        // Cycle detected, move to root or keep as is but break link
                        result.add(folder.copy(parentFolderId = null, telegramParentThreadId = null))
                    } else {
                        // Valid parent found, parentFolderId will be updated by repository using local DB IDs
                        result.add(folder)
                    }
                } else {
                    // Parent not found in this sync, move to root
                    result.add(folder.copy(parentFolderId = null, telegramParentThreadId = null))
                }
            } else {
                result.add(folder)
            }
        }
        return result
    }

    private fun wouldCreateCycle(folder: Folder, targetParentThreadId: Long, folderMap: Map<Long, Folder>): Boolean {
        var current: Long? = targetParentThreadId
        val visited = mutableSetOf<Long>()
        while (current != null) {
            if (current == folder.telegramThreadMsgId) return true
            if (visited.contains(current)) return true // Already visited this path
            visited.add(current)
            current = folderMap[current]?.telegramParentThreadId
        }
        return false
    }
}
