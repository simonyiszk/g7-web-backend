package hu.bme.sch.g7.controller

import hu.bme.sch.g7.admin.INPUT_TYPE_FILE
import hu.bme.sch.g7.admin.INTERPRETER_INHERIT
import hu.bme.sch.g7.admin.OverviewBuilder
import hu.bme.sch.g7.dao.GroupRepository
import hu.bme.sch.g7.dao.SoldProductRepository
import hu.bme.sch.g7.dto.virtual.DebtsByGroup
import hu.bme.sch.g7.dto.virtual.DebtsByUser
import hu.bme.sch.g7.model.SoldProductEntity
import hu.bme.sch.g7.service.ClockService
import hu.bme.sch.g7.util.getUser
import hu.bme.sch.g7.util.getUserOrNull
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KMutableProperty1

@Controller
@RequestMapping("/admin/control/debts-by-users")
class DebtsByUsersController(
        private val soldProductController: SoldProductRepository,
        private val groupRepository: GroupRepository,
        private val clock: ClockService
) {

    private val view = "debts-by-users"
    private val titleSingular = "Tartozás"
    private val titlePlural = "Tartozások"
    private val description = "Tartozások felhasználónként csoportosítva"

    private val entitySourceMapping: Map<String, (SoldProductEntity) -> List<String>> =
            mapOf(Nothing::class.simpleName!! to { listOf() })

    private val overviewDescriptor = OverviewBuilder(DebtsByUser::class)
    private val submittedDescriptor = OverviewBuilder(SoldProductEntity::class)

    @GetMapping("")
    fun view(model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", titlePlural)
        model.addAttribute("titleSingular", titleSingular)
        model.addAttribute("description", description)
        model.addAttribute("view", view)
        model.addAttribute("columns", overviewDescriptor.getColumns())
        model.addAttribute("fields", overviewDescriptor.getColumnDefinitions())
        model.addAttribute("rows", fetchOverview())
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_VIEW)

        return "overview"
    }

    private fun fetchOverview(): List<DebtsByUser> {
        return soldProductController.findAll().groupBy { it.ownerId }
                .map { it.value }
                .filter { !it.isEmpty() }
                .map {
                    val groupName = groupRepository.findById(it[0].responsibleGroupId).map { it.name }.orElse("n/a")
                    DebtsByUser(
                            it[0].ownerId,
                            it[0].ownerName,
                            groupName,
                            it.sumOf { it.price },
                            it.filter { !it.payed }.sumOf { it.price },
                            it.filter { !it.finsihed }.sumOf { it.price }
                    )
                }
    }

    @GetMapping("/view/{id}")
    fun viewAll(@PathVariable id: Int, model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", titlePlural)
        model.addAttribute("titleSingular", titleSingular)
        model.addAttribute("description", description)
        model.addAttribute("view", view)
        model.addAttribute("columns", submittedDescriptor.getColumns())
        model.addAttribute("fields", submittedDescriptor.getColumnDefinitions())
        model.addAttribute("rows", soldProductController.findAllByOwnerId(id))
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_EDIT)

        return "overview"
    }

    @GetMapping("/edit/{id}")
    fun edit(@PathVariable id: Int, model: Model, request: HttpServletRequest): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        model.addAttribute("title", titleSingular)
        model.addAttribute("editMode", true)
        model.addAttribute("view", view)
        model.addAttribute("id", id)
        model.addAttribute("inputs", submittedDescriptor.getInputs())
        model.addAttribute("mappings", entitySourceMapping)
        model.addAttribute("user", request.getUser())
        model.addAttribute("controlMode", CONTROL_MODE_EDIT)

        val entity = soldProductController.findById(id)
        if (entity.isEmpty) {
            model.addAttribute("error", INVALID_ID_ERROR)
        } else {
            model.addAttribute("data", entity.orElseThrow())
        }
        return "details"
    }

    @PostMapping("/edit/{id}")
    fun edit(@PathVariable id: Int,
             @ModelAttribute(binding = false) dto: SoldProductEntity,
             model: Model,
             request: HttpServletRequest
    ): String {
        if (request.getUserOrNull()?.let { it.isAdmin() || it.grantCreateAchievement || it.grantCreateAchievement }?.not() ?: true) {
            model.addAttribute("user", request.getUser())
            return "admin403"
        }

        val entity = soldProductController.findById(id)
        if (entity.isEmpty) {
            return "redirect:/admin/control/$view/edit/$id"
        }

        val user = request.getUser()
        val date = clock.getTimeInSeconds()
        val transaction = entity.get()
        updateEntity(submittedDescriptor, transaction, dto)
        transaction.log = "${transaction.log} '${user.fullName}'(${user.id}) changed [shipped: ${transaction.shipped}, payed: ${transaction.payed}, finsihed: ${transaction.finsihed}] at $date;"
        transaction.id = id
        soldProductController.save(transaction)
        return "redirect:/admin/control/$view"
    }

    private fun updateEntity(
            descriptor: OverviewBuilder,
            entity: SoldProductEntity,
            dto: SoldProductEntity
    ) {
        descriptor.getInputs().forEach {
            if (it.first is KMutableProperty1<out Any, *> && !it.second.ignore) {
                when {
                    it.second.interpreter == INTERPRETER_INHERIT && it.second.type != INPUT_TYPE_FILE -> {
                        (it.first as KMutableProperty1<out Any, *>).setter.call(entity, it.first.getter.call(dto))
                    }
                }
            }
        }
    }

}