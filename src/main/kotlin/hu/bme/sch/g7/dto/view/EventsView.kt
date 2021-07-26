package hu.bme.sch.g7.dto.view

import hu.bme.sch.g7.model.EventEntity

data class EventsView(
        val userPreview: UserEntityPreview, // FIXME: ezt mindig le kell küldeni?
        val warningMessage: String = "",
        val eventsToday: List<EventEntity> = listOf(),
        val allEvents: List<EventEntity> = listOf(),
)