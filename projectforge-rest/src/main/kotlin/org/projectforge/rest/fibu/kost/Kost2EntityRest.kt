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

package org.projectforge.rest.fibu.kost

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Customer
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.dto.Kost2Art
import org.projectforge.rest.dto.Project
import org.projectforge.rest.fibu.BusinessUnitChecklistFilter
import org.projectforge.rest.fibu.CustomerChecklistFilter
import org.projectforge.rest.fibu.CustomerRow
import org.projectforge.rest.fibu.ProjectChecklistFilter
import org.projectforge.rest.fibu.ViaProjectCriteria
import org.projectforge.rest.fibu.customerRowsOf
import org.projectforge.rest.fibu.projectCustomersOf
import org.projectforge.rest.fibu.removeTextFilters
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.KostStatusFilterUtils
import org.projectforge.ui.filter.UIFilterListValue
import org.projectforge.ui.filter.addLeading
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/cost2")
class Kost2EntityRest : AbstractDTOEntityRest<Kost2DO, Kost2, Kost2Dao>(Kost2Dao::class.java, "fibu.kost2.title") {
    @Autowired
    private lateinit var kostFormatter: KostFormatter

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var projektCache: ProjektCache

    override fun transformFromDB(obj: Kost2DO, editMode: Boolean): Kost2 {
        // Resolve the lazy references from the caches rather than from the row itself. A list of cost
        // units would otherwise fire one select per row for its Kost2Art and its project (and the
        // project's customer) - the N+1 that showed as a wall of identical T_FIBU_KOST2ART and
        // T_FIBU_PROJEKT queries. The caches hand out fully initialized, detached instances.
        obj.kost2Art = caches.getKost2ArtIfNotInitialized(obj.kost2Art)
        obj.projekt = caches.getProjektIfNotInitialized(obj.projekt)
        val kost2 = Kost2()
        kost2.copyFrom(obj)
        kost2.displayName = KostFormatter.instance.formatKost2(obj, KostFormatter.FormatType.TEXT)
        obj.projekt?.let { projekt ->
            val project = Project()
            project.copyFrom(projekt)
            projekt.kunde?.let { kunde ->
                project.customer = Customer()
                project.customer!!.copyFrom(kunde)
            }
            kost2.project = project
        }
        obj.kost2Art?.let { art ->
            val kost2Art = Kost2Art()
            kost2Art.copyFrom(art)
            kost2.kost2Art = kost2Art
        }
        return kost2
    }

    override fun transformForDB(dto: Kost2): Kost2DO {
        val kost2DO = Kost2DO()
        dto.copyTo(kost2DO)
        return kost2DO
    }

    /**
     * The list shows the *effective* status: a cost 2 rarely sets its own [Kost2DO.kostentraegerStatus]
     * (it stays null and is inherited from the project), so the raw column the list binds to would be
     * empty for almost every row. The status column is one generated field (`kostentraegerStatus`) with
     * the enum's translated labels, so mapping the effective value onto it here keeps that translation
     * and needs no extra column. Only for the list: the edit page keeps the own status, which
     * [transformFromDB] leaves untouched.
     */
    override fun createListRow(obj: Kost2DO): Kost2 {
        val dto = transformFromDB(obj, false)
        dto.kostentraegerStatus = dto.effectiveKostentraegerStatus
        return dto
    }

    /**
     * Adds the status ("list type") filter the legacy cost 2 list offered (all / active / nonactive /
     * notEnded / ended). See [KostStatusFilterUtils].
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        KostStatusFilterUtils.addFilterElement(elements)
        // The customer and the project as the list's cells show them, picked from those of the cost 2 (see
        // customerFilterValues), as on the order list, replacing the free-text pills on the project's fields.
        elements.removeTextFilters("projekt")
        elements.addLeading(businessUnitFilter.element(), customerFilter.element(), projectFilter.element())
    }

    /**
     * The picked status filters on the *computed* [Kost2DO.effectiveKostentraegerStatus], which is no
     * column and so can't be a query predicate; a [CustomResultFilter] tests it per row (mirrors
     * `Kost2Dao.select`).
     */
    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<Kost2DO>>? {
        viaProject.addCriterion(target, source, CustomerChecklistFilter.FIELD, customerFilter::projectMatch)
        viaProject.addCriterion(target, source, BusinessUnitChecklistFilter.FIELD, businessUnitFilter::projectMatch)
        projectFilter.addCriterion(target, source)
        val listTypes = KostStatusFilterUtils.consumeListTypes(source)
        if (listTypes.isEmpty()) {
            return null
        }
        return listOf(Kost2StatusResultFilter(listTypes))
    }

