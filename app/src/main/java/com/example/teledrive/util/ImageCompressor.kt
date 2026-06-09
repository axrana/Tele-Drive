package com.example.teledrive.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class ImageCompressor(private val context: Context) {
    fun compressImage(imageFile: File): File {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val compressedFile = File(context.cacheDir, "compressed_${imageFile.name}")
        val out = FileOutputStream(compressedFile)

        // Simplified compression: 85% quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.flush()
        out.close()

        return compressedFile
    }
}
