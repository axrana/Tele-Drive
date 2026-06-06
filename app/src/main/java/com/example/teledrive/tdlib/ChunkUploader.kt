package com.example.teledrive.tdlib

import java.io.File
import java.io.RandomAccessFile

class ChunkUploader {
    companion object {
        const val CHUNK_SIZE = 20 * 1024 * 1024 // 20MB

        /**
         * Streams chunks of a file one by one to avoid OutOfMemoryError.
         */
        fun streamChunks(file: File, onChunkReady: (ByteArray) -> Unit) {
            val raf = RandomAccessFile(file, "r")
            var bytesRead = 0L
            val fileLength = file.length()

            try {
                while (bytesRead < fileLength) {
                    val currentChunkSize = minOf(CHUNK_SIZE.toLong(), fileLength - bytesRead).toInt()
                    val buffer = ByteArray(currentChunkSize)
                    raf.readFully(buffer)
                    onChunkReady(buffer)
                    bytesRead += currentChunkSize
                }
            } finally {
                raf.close()
            }
        }

        /**
         * Get total chunk count for progress tracking.
         */
        fun getChunkCount(file: File): Int {
            return Math.ceil(file.length().toDouble() / CHUNK_SIZE).toInt()
        }
    }
}