    /**
     * Sorts by the cost number the list shows, which no database column holds: [Kost2DO.formattedNumber]
     * is a getter over the number fields, so the criteria query can't order by it. Its parts in order are
     * exactly that sort — and for cost 2 the last part is the [org.projectforge.business.fibu.kost.Kost2ArtDO]
     * (its id), not a plain `endziffer` as for cost 1, so the join `Kost2Dao.select` uses is needed here too.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        val index = target.sortProperties.indexOfFirst { it.property == "formattedNumber" }
        if (index < 0) {
            return
        }
        val sortOrder = target.sortProperties[index].sortOrder
        target.sortProperties.removeAt(index)
        target.sortProperties.addAll(
            index,
            NUMBER_PROPERTIES.map { SortProperty(it, sortOrder) },
        )
        target.createJoin("kost2Art")
    }

    /**
     * @param request `onlyActiveEntries` (default true) and `projektId`: the latter narrows the answer to
     * the cost units of one project, which the kost2 picker of the task form needs — the units offered
     * there are the ones the task's project has (Wicket prefilters the picker with the search string
     * `"nummer:" + projekt.kost + ".*"`, `TaskEditForm`; the id keeps the number format out of the url and
     * needs no re-parsing here).
     *
     * `siblingsOf` (a cost 2 id) answers an empty term with the *siblings* of that unit instead of the first
     * slice of all units, which practically never fits: the units sharing its number range, area and number
     * (`x.xxx.xx`, everything but the Kost2Art) — what a user re-opening the picker of a chosen unit is
     * looking for (cost assignments of invoices). A typed term searches as usual.
     */
    override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<Kost2DO> {
        val onlyActiveEntries = request.getParameter("onlyActiveEntries")?.toBooleanStrictOrNull() ?: true
        val projektId = NumberHelper.parseLong(request.getParameter("projektId"))
        val siblingsOf = NumberHelper.parseLong(request.getParameter("siblingsOf"))
        val searchString = filter.searchString?.replace(Regex("[*+]"), "")?.trim()
        if (siblingsOf != null && searchString.isNullOrEmpty()) {
            querySiblings(siblingsOf, onlyActiveEntries)?.let { return it }
        }
        var list = super.queryAutocompleteObjects(request, filter)
        if (onlyActiveEntries && !NumberHelper.isDigitsAndDotsOnly(searchString)) {
            list = list.filter { it.isActive() }
        }
        if (projektId != null) {
            list = list.filter { it.projekt?.id == projektId }
        }
        list.forEach { it.displayName = kostFormatter.formatKost2(it, KostFormatter.FormatType.LONG) }
        return list.sortedBy { it.displayName }
    }

    /**
     * The units sharing number range, area and number with the given one, the given one included even if
     * it is no longer active (it is the current value). Loaded from the database rather than taken from
     * [org.projectforge.business.fibu.kost.KostCache]: the display name set here must not leak into the
     * cached instances. Null for an unknown id, which then falls back to the usual search.
     */
    private fun querySiblings(kost2Id: Long, onlyActiveEntries: Boolean): List<Kost2DO>? {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val kost2 = caches.getKost2(kost2Id) ?: return null
        return baseDao.getActiveKost2(kost2.nummernkreis, kost2.bereich, kost2.teilbereich)
            .filter { !it.deleted || it.id == kost2Id }
            .filter { !onlyActiveEntries || it.id == kost2Id || it.isActive() }
            .onEach { it.displayName = kostFormatter.formatKost2(it, KostFormatter.FormatType.LONG) }
            .sortedBy { it.displayName }
    }

    private fun Kost2DO.isActive() =
        effectiveKostentraegerStatus == null || effectiveKostentraegerStatus == KostentraegerStatus.ACTIVE

    override val autoCompleteSearchFields =
        arrayOf("description", "nummer", "rawNumberString", "projekt.name", "projekt.kunde.name")

    /**
     * The customers to choose from in the customer filter ([CustomerChecklistFilter]): those of the projects
     * of the cost 2 the list's *other* criteria in [filter] match (see [checklistFilter]).
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
        return businessUnitFilter.valuesOfProjects(projektIdsOf(filter, BusinessUnitChecklistFilter.FIELD))
    }

    /** The customers of the projects of the cost 2 of a checklist (see [projectCustomersOf]). */
    private fun customerRefs(filter: MagicFilter?, ownField: String): Sequence<CustomerRow> {
        val projectCustomers = projectCustomersOf(projektCache.all, caches)
        return projektIdsOf(filter, ownField).flatMap { customerRowsOf(it, projectCustomers) }
    }

    private fun projektIdsOf(filter: MagicFilter?, ownField: String): Sequence<Long?> =
        getResultDistinct(checklistFilter(filter, ownField), "projekt.id").asSequence().map { it as Long? }

    /**
     * A project without a customer entity is matched by the customers of its orders, so the customer and
     * business-unit picks are resolved to the projects in memory (see [ViaProjectCriteria]).
     */
    private val viaProject by lazy {
        ViaProjectCriteria(
            path = "projekt.id",
            nullPath = "projekt",
            projects = { projectCustomersOf(projektCache.all, caches) },
            idsOf = { it },
            idsWithoutProject = { emptyList() },
        )
    }

    /** The projects to choose from in the project filter ([ProjectChecklistFilter]), as [customerFilterValues]. */
    @AccessChecked("DAO: select access (checklistFilter + getResultList)")
    @PostMapping("projectFilterValues")
    fun projectFilterValues(@RequestBody(required = false) filter: MagicFilter?): List<UIFilterListValue> {
        return ProjectChecklistFilter.valuesOf(projektIdsOf(filter, ProjectChecklistFilter.FIELD))
    }

    private class Kost2StatusResultFilter(private val listTypes: List<String>) : CustomResultFilter<Kost2DO> {
        override fun match(list: MutableList<Kost2DO>, element: Kost2DO): Boolean =
            KostStatusFilterUtils.matchesKost2(listTypes, element.effectiveKostentraegerStatus)
    }

    companion object {
        /** The customers of a cost 2 are its project's, matched via [viaProject]: the paths only serve the element. */
        private val customerFilter =
            CustomerChecklistFilter("cost2/customerFilterValues", kundePath = "projekt.kunde", kundeTextPath = null)
        private val businessUnitFilter = BusinessUnitChecklistFilter(
            "cost2/businessUnitFilterValues", kundePath = "projekt.kunde", kundeTextPath = null,
        )
        private val projectFilter = ProjectChecklistFilter("cost2/projectFilterValues")

        /** The parts of the cost number, most significant first — [Kost2DO.formattedNumber] in columns. */
        private val NUMBER_PROPERTIES = listOf("nummernkreis", "bereich", "teilbereich", "kost2Art.id")
    }
}
