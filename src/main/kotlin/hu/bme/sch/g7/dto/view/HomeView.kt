package hu.bme.sch.g7.dto.view

import com.fasterxml.jackson.annotation.JsonView
import hu.bme.sch.g7.dto.AchievementEntityWrapper
import hu.bme.sch.g7.dto.FullDetails
import hu.bme.sch.g7.dto.Preview
import hu.bme.sch.g7.dto.TopListEntryDto
import hu.bme.sch.g7.model.EventEntity
import hu.bme.sch.g7.model.NewsEntity

data class HomeView(
        @JsonView(FullDetails::class)
        val warningMessage: String = "",

        @JsonView(Preview::class)
        val news: List<NewsEntity> = listOf(),

        @JsonView(Preview::class)
        val eventsToday: List<EventEntity> = listOf(),

        @JsonView(Preview::class)
        val achievements: List<AchievementEntityWrapper> = listOf(),

        @JsonView(Preview::class)
        val leaderBoard: List<TopListEntryDto>
)