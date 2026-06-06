package com.example.teledrive.tdlib

import java.io.File
import java.io.RandomAccessFile

class ChunkUploader {
    companion object {
        const val CHUNK_SIZE = 20 * 1024 * 1024 // 20MB

        fun getChunks(file: File): List<ByteArray> {
            val chunks = mutableListOf<ByteArray>()
            val raf = RandomAccessFile(file, "r")
            var bytesRead = 0L
            val fileLength = file.length()

            while (bytesRead < fileLength) {
                val currentChunkSize = minOf(CHUNK_SIZE.toLong(), fileLength - bytesRead).toInt()
                val buffer = ByteArray(currentChunkSize)
                raf.readFully(buffer)
                chunks.add(buffer)
                bytesRead += currentChunkSize
            }
            raf.close()
            return chunks
        }
    }
}
