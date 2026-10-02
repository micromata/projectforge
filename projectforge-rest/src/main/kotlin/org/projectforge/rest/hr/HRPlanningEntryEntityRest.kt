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

package org.projectforge.rest.hr

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.humanresources.HRPlanningDao
import org.projectforge.business.humanresources.HRPlanningEntryDO
import org.projectforge.business.humanresources.HRPlanningEntryDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.HRPlanningEntry
import org.projectforge.rest.fibu.ProjectChecklistFilter
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListValue
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * The read-only list of the HR planning (menu "HR planning list"), serving the hand-built projectforge-next
 * list page (see components/features/hr-planning). Replaces the retired `HRPlanningListPagesRest` and the
 * removed Wicket `HRPlanningListPage`.
 *
 * A row is an entry of a planned week, not the week itself; a click on it opens the week
 * ([HRPlanningEntityRest]). Entries are never written here: `HRPlanningEntryDao` refuses it, they are saved
 * with their week.
 *
 * The query is the one of `HRPlanningEntryDao.select(HRPlanningFilter)`, built from the filter of the list:
 * the period and the employee of the week, the projects, and the two options of the legacy list
 * (`groupEntries`, `onlyMyProjects`), applied by the DAO itself ([filterList]).
 */
@RestController
@RequestMapping("${Rest.URL}/hrPlanningEntry")
class HRPlanningEntryEntityRest : AbstractDTOEntityRest<HRPlanningEntryDO, HRPlanningEntry, HRPlanningEntryDao>(
    baseDaoClazz = HRPlanningEntryDao::class.java,
    i18nKeyPrefix = "hr.planning.title",
) {
    @Autowired
    private lateinit var hrPlanningDao: HRPlanningDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    /** The sums of the list, shown above its table as the legacy list did ("total duration"). */
    class HRPlanningListStatistics(val totalHours: BigDecimal)

    override fun transformForDB(dto: HRPlanningEntry): HRPlanningEntryDO {
        val entry = HRPlanningEntryDO()
        dto.copyTo(entry)
        return entry
    }

    override fun transformFromDB(obj: HRPlanningEntryDO, editMode: Boolean): HRPlanningEntry {
        return HRPlanningEntry().also { it.copyFrom4ListRow(obj) }
    }

    override fun newDTO(): HRPlanningEntry = HRPlanningEntry()

    /**
     * Whether the user may open (and so edit) the planned weeks, i.e. may write the HR planning at all: the
     * legacy list linked its rows only then.
     */
    private val fullAccess: Boolean
        get() = hrPlanningDao.hasLoggedInUserInsertAccess(null, false)

    override fun listUpdateAccess(): Boolean = fullAccess

    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf("fullAccess" to fullAccess)
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            // A synthetic field (the week is a property of the planning, not of the entry), consumed in
            // preProcessMagicFilter.
            UIFilterElement(PERIOD, filterType = UIFilterElement.FilterType.DATE, label = translate("timePeriod"))
                .also { it.defaultFilter = true }
        )
        elements.add(
            // The employee of the planned week, consumed in preProcessMagicFilter.
            UIFilterObjectElement(
                USER,
                label = translate("timesheet.user"),
                autoCompletion = AutoCompletion.getAutoCompletion4Users(),
            ).also { it.defaultFilter = true }
        )
        if (projektDao.hasLoggedInUserSelectAccess(false)) {
            // As the legacy list: the project filter only for users seeing projects at all.
            elements.add(projectFilter.element())
        }
        elements.add(UIFilterBooleanElement(GROUP_ENTRIES, label = translate("hr.planning.filter.groupEntries")))
        elements.add(UIFilterBooleanElement(ONLY_MY_PROJECTS, label = translate("hr.planning.filter.onlyMyProjects")))
    }

    /**
     * The current week and the logged-in user, as the legacy list started (`HRPlanningListFilter.reset`).
     */
    override fun newMagicFilter(): MagicFilter {
        val filter = super.newMagicFilter()
        val today = PFDay.now()
        filter.entries.add(MagicFilterEntry(PERIOD).also {
            it.value.fromValue = today.beginOfWeek.isoString
            it.value.toValue = today.endOfWeek.isoString
            it.value.periodKind = "week"
        })
        ThreadLocalUserContext.loggedInUser?.let { user ->
            filter.entries.add(MagicFilterEntry(USER).also {
                it.value.id = user.id
                it.value.displayName = user.displayName
            })
        }
        return filter
    }

    private val projectFilter = ProjectChecklistFilter("hrPlanningEntry/projectFilterValues")

    /**
     * The projects to choose from in the project filter: those of the entries the list's *other* criteria match.
     */
    @PostMapping("projectFilterValues")
    fun projectFilterValues(@RequestBody(required = false) filter: MagicFilter?): List<UIFilterListValue> {
        val entries = getResultList(checklistFilter(filter, ProjectChecklistFilter.FIELD))
        return ProjectChecklistFilter.valuesOf(entries.asSequence().map { it.projekt?.id })
    }

    /**
     * Turns the synthetic entries of [addMagicFilterElements] into the criteria `HRPlanningEntryDao.buildQueryFilter`
     * builds: the week of the planning within the period (both ends inclusive) and its employee. The two options
     * are only marked consumed here, [filterList] applies them.
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<HRPlanningEntryDO>>? {
        HRPlanningEntryDao.addListFetchJoins(target)
        projectFilter.addCriterion(target, source)
        source.entries.find { it.field == PERIOD }?.let { entry ->
            entry.synthetic = true
            PFDayUtils.parseDate(entry.value.fromValue)?.let { target.add(QueryFilter.ge("planning.week", it)) }
            PFDayUtils.parseDate(entry.value.toValue)?.let { target.add(QueryFilter.le("planning.week", it)) }
        }
        source.entries.find { it.field == USER }?.let { entry ->
            entry.synthetic = true
            (entry.value.id ?: entry.value.value?.toLongOrNull())?.let { target.add(QueryFilter.eq("planning.user.id", it)) }
        }
        source.entries.filter { it.field == GROUP_ENTRIES || it.field == ONLY_MY_PROJECTS }.forEach { it.synthetic = true }
        return null
    }

    /**
     * An entry of a deleted planned week is as gone as a deleted entry, so the deleted flag is asked of both (as
     * `HRPlanningEntryDao.select` does). And the legacy order unless the user sorts: latest week first.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        when (target.deleted) {
            true -> {
                target.deleted = null
                target.add(QueryFilter.or(QueryFilter.eq("deleted", true), QueryFilter.eq("planning.deleted", true)))
            }

            false -> target.add(QueryFilter.eq("planning.deleted", false))
            null -> {}
        }
        if (target.sortProperties.isEmpty()) {
            target.addOrder(SortProperty.desc("planning.week")).addOrder(SortProperty.asc("planning.user.firstname"))
        }
    }

    override fun filterList(resultSet: MutableList<HRPlanningEntryDO>, filter: MagicFilter): List<HRPlanningEntryDO> {
        baseDao.initializePlanningEntries(resultSet)
        val list = baseDao.groupAndFilter(
            resultSet,
            filter.entries.any { it.field == GROUP_ENTRIES && it.isTrueValue },
            filter.entries.any { it.field == ONLY_MY_PROJECTS && it.isTrueValue },
        )
        return super.filterList(list, filter)
    }

    override fun postProcessResultSet(
        resultSet: ResultSet<HRPlanningEntryDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        // Grouping replaces the entries of a week by one row: the count is the one of the rows shown.
        resultSet.totalSize = resultSet.resultSet.size
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        result.statistics = HRPlanningListStatistics(
            resultSet.resultSet.fold(BigDecimal.ZERO) { sum, entry -> sum.add(entry.totalHours) }
        )
        return result
    }

    companion object {
        private const val PERIOD = "period"
        private const val USER = "user"
        private const val GROUP_ENTRIES = "groupEntries"
        private const val ONLY_MY_PROJECTS = "onlyMyProjects"
    }
}
