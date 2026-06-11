package com.example.teledrive.util

import com.example.teledrive.data.local.entity.Folder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeBuilderTest {

    @Test
    fun testReconstructHierarchy() {
        val folder1 = Folder(id = 1, name = "Root", telegramThreadMsgId = 101L, createdDate = 0L)
        val folder2 = Folder(id = 2, name = "Child", telegramThreadMsgId = 102L, telegramParentThreadId = 101L, createdDate = 0L)

        val folders = listOf(folder1, folder2)
        val result = TreeBuilder.reconstructHierarchy(folders)

        assertEquals(2, result.size)
        assertEquals(101L, result.find { it.name == "Child" }?.telegramParentThreadId)
    }

    @Test
    fun testCycleDetection() {
        // A -> B -> A
        val folderA = Folder(id = 1, name = "A", telegramThreadMsgId = 101L, telegramParentThreadId = 102L, createdDate = 0L)
        val folderB = Folder(id = 2, name = "B", telegramThreadMsgId = 102L, telegramParentThreadId = 101L, createdDate = 0L)

        val folders = listOf(folderA, folderB)
        val result = TreeBuilder.reconstructHierarchy(folders)

        // Both should have their parent links broken to prevent cycles
        assertNull(result.find { it.name == "A" }?.telegramParentThreadId)
        assertNull(result.find { it.name == "B" }?.telegramParentThreadId)
    }
}
