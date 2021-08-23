package hu.bme.sch.g7.controller

import hu.bme.sch.g7.admin.OverviewBuilder
import hu.bme.sch.g7.dao.SubmittedAchievementRepository
import hu.bme.sch.g7.dto.TopListEntryDto
import hu.bme.sch.g7.dto.virtual.CheckRatingVirtualEntity
import hu.bme.sch.g7.dto.virtual.GroupMemberVirtualEntity
import hu.bme.sch.g7.model.SoldProductEntity
import hu.bme.sch.g7.model.SubmittedAchievementEntity
import hu.bme.sch.g7.service.LeaderBoardService
import hu.bme.sch.g7.service.ProductService
import hu.bme.sch.g7.service.RealtimeConfigService
import hu.bme.sch.g7.service.UserService
import hu.bme.sch.g7.util.getUser
import hu.bme.sch.g7.util.getUserOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest

const val CONTROL_MODE_TOPLIST = "toplist"
const val CONTROL_MODE_PAYED = "payed"
const val CONTROL_MODE_NONE = "none"

@Controller
@RequestMapping("/admin/control")
class AdminPanelCustomController(
        private val leaderBoardService: LeaderBoardService,
        private val productService: ProductService,
        private val userService: UserService,
        private val config: RealtimeConfigService,
        private val submittedRepository: SubmittedAchievementRepository,
        @Value("\${g7web.profile.qr-prefix:G7_}") private val prefix: String
) {

    private val topListDescriptor = OverviewBuilder(TopListEntryDto::class)
    private val debtsDescriptor = OverviewBuilder(SoldProductEntity::class)
    private val membersDescriptor = OverviewBuilder(GroupMemberVirtualEntity::class)
    private val submittedDescriptor = OverviewBuilder(CheckRatingVirtualEntity::class)

    @GetMapping("")
    fun index(): String {
        return "redirect:/admin/control/basics"
    }

    @GetMapping("/basics")
    fun dashboard(model: Model, request: HttpServletRequest): String {
        model.addAttribute("user", request.getUser())
        model.addAttribute("motd", config.getMotd())
        model.addAttribute("website", config.getWebsiteUrl())
        model.addAttribute("staffMessage", config.getStaffMessage())

        return "admin"
    }

    @GetMapping("/toplist")
    fun toplist(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", "Toplista")
        model.addAttribute("description", "Tankörök helyezése a pontversenyben. " +
                "A pontok 10 percenként számítódnak újra. " +
                "Manuális újrageneráláshoz van egy gomb a lap alján.")
        model.addAttribute("view", "toplist")
        model.addAttribute("columns", topListDescriptor.getColumns())
        model.addAttribute("fields", topListDescriptor.getColumnDefinitions())
        model.addAttribute("rows", leaderBoardService.getBoardAnyways())
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_TOPLIST)
        model.addAttribute("leaderboardEnabled", config.isLeaderBoardEnabled())
        model.addAttribute("leaderboardUpdates", config.isLeaderBoardUpdates())

        return "overview"
    }

    @GetMapping("/toplist/refresh")
    fun refreshTopList(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        leaderBoardService.forceRecalculate()
        return "redirect:/admin/control/toplist"
    }

    @GetMapping("/toplist/refresh-enable")
    fun enableRefreshTopList(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        config.setLeaderboardUpdates(true)
        return "redirect:/admin/control/toplist"
    }

    @GetMapping("/toplist/refresh-disable")
    fun disableRefreshTopList(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        config.setLeaderboardUpdates(false)
        return "redirect:/admin/control/toplist"
    }

    @GetMapping("/check-ratings")
    fun viewAll(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantRateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", "Pontok ellenőrzése")
        model.addAttribute("description", "Itt azok a beadások láthatóak amik eltérnek a beadásra adható max ponttól vagy a 0 ponttól.")
        model.addAttribute("view", "rate-achievements")
        model.addAttribute("columns", submittedDescriptor.getColumns())
        model.addAttribute("fields", submittedDescriptor.getColumnDefinitions())
        model.addAttribute("rows", fetchSubmittedChecks())
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_GRADE)

        return "overview"
    }

    private fun fetchSubmittedChecks(): List<CheckRatingVirtualEntity> {
        return submittedRepository.findAllByScoreGreaterThanAndApprovedIsTrue(0)
                .filter { it.score != it.achievement?.maxScore ?: 0 }
                .map { CheckRatingVirtualEntity(it.id, it.groupName, it.score, it.achievement?.maxScore ?: 0) }
    }

    @GetMapping("/debts-of-my-group")
    fun debtsOfMyGroup(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantGroupDebtsMananger }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", "Tanköröm tartozásai")
        model.addAttribute("titleSingular", "Tartozás")
        model.addAttribute("description", "Ha a tartozáshoz a pénzt odaadta neked a kolléga, akkor pipáld ki itt. " +
                "Onnantól a te felelősséged lesz majd elszámolni a gazdaságisnak.")
        model.addAttribute("view", "debts-of-my-group")
        model.addAttribute("columns", debtsDescriptor.getColumns())
        model.addAttribute("fields", debtsDescriptor.getColumnDefinitions())
        model.addAttribute("rows", productService.getAllDebtsByGroup(request.getUser()))
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_PAYED)

        return "overview"
    }

    @GetMapping("/debts-of-my-group/payed/{id}")
    fun setDebtsStatus(@PathVariable id: Int, model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantGroupDebtsMananger }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", "Tartozás")
        model.addAttribute("view", "debts-of-my-group")
        model.addAttribute("id", id)
        model.addAttribute("user", request.getUser())

        val entity = productService.findTransactionById(id)
        if (entity.isEmpty) {
            model.addAttribute("error", INVALID_ID_ERROR)
        } else {
            model.addAttribute("item", entity.orElseThrow().toString())
        }
        return "payed"
    }

    @PostMapping("/debts-of-my-group/payed/{id}")
    fun payed(@PathVariable id: Int, model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantGroupDebtsMananger }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        productService.setTransactionPayed(id, request.getUser())
        return "redirect:/admin/control/debts-of-my-group"
    }

    @GetMapping("/members-of-my-group")
    fun membersOfMyGroup(model: Model, request: HttpServletRequest): String {

        model.addAttribute("title", "Tanköröm tagjai")
        model.addAttribute("description", "A tankörödben lévő emberek. Ameddig valaki nem jelentkezik be, addig itt nem látszik, hogy rendező-e.")
        model.addAttribute("view", "members-of-my-group")
        model.addAttribute("columns", membersDescriptor.getColumns())
        model.addAttribute("fields", membersDescriptor.getColumnDefinitions())
        model.addAttribute("rows", userService.allMembersOfGroup(request.getUser().groupName))
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_NONE)

        return "overview"
    }

    @GetMapping("/share-location")
    fun shareLocation(model: Model, request: HttpServletRequest): String {

        model.addAttribute("user", request.getUser())
        model.addAttribute("accessToken", request.getUser().g7id.substring(prefix.length))

        return "shareLocation"
    }

}