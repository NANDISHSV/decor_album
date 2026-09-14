package com.nandi.srctenthouse.data

import com.google.firebase.firestore.DocumentSnapshot

data class EventCategory(
    val id: String = "",
    val name: String = "",
    val coverImage: String = "",
    val order: Int = 0
)

data class Photo(
    val id: String = "",
    val eventId: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val order: Int = 0,
    val displayNumber: Long = 0
)

data class PhotoPage(
    val photos: List<Photo>,
    val lastDocument: DocumentSnapshot?,
    val isLastPage: Boolean
)

data class PhotoPageBefore(
    val photos: List<Photo>,
    val hasMoreBefore: Boolean
)