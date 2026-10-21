/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.fibu

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.fibu.kost.ProjektKost2Service
import org.projectforge.business.task.TaskTree
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Kost2Art
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Project
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.Kost2FilterUtils
import org.projectforge.ui.filter.UIFilterListValue
import org.projectforge.ui.filter.addLeading
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * The project (Projekt) list and edit page, layout free — its list and form are hand built in
 * projectforge-next (`/next/project`), so this carries no `createListLayout` or `createEditLayout` any
 * more. The list used to be the generic React page, the form the Wicket `ProjektEditPage`; the Wicket
 * project pages have been removed.
 *
 * The form offers the cost 2 types (Kost2-Arten) of the project: a [Kost2DO] is created for each newly
 * checked one after the save ([onAfterSaveOrUpdate]), as the Wicket page did. Unlike there, an existing
 * one may be unchecked, which sets its cost 2 unit non-active (never deleted), and checked again.
 *
 * Project favorites (`UserPrefArea.PROJEKT_FAVORITE`) are deliberately not carried over — the next list
 * offers the generic saved-filter favorites instead.
 */
@RestController
@RequestMapping("${Rest.URL}/project")
class ProjectEntityRest
    : AbstractDTOEntityRest<ProjektDO, Project, ProjektDao>(
    ProjektDao::class.java,
    "fibu.projekt.title"
) {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var projektKost2Service: ProjektKost2Service

    @Autowired
    private lateinit var fibuNumberValidation: FibuNumberValidation

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var taskTree: TaskTree

    @PostConstruct
    private fun postConstruct() {
        // Read-only getters the form posts back unchanged.
        JacksonConfiguration.registerAllowedUnknownProperties(Project::class.java, "statusAsString")
        JacksonConfiguration.registerAllowedUnknownProperties(Kost2Art::class.java, "formattedId")
    }

    /**
     * The edit form (and a new entry) also gets all cost 2 types, the ones the project already has marked
     * [Kost2Art.existsAlready]. The ones with an active cost 2 unit are [Kost2Art.active] and start
     * [Kost2Art.selected] — the checkbox, unchecked deactivates it on save (see [onAfterSaveOrUpdate]).
     * Not for a list row: the list shows [Project.kost2ArtsAsString] instead.
     */
    override fun transformFromDB(obj: ProjektDO, editMode: Boolean): Project {
        val projekt = Project()
        caches.initialize(obj)
        projekt.copyFrom(obj)
        projekt.nummer = baseDao.localNummer(obj)
        if (editMode || obj.id == null) {
            val activeArtIds = kostCache.getKost2ForProjekt(obj.id)
                .filter { projektKost2Service.isActive(it) }
                .mapNotNull { it.kost2Art?.id }
                .toSet()
            projekt.kost2Arts = kostCache.getAllKost2ArtsForProjekt(obj.id).map { art ->
                Kost2Art(
                    id = art.id,
                    name = art.name,
                    fakturiert = art.isFakturiert,
                    projektStandard = art.isProjektStandard,
                    description = art.description,
                ).also {
                    it.existsAlready = art.isExistsAlready
                    it.active = art.isExistsAlready && art.id in activeArtIds
                    it.selected = it.active
                }
            }.sortedBy { it.id }
            projekt.numberLocked = baseDao.isNumberLocked(obj.id)
        }
        return projekt
    }

    override fun transformForDB(dto: Project): ProjektDO {
        val projektDO = ProjektDO()
        dto.copyTo(projektDO)
        return projektDO
    }

    /**
     * The cost 2 types column: the two-digit ids of the project's existing cost 2 units, read from the
     * [KostCache] (no query per row). [Project.kost2Arts] carries the same types with name and
     * whether their unit is [Kost2Art.active], for the cell (a non-active one struck through) and its tooltip.
     */
    override fun createListRow(obj: ProjektDO): Project {
        val dto = transformFromDB(obj, false)
        val kost2s = kostCache.getKost2ForProjekt(obj.id)
            .filter { it.kost2Art?.id != null }
            .sortedBy { it.kost2Art!!.id }
        dto.kost2ArtsAsString = kost2s.joinToString { StringHelper.format2DigitNumber(it.kost2Art!!.id!!) }
        dto.kost2Arts = kost2s.map { kost2 ->
            // The type from the cache: the unit's own reference may be a lazy proxy.
            val art = kostCache.getKost2ArtIfNotInitialized(kost2.kost2Art)
            Kost2Art(id = kost2.kost2Art!!.id, name = art?.name).also {
                it.existsAlready = true
                it.active = projektKost2Service.isActive(kost2)
            }
        }
        dto.lastTimesheetDate = lastTimesheetDate(obj)
        dto.lastOrderDate = lastOrderDate(obj)
        dto.lastActivityDate = lastActivityDate(obj)
        return dto
    }

    /**
     * The activity columns, sorted in memory: both values come from caches ([TaskTree], [AuftragsCache]).
     * And the number column: [ProjektDO.kost] is transient and its text doesn't sort (free numbers of any
     * length, ranges of other segments), so by the project number it shows.
     */
    override val computedSortProperties: Map<String, (ProjektDO) -> Comparable<*>?> = mapOf(
        "kost" to ProjektDO::projektnummer,
        "lastTimesheetDate" to ::lastTimesheetDate,
        "lastOrderDate" to ::lastOrderDate,
        "lastActivityDate" to ::lastActivityDate,
    )

    /** The day (in the user's time zone) of the latest time sheet on the project's task tree. */
    private fun lastTimesheetDate(obj: ProjektDO): LocalDate? {
        val date = taskTree.getLatestTimesheetStopDate(obj.id, obj.task?.id)
        return PFDay.fromOrNull(date)?.localDate
    }

    private fun lastOrderDate(obj: ProjektDO): LocalDate? = auftragsCache.getLatestOrderDate(obj.id)

    /**
     * The later of the last time sheet and the last order date: a project without any recent one has
     * probably ended.
     */
    private fun lastActivityDate(obj: ProjektDO): LocalDate? =
        listOfNotNull(lastTimesheetDate(obj), lastOrderDate(obj)).maxOrNull()

    /**
     * Replaces the auto-detected `status` filter by the list type the Wicket list offered: "not ended"
     * (the default, see [newMagicFilter]) plus every single status. A plain status filter couldn't say "not
     * ended", because most projects have no status at all (`NONE` is persisted as null, see
     * [ProjektDao.onInsertOrModify]). Several values are OR-combined; an empty selection means "all".
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.removeIf { it is UIFilterElement && it.id == STATUS_FIELD }
        val values = mutableListOf(UISelectValue(FILTER_NOT_ENDED, translate("notEnded")))
        ProjektStatus.entries.forEach { status ->
            values.add(UISelectValue(status.name, translate(status.i18nKey)))
        }
        elements.add(
            UIFilterListElement(
                LIST_TYPE_FIELD,
                label = translate("status"),
                values = values,
                multi = true,
                defaultFilter = true,
            )
        )
        // The customer as the list's cell shows it, picked from those of the projects (see
        // customerFilterValues), as on the order list, replacing the free-text pills on the customer's fields.
        elements.removeTextFilters("kunde")
        elements.addLeading(businessUnitFilter.element(), customerFilter.element(), projectFilter.element())
        // The cost 2 types the projects have active, and the gaps (non-active or missing ones); both pinned,
        // consumed in preProcessMagicFilter.
        val kost2Arts = kostCache.getKost2Arts()
        elements.add(
            Kost2FilterUtils.createKost2ArtFilterElement(kost2Arts).also {
                it.label = translate("fibu.projekt.filter.kost2ArtsActive")
                it.tooltip = translate("fibu.projekt.filter.kost2ArtsActive.tooltip")
                it.defaultFilter = true
            }
        )
        elements.add(
            Kost2FilterUtils.createKost2ArtFilterElement(kost2Arts).also {
                it.id = KOST2_ARTS_NOT_ACTIVE_FIELD
                it.key = KOST2_ARTS_NOT_ACTIVE_FIELD
                it.label = translate("fibu.projekt.filter.kost2ArtsNotActive")
                it.tooltip = translate("fibu.projekt.filter.kost2ArtsNotActive.tooltip")
                it.defaultFilter = true
            }
        )
    }

    /**
     * A user without a stored filter starts with the projects not ended, as the former Wicket list
     * did (`ProjektListFilter.reset`).
     */
    override fun newMagicFilter(): MagicFilter {
        val filter = super.newMagicFilter()
        val entry = MagicFilterEntry(LIST_TYPE_FIELD)
        entry.value.values = arrayOf(FILTER_NOT_ENDED)
        filter.entries.add(entry)
        return filter
    }

    /**
     * The status mirrors `ProjektDao.select`: "not ended" matches a null status too, and so does `NONE`.
     */
    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<ProjektDO>>? {
        viaProject.addCriterion(target, source, CustomerChecklistFilter.FIELD, customerFilter::projectMatch)
        viaProject.addCriterion(target, source, BusinessUnitChecklistFilter.FIELD, businessUnitFilter::projectMatch)
        projectFilter.addCriterion(target, source)
        addKost2ArtCriterion(target, source)
        addKost2ArtsNotActiveCriterion(target, source)
        val entry = source.entries.find { it.field == LIST_TYPE_FIELD } ?: return null
        entry.synthetic = true
        val listTypes = entry.value.values?.filter { it.isNotBlank() }.orEmpty()
        val predicates = listTypes.mapNotNull { predicateFor(it) }
        when (predicates.size) {
            0 -> {} // "all": no predicate.
            1 -> target.add(predicates.first())
            else -> target.add(or(*predicates.toTypedArray()))
        }
        return null
    }

    /**
     * The "active cost 2 types" filter: the projects with an active (not deleted) unit of any picked type,
     * resolved from the [KostCache] — a project has no `kost2` path, so `Kost2FilterUtils.preProcessKost2Art`
     * doesn't fit.
     */
    private fun addKost2ArtCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == Kost2FilterUtils.kost2ArtFieldId() } ?: return
        entry.synthetic = true
        val artIds = entry.value.values?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (artIds.isEmpty()) {
            return
        }
        val projektIds = kostCache.getProjektIdsWithActiveKost2Arts(artIds)
        // An empty IN is dropped by DBPredicate.IsIn (would match all), so match no id instead.
        target.add(if (projektIds.isEmpty()) eq("id", -1L) else isIn("id", projektIds))
    }

    /**
     * The "non-active or missing cost 2 types" filter, to find the gaps: the projects without an active (not
     * deleted) unit of at least one picked type — the complement of [addKost2ArtCriterion] per type. Resolved
     * as the cached projects minus those having all picked types active.
     */
    private fun addKost2ArtsNotActiveCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == KOST2_ARTS_NOT_ACTIVE_FIELD } ?: return
        entry.synthetic = true
        val artIds = entry.value.values?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (artIds.isEmpty()) {
            return
        }
        val complete = kostCache.getProjektIdsWithAllActiveKost2Arts(artIds)
        val projektIds = projektCache.all.keys - complete
        // An empty IN is dropped by DBPredicate.IsIn (would match all), so match no id instead.
        target.add(if (projektIds.isEmpty()) eq("id", -1L) else isIn("id", projektIds))
    }

    private fun predicateFor(listType: String): DBPredicate? {
        if (listType == FILTER_NOT_ENDED) {
            return or(ne(STATUS_FIELD, ProjektStatus.ENDED), isNull(STATUS_FIELD))
        }
        val status = ProjektStatus.entries.find { it.name == listType } ?: return null
        return if (status == ProjektStatus.NONE) isNull(STATUS_FIELD) else eq(STATUS_FIELD, status)
    }

    /**
     * The number of a project must be free within its customer (unique constraint of [ProjektDO]) and its
     * project number must fit the configured number ranges and be free system-wide. Checked here as field
     * errors, so the hand built form marks the number instead of failing with the DAO's exception.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Project) {
        super.validate(validationErrors, dto)
        fibuNumberValidation.validateProjectNumbers(
            validationErrors,
            // An unknown customer is no customer, as for the DAO, which loads it by id.
            kundeNummer = dto.customer?.id?.let { caches.getKunde(it) }?.nummer,
            projectId = dto.id,
            nummer = dto.nummer,
            projektnummer = dto.projektnummer,
            internKost2_4 = dto.internKost2_4,
            newKost2Requested = dto.kost2Arts?.any { it.selected && !it.existsAlready } == true,
        )
    }

    /**
     * Brings the project's cost 2 units in line with the cost 2 types checked in the form: a checked type gets
     * an active cost 2 unit, an unchecked one with an active unit is set non-active, never deleted (see
     * [ProjektKost2Service]).
     *
     * Nothing is changed for an ended project (the status as saved, also if it was ended with this save): its
     * cost 2 units are ended anyway, the form shows them read-only.
     */
    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: ProjektDO, postData: PostData<Project>) {
        super.onAfterSaveOrUpdate(request, obj, postData)
        if (obj.status == ProjektStatus.ENDED) {
            return
        }
        val projektId = obj.id ?: return
        val (selected, unselected) = postData.data.kost2Arts.orEmpty()
            .filter { it.id != null }
            .partition { it.selected }
        projektKost2Service.activate(projektId, selected.map { it.id!! })
        projektKost2Service.deactivate(projektId, unselected.map { it.id!! })
    }

    override val autoCompleteSearchFields = arrayOf("name", "identifier")

    /** Ended projects only on request ([org.projectforge.ui.AutoCompletion.SHOW_INACTIVE_PARAM]). */
    override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<ProjektDO> {
        val list = super.queryAutocompleteObjects(request, filter)
        return if (showInactive(request)) list else list.filter { !isInactiveAutoCompleteEntry(it) }
    }

    override fun isInactiveAutoCompleteEntry(obj: ProjektDO): Boolean =
        super.isInactiveAutoCompleteEntry(obj) || obj.status == ProjektStatus.ENDED

    /**
     * The customers to choose from in the customer filter ([CustomerChecklistFilter]): those of the projects
     * the list's *other* criteria in [filter] match (see [checklistFilter]).
     */
    @AccessChecked("DAO: select access (checklistFilter + getResultList)")
    @PostMapping("customerFilterValues")
    fun customerFilterValues(@RequestBody(required = false) filter: MagicFilter?): List<UIFilterListValue> {
        return CustomerChecklistFilter.valuesOf(customerRefs(filter, CustomerChecklistFilter.FIELD))
    }

    /** The business units to choose from ([BusinessUnitChecklistFilter]), as [customerFilterValues]. */
    @AccessChecked("DAO: select access (checklistFilter + getResultList)")
    @PostMapping("businessUnitFilterValues")
    fun businessUnitFilterValues(@RequestBody(required = false) filter: MagicFilter?): List<UIFilterListValue> {
        return businessUnitFilter.valuesOfProjects(
            getResultIds(checklistFilter(filter, BusinessUnitChecklistFilter.FIELD)).asSequence()
        )
    }

    /**
     * The projects to choose from in the project filter ([ProjectChecklistFilter]), as [customerFilterValues]:
     * mainly for picking a project group, which then lists its projects.
     */
    @AccessChecked("DAO: select access (checklistFilter + getResultList)")
    @PostMapping("projectFilterValues")
    fun projectFilterValues(@RequestBody(required = false) filter: MagicFilter?): List<UIFilterListValue> {
        return ProjectChecklistFilter.valuesOf(getResultIds(checklistFilter(filter, ProjectChecklistFilter.FIELD)).asSequence())
    }

    /** The customers of the projects of a checklist: their own, else those of their orders ([projectCustomersOf]). */
    private fun customerRefs(filter: MagicFilter?, ownField: String): Sequence<CustomerRow> {
        val projektIds = getResultIds(checklistFilter(filter, ownField))
        val projectCustomers = projectCustomersOf(projektCache.all, caches)
        return projektIds.asSequence().flatMap { customerRowsOf(it, projectCustomers) }
    }

    /**
     * A project without a customer entity is matched by the customers of its orders, so the customer and
     * business-unit picks are resolved to the projects in memory (see [ViaProjectCriteria]).
     */
    private val viaProject by lazy {
        ViaProjectCriteria(
            path = "id",
            nullPath = null,
            projects = { projectCustomersOf(projektCache.all, caches) },
            idsOf = { it },
            idsWithoutProject = { emptyList() },
        )
    }

    companion object {
        private const val STATUS_FIELD = "status"

        /** The id of the synthetic status filter element, consumed in [preProcessMagicFilter]. */
        const val LIST_TYPE_FIELD = "listType"

        private const val FILTER_NOT_ENDED = "notEnded"

        /** The id of the synthetic "non-active or missing cost 2 types" filter element, see [addKost2ArtsNotActiveCriterion]. */
        const val KOST2_ARTS_NOT_ACTIVE_FIELD = "kost2ArtsNotActive"

        /** Matched via [viaProject]: the paths only serve the element. */
        private val customerFilter = CustomerChecklistFilter("project/customerFilterValues", kundeTextPath = null)
        private val businessUnitFilter =
            BusinessUnitChecklistFilter("project/businessUnitFilterValues", kundeTextPath = null, projektIdPath = "id")
        private val projectFilter = ProjectChecklistFilter("project/projectFilterValues", path = "id")
    }
}
