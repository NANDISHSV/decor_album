package com.nandi.srctenthouse.data

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