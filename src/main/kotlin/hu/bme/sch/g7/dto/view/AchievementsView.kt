package hu.bme.sch.g7.dto.view

import com.fasterxml.jackson.annotation.JsonView
import hu.bme.sch.g7.dto.AchievementCategoryDto
import hu.bme.sch.g7.dto.Preview
import hu.bme.sch.g7.dto.TopListEntryDto

data class AchievementsView(
        @JsonView(Preview::class)
        val groupScore: Int?,

        @JsonView(Preview::class)
        val categories: List<AchievementCategoryDto> = listOf(),

        @JsonView(Preview::class)
        val leaderBoard: List<TopListEntryDto>,

        @JsonView(Preview::class)
        val leaderBoardVisible: Boolean,

        @JsonView(Preview::class)
        val leaderBoardFrozen: Boolean,
)