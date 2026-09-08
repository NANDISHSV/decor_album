package com.nandi.srctenthouse.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object LocalImageStore {
    private const val FOLDER_NAME = "downloads"

    private fun downloadsDir(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun downloadImage(context: Context, url: String, fileName: String): File =
        withContext(Dispatchers.IO) {
            val file = File(downloadsDir(context), fileName)
            URL(url).openStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        }

    fun listDownloadedImages(context: Context): List<File> {
        return downloadsDir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun isDownloaded(context: Context, fileName: String): Boolean {
        return File(downloadsDir(context), fileName).exists()
    }

    fun deleteImage(file: File): Boolean {
        return file.delete()
    }
}