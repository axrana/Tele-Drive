package com.example.teledrive.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object UriUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()

        val file = File(context.cacheDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream.available()
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            return null
        }
        return file
    }
}
