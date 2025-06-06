/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.json.IdsOnlySerializer
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.LocalDate

/**
 * Repräsentiert einen Urlaub. Ein Urlaub ist einem ProjectForge-Mitarbeiter zugeordnet und enthält buchhalterische
 * Angaben.
 *
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@Table(
    name = "t_employee_vacation",
    indexes = [jakarta.persistence.Index(
        name = "idx_fk_t_vacation_employee_id",
        columnList = "employee_id"
    ), jakarta.persistence.Index(name = "idx_fk_t_vacation_manager_id", columnList = "manager_id")]
)
@NamedQueries(
    NamedQuery(
        name = VacationDO.FIND_CURRENT_AND_FUTURE,
        query = "from VacationDO where endDate>=:endDate and status in :statusList and deleted=false",
    ),
)
@NamedEntityGraph(
    name = VacationDO.ENTITY_GRAPH_WITH_OTHER_REPLACEMENTIDS,
    attributeNodes = [NamedAttributeNode(value = "otherReplacements", subgraph = "otherReplacementIds")],
    subgraphs = [NamedSubgraph(
        name = "otherReplacementIds",
        attributeNodes = [NamedAttributeNode(value = "id")]
    )]
)
@AUserRightId(value = "EMPLOYEE_VACATION", checkAccess = false)
open class VacationDO : DefaultBaseDO() {

    /**
     * The employee.
     */
    @PropertyInfo(i18nKey = "vacation.employee")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var employee: EmployeeDO? = null

    @PropertyInfo(i18nKey = "vacation.startdate")
    @get:Column(name = "start_date", nullable = false)
    open var startDate: LocalDate? = null

    @PropertyInfo(i18nKey = "vacation.enddate")
    @get:Column(name = "end_date", nullable = false)
    open var endDate: LocalDate? = null

    /**
     * Coverage (during leave). This is the main responsible colleague for replacement.
     */
    @PropertyInfo(i18nKey = "vacation.replacement")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "replacement_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var replacement: EmployeeDO? = null

    /**
     * Other employees as substitutes.
     */
    @PropertyInfo(i18nKey = "vacation.replacement.others")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToMany(fetch = FetchType.LAZY)
    @get:JoinTable(
        name = "t_employee_vacation_other_replacements",
        joinColumns = [JoinColumn(name = "vacation_id", referencedColumnName = "PK")],
        inverseJoinColumns = [JoinColumn(name = "employee_id", referencedColumnName = "PK")],
        indexes = [jakarta.persistence.Index(
            name = "idx_fk_t_employee_vacation_other_replacements_vacation_id", columnList = "vacation_id",
        ), jakarta.persistence.Index(
            name = "idx_fk_t_employee_vacation_other_replacements_employee_id",
            columnList = "employee_id",
        )]
    )
    @JsonSerialize(using = IdsOnlySerializer::class)
    open var otherReplacements: MutableSet<EmployeeDO>? = null

    /**
     * Will fetch all other replacements (lazy loading)!!!
     */
    open val allReplacements: Collection<EmployeeDO>
        @Transient
        get() {
            val result = mutableListOf<EmployeeDO>()
            replacement?.let {
                result.add(it)
            }
            otherReplacements?.forEach { other ->
                if (result.none { other.id == it.id }) {
                    result.add(other)
                }
            }
            return result
        }

    /**
     * The manager.
     */
    @PropertyInfo(i18nKey = "vacation.manager")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "manager_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var manager: EmployeeDO? = null

    @PropertyInfo(i18nKey = "vacation.status")
    open var status: VacationStatus? = null
        @Enumerated(EnumType.STRING)
        @Column(name = "vacation_status", length = 30, nullable = false) get() = if (field == null) {
            VacationStatus.IN_PROGRESS
        } else field

    // Needed by Wicket in VacationListPage (could be removed after migration to ReactJS).
    @PropertyInfo(i18nKey = "vacation.vacationmode")
    private val vacationmode: VacationMode? = null

    @PropertyInfo(i18nKey = "vacation.special", tooltip = "vacation.special.tooltip")
    @get:Column(name = "is_special", nullable = false)
    open var special: Boolean? = null

    @PropertyInfo(i18nKey = "vacation.halfDayBegin", tooltip = "vacation.halfDayBegin.tooltip")
    @get:Column(name = "is_half_day_begin")
    open var halfDayBegin: Boolean? = null

    @PropertyInfo(i18nKey = "vacation.halfDayEnd", tooltip = "vacation.halfDayEnd.tooltip")
    @get:Column(name = "is_half_day_end")
    open var halfDayEnd: Boolean? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = 4000)
    open var comment: String? = null

    @Transient
    fun getVacationmode(): VacationMode {
        val currentUserId = ThreadLocalUserContext.requiredLoggedInUserId
        val employeeDO = PfCaches.instance.getEmployeeIfNotInitialized(employee)
        val employeeUserId = employeeDO?.user?.id
        val managerUserDO = PfCaches.instance.getEmployeeIfNotInitialized(manager)
        val managerUserId = managerUserDO?.user?.id
        if (currentUserId == employeeUserId) {
            return VacationMode.OWN
        }
        if (currentUserId == managerUserId) {
            return VacationMode.MANAGER
        }
        return if (isReplacement(currentUserId)) {
            VacationMode.REPLACEMENT
        } else VacationMode.OTHER
    }

    @Transient
    fun isReplacement(userId: Long?): Boolean {
        val replacementDO = PfCaches.instance.getEmployeeIfNotInitialized(replacement)
        return userId != null && replacementDO?.user?.id == userId
    }

    fun hasOverlap(other: VacationDO): Boolean {
        return Companion.hasOverlap(startDate, endDate, other.startDate, other.endDate)
    }

    fun isInBetween(date: LocalDate): Boolean {
        val start = startDate ?: return false
        val end = endDate ?: return false
        return date in start..end
    }

    companion object {
        internal const val FIND_CURRENT_AND_FUTURE = "VacationDO_FindCurrentAndFuture"

        const val ENTITY_GRAPH_WITH_OTHER_REPLACEMENTIDS = "VacationDO.withOtherReplacementIds"

        private fun hasOverlap(begin1: LocalDate?, end1: LocalDate?, begin2: LocalDate?, end2: LocalDate?): Boolean {
            if (begin1 == null || end1 == null || begin2 == null || end2 == null) {
                return false
            }
            return begin1 <= end2 && end1 >= begin1
        }
    }
}
