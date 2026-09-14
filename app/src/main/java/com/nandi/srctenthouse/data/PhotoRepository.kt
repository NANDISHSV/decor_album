package com.nandi.srctenthouse.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class PhotoRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun mapCategories(snapshot: com.google.firebase.firestore.QuerySnapshot): List<EventCategory> {
        return snapshot.documents.map { doc ->
            EventCategory(
                id = doc.id,
                name = doc.getString("name") ?: "",
                coverImage = doc.getString("coverImage") ?: "",
                order = (doc.getLong("order") ?: 0).toInt()
            )
        }.sortedBy { it.order }
    }

    fun getCategoriesFlow(): Flow<List<EventCategory>> = flow {
        var emittedFromCache = false
        try {
            val cached = db.collection("events").get(Source.CACHE).await()
            if (!cached.isEmpty) {
                emit(mapCategories(cached))
                emittedFromCache = true
            }
        } catch (e: Exception) {
            // no local cache yet
        }

        try {
            val fresh = db.collection("events").get(Source.SERVER).await()
            emit(mapCategories(fresh))
        } catch (e: Exception) {
            // Offline (or server unreachable) and we already showed cached data — that's fine,
            // just don't crash the flow. If we had NOTHING to show at all, rethrow so the UI
            // can show a real "offline" state instead of an empty screen.
            if (!emittedFromCache) throw e
        }
    }

    private fun docToPhoto(doc: com.google.firebase.firestore.DocumentSnapshot): Photo {
        return Photo(
            id = doc.id,
            eventId = doc.getString("eventId") ?: "",
            imageUrl = doc.getString("imageUrl") ?: "",
            caption = doc.getString("caption") ?: "",
            order = (doc.getLong("order") ?: 0).toInt(),
            displayNumber = doc.getLong("displayNumber") ?: 0
        )
    }

    // Forward pagination. If startAtOrder is given, jumps directly there (O(1) —
    // used when landing from search) instead of paging through from the start.
    suspend fun getPhotosPage(
        eventId: String,
        pageSize: Long = 10,
        lastVisible: DocumentSnapshot? = null,
        startAtOrder: Int? = null
    ): PhotoPage {
        var query = db.collection("photos")
            .whereEqualTo("eventId", eventId)
            .orderBy("order")
            .limit(pageSize)

        if (startAtOrder != null) {
            query = query.startAt(startAtOrder)
        } else if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        return try {
            val snapshot = query.get().await()
            val photos = snapshot.documents.map { docToPhoto(it) }

            PhotoPage(
                photos = photos,
                lastDocument = snapshot.documents.lastOrNull(),
                isLastPage = snapshot.size() < pageSize
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // let real cancellation (e.g. leaving the screen) propagate normally
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "getPhotosPage failed for eventId=$eventId", e)
            // A transient failure isn't "no more photos" — don't lock out further pages.
            PhotoPage(photos = emptyList(), lastDocument = lastVisible, isLastPage = false)
        }
    }

    // Backward pagination — loads photos just before a given order value, for when
    // the user jumped in via search and swipes UP to see what came before it.
    suspend fun getPhotosPageBefore(
        eventId: String,
        beforeOrder: Int,
        pageSize: Long = 10
    ): PhotoPageBefore {
        return try {
            val snapshot = db.collection("photos")
                .whereEqualTo("eventId", eventId)
                .whereLessThan("order", beforeOrder)
                .orderBy("order", Query.Direction.DESCENDING)
                .limit(pageSize)
                .get()
                .await()

            val descending = snapshot.documents.map { docToPhoto(it) }

            PhotoPageBefore(
                photos = descending.reversed(),
                hasMoreBefore = snapshot.size() >= pageSize
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "getPhotosPageBefore failed for eventId=$eventId", e)
            PhotoPageBefore(photos = emptyList(), hasMoreBefore = true)
        }
    }
    suspend fun findPhotoByDisplayNumber(displayNumber: Long): Photo? {
        val snapshot = db.collection("photos")
            .whereEqualTo("displayNumber", displayNumber)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.let { docToPhoto(it) }
    }
}