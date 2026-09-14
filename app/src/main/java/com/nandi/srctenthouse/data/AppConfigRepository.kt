package com.nandi.srctenthouse.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppUpdateConfig(
    val minVersionCode: Long = 0,
    val message: String = "A new version is available. Please update to continue.",
    val latestVersionCode: Long = 0,
    val softMessage: String = "A new version is available."
)

class AppConfigRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUpdateConfig(): AppUpdateConfig? {
        return try {
            val doc = db.collection("config").document("app_version").get().await()
            if (!doc.exists()) return null
            AppUpdateConfig(
                minVersionCode = doc.getLong("minVersionCode") ?: 0,
                message = doc.getString("message")
                    ?: "A new version is available. Please update to continue.",
                latestVersionCode = doc.getLong("latestVersionCode") ?: 0,
                softMessage = doc.getString("softMessage") ?: "A new version is available."
            )
        } catch (e: Exception) {
            null
        }
    }
}