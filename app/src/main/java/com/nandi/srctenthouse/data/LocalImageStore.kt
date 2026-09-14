package com.nandi.srctenthouse.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object LocalImageStore {
    private const val FOLDER_NAME = "downloads"
    private const val SHARE_FOLDER_NAME = "shared_images"

    private fun downloadsDir(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun shareDir(context: Context): File {
        val dir = File(context.cacheDir, SHARE_FOLDER_NAME)
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

    // Used for sharing: reuses an already-downloaded copy if one exists,
    // otherwise fetches a temporary copy into a separate cache folder
    // (kept apart from the user's actual Downloads tab).
    suspend fun getShareableFile(context: Context, imageUrl: String, photoId: String): File =
        withContext(Dispatchers.IO) {
            val downloadedFileName = "decor_${photoId}.jpg"
            val alreadyDownloaded = File(downloadsDir(context), downloadedFileName)
            if (alreadyDownloaded.exists()) {
                return@withContext alreadyDownloaded
            }

            val file = File(shareDir(context), "share_${photoId}.jpg")
            if (!file.exists()) {
                URL(imageUrl).openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            file
        }
}