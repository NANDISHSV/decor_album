package com.nandi.srctenthouse.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PhotoRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getCategories(): List<EventCategory> {
        val snapshot = db.collection("events")
            .get()
            .await()
        return snapshot.documents.map { doc ->
            EventCategory(
                id = doc.id,
                name = doc.getString("name") ?: "",
                coverImage = doc.getString("coverImage") ?: "",
                order = (doc.getLong("order") ?: 0).toInt()
            )
        }.sortedBy { it.order }
    }

    suspend fun getPhotosForEvent(eventId: String): List<Photo> {
        val snapshot = db.collection("photos")
            .whereEqualTo("eventId", eventId)
            .get()
            .await()
        return snapshot.documents.map { doc ->
            Photo(
                id = doc.id,
                eventId = doc.getString("eventId") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                caption = doc.getString("caption") ?: "",
                order = (doc.getLong("order") ?: 0).toInt(),
                displayNumber = doc.getLong("displayNumber") ?: 0
            )
        }.sortedBy { it.order }
    }
}