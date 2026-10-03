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
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
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
import org.projectforge.ui.filter.UIFilterListValue
import org.projectforge.ui.filter.addLeading
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The project (Projekt) list and edit page, layout free — its list and form are hand built in
 * projectforge-next (`/next/project`), so this carries no `createListLayout` or `createEditLayout` any
 * more. The list used to be the generic React page, the form the Wicket `ProjektEditPage`; that one is
 * still reachable as the way back and writes through the same [ProjektDao].
 *
 * The form offers the cost 2 types (Kost2-Arten) of the project: a [Kost2DO] is created for each newly
 * checked one after the save ([onAfterSaveOrUpdate]), as the Wicket page does. Unlike there, an existing
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
    private lateinit var kost2Dao: Kost2Dao

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
        if (editMode || obj.id == null) {
            val activeArtIds = kostCache.getKost2ForProjekt(obj.id)
                .filter { isActive(it) }
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
     * [KostCache] (no query per row).
     */
    override fun createListRow(obj: ProjektDO): Project {
        val dto = transformFromDB(obj, false)
        dto.kost2ArtsAsString = kostCache.getKost2ArtsForProjekt(obj.id)
            .mapNotNull { it.id }
            .sorted()
            .joinToString { StringHelper.format2DigitNumber(it) }
        return dto
    }

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
        elements.addLeading(businessUnitFilter.element(), customerFilter.element())
    }

    /**
     * A user without a stored filter starts with the projects not ended, as the Wicket list did
     * (`ProjektListFilter.reset`).
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

    private fun predicateFor(listType: String): DBPredicate? {
        if (listType == FILTER_NOT_ENDED) {
            return or(ne(STATUS_FIELD, ProjektStatus.ENDED), isNull(STATUS_FIELD))
        }
        val status = ProjektStatus.entries.find { it.name == listType } ?: return null
        return if (status == ProjektStatus.NONE) isNull(STATUS_FIELD) else eq(STATUS_FIELD, status)
    }

    /**
     * The number of a project must be free within its customer, or within its internal range (4.xxx) for a
     * project without customer — both are unique constraints of [ProjektDO]. Checked here as a field error,
     * so the hand built form marks the number instead of failing with the database's exception. Wicket's
     * `ProjektEditForm` checks the customer case only.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Project) {
        super.validate(validationErrors, dto)
        val kundeId = dto.customer?.id
        // The form shows number and customer read-only then; this catches a stale form or a direct REST
        // call as a field error rather than ProjektDao.onUpdate's exception.
        val stored = dto.id?.let { caches.getProjekt(it) }
        if (stored != null && baseDao.isNumberLocked(stored.id) && isNumberChanged(dto, stored)) {
            validationErrors.add(
                ValidationError(
                    translate(ProjektDao.NUMBER_LOCKED_I18N_KEY),
                    fieldId = ProjektDO::nummer.name,
                )
            )
            return
        }
        val other = if (kundeId != null) {
            caches.getKunde(kundeId)?.let { baseDao.getProjekt(it, dto.nummer.toLong()) }
        } else {
            dto.internKost2_4?.let { baseDao.getProjekt(it, dto.nummer) }
        }
        if (other != null && other.id != dto.id) {
            validationErrors.add(
                ValidationError(
                    translate("fibu.projekt.validation.numbernotfreeforcustomer"),
                    fieldId = ProjektDO::nummer.name,
                )
            )
        }
        // A cost 2 unit takes its range (bereich) from the project: the customer's number, or the internal
        // range. Without either, no cost 2 unit can be created (Kost2Dao.setProjekt).
        if (kundeId == null && dto.internKost2_4 == null && dto.kost2Arts?.any { it.selected && !it.existsAlready } == true) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.fieldRequired", translate("fibu.projekt.internKost2_4")),
                    fieldId = ProjektDO::internKost2_4.name,
                )
            )
        }
    }

    /**
     * Customer (or the internal range of a project without one) and number, as they make up the cost 2
     * numbers. `internKost2_4` of a customer project is dropped on save (ProjektDao.onInsertOrModify).
     */
    private fun isNumberChanged(dto: Project, stored: ProjektDO): Boolean {
        val kundeId = dto.customer?.id
        // Via the cache: the kunde of a cached ProjektDO may be a lazy proxy (see Project.copyFromMinimal).
        val storedKundeId = caches.getKundeIfNotInitialized(stored.kunde)?.nummer
        return kundeId != storedKundeId || dto.nummer != stored.nummer ||
                (kundeId == null && dto.internKost2_4 != stored.internKost2_4)
    }

    /**
     * Brings the project's cost 2 units in line with the cost 2 types checked in the form:
     * - a checked type gets an active cost 2 unit: a new one (see `ProjektEditPage.afterSaveOrUpdate`), a
     *   deleted one is undeleted (inserting would collide with its number), a non-active or ended one is
     *   activated again;
     * - an unchecked type with an active cost 2 unit is set non-active. It is never deleted: the time sheets
     *   and invoices booked on it keep it, only new time sheets can't be booked on it any more
     *   ([KostCache.getActiveKost2]).
     *
     * Read from the cache but written through [Kost2Dao] on a freshly loaded object (rights, history, cache).
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
        val kost2ByArtId = kostCache.getKost2ForProjekt(projektId, includeDeleted = true)
            .filter { it.kost2Art?.id != null }
            // A deleted unit only counts if there is no other one of the same type (there shouldn't be).
            .sortedBy { !it.deleted }
            .associateBy { it.kost2Art!!.id!! }
        postData.data.kost2Arts?.forEach { art ->
            val artId = art.id ?: return@forEach
            val cached = kost2ByArtId[artId]
            if (art.selected) {
                if (cached == null) {
                    val kost2 = Kost2DO()
                    kost2Dao.setProjekt(kost2, projektId)
                    kost2Dao.setKost2Art(kost2, artId)
                    kost2Dao.insert(kost2)
                    return@forEach
                }
                val kost2 = kost2Dao.find(cached.id) ?: return@forEach
                val activate = !isActive(kost2)
                if (activate) {
                    kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE
                }
                if (kost2.deleted) {
                    kost2Dao.undelete(kost2) // Takes the status change along.
                } else if (activate) {
                    kost2Dao.update(kost2)
                }
            } else if (cached != null && !cached.deleted && isActive(cached)) {
                val kost2 = kost2Dao.find(cached.id) ?: return@forEach
                kost2.kostentraegerStatus = KostentraegerStatus.NONACTIVE
                kost2Dao.update(kost2)
            }
        }
    }

    /** The cost 2 unit's own status, not the effective one (an ended project ends all its units anyway). */
    private fun isActive(kost2: Kost2DO): Boolean =
        kost2.kostentraegerStatus == null || kost2.kostentraegerStatus == KostentraegerStatus.ACTIVE

    override val autoCompleteSearchFields = arrayOf("name", "identifier")

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

        /** Matched via [viaProject]: the paths only serve the element. */
        private val customerFilter = CustomerChecklistFilter("project/customerFilterValues", kundeTextPath = null)
        private val businessUnitFilter =
            BusinessUnitChecklistFilter("project/businessUnitFilterValues", kundeTextPath = null, projektIdPath = "id")
    }
}
