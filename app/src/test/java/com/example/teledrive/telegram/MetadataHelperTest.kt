package com.example.teledrive.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataHelperTest {

    @Test
    fun testFolderMetadataFormattingAndParsing() {
        val name = "Test Folder"
        val parentId = 123L
        val formatted = MetadataHelper.formatFolderMetadata(name, parentId)

        val parsed = MetadataHelper.parseFolderMetadata(formatted)
        assertEquals(name, parsed?.name)
        assertEquals(parentId, parsed?.parentId)
    }

    @Test
    fun testFolderMetadataWithoutParent() {
        val name = "Root Folder"
        val formatted = MetadataHelper.formatFolderMetadata(name, null)

        val parsed = MetadataHelper.parseFolderMetadata(formatted)
        assertEquals(name, parsed?.name)
        assertNull(parsed?.parentId)
    }

    @Test
    fun testFileMetadataFormattingAndParsing() {
        val name = "test.txt"
        val folderId = 456L
        val formatted = MetadataHelper.formatFileMetadata(name, folderId)

        val parsed = MetadataHelper.parseFileMetadata(formatted)
        assertEquals(name, parsed?.name)
        assertEquals(folderId, parsed?.folderId)
    }
}
