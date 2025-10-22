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

package org.projectforge.business.availability.model

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
import java.time.LocalDate

/**
 * Represents an employee availability entry (e.g., absent, remote, partial absence).
 * Similar to VacationDO but independent from vacation days calculation.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
@Table(
    name = "t_employee_availability",
    indexes = [
        jakarta.persistence.Index(
            name = "idx_fk_t_availability_employee_id",
            columnList = "employee_id"
        ),
        jakarta.persistence.Index(
            name = "idx_fk_t_availability_replacement_id",
            columnList = "replacement_id"
        )
    ]
)
@NamedQueries(
    NamedQuery(
        name = AvailabilityDO.FIND_CURRENT_AND_FUTURE,
        query = "from AvailabilityDO where endDate>=:endDate and deleted=false",
    ),
)
@NamedEntityGraph(
    name = AvailabilityDO.ENTITY_GRAPH_WITH_OTHER_REPLACEMENTIDS,
    attributeNodes = [NamedAttributeNode(value = "otherReplacements", subgraph = "otherReplacementIds")],
    subgraphs = [NamedSubgraph(
        name = "otherReplacementIds",
        attributeNodes = [NamedAttributeNode(value = "id")]
    )]
)
@AUserRightId(value = "EMPLOYEE_AVAILABILITY", checkAccess = false)
open class AvailabilityDO : DefaultBaseDO() {

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
     * Type of availability (key from AvailabilityTypeConfiguration).
     * Examples: "ABSENT", "REMOTE", "PARTIAL_ABSENCE", "PARENTAL_LEAVE"
     */
    @PropertyInfo(i18nKey = "availability.type")
    @get:Column(name = "availability_type", length = 50, nullable = false)
    open var availabilityType: String? = null

    /**
     * Percentage of absence (e.g., 50 for 50%, 100 for full absence).
     * Only relevant for partial absence types.
     */
    @PropertyInfo(i18nKey = "availability.percentage")
    @get:Column(name = "percentage")
    open var percentage: Int? = null

    /**
     * Coverage (during absence). This is the main responsible colleague for replacement.
     */
    @PropertyInfo(i18nKey = "vacation.replacement")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "replacement_id")
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
        name = "t_employee_availability_other_replacements",
        joinColumns = [JoinColumn(name = "availability_id", referencedColumnName = "PK")],
        inverseJoinColumns = [JoinColumn(name = "employee_id", referencedColumnName = "PK")],
        indexes = [
            jakarta.persistence.Index(
                name = "idx_fk_t_employee_availability_other_replacements_availability_id",
                columnList = "availability_id"
            ),
            jakarta.persistence.Index(
                name = "idx_fk_t_employee_availability_other_replacements_employee_id",
                columnList = "employee_id"
            )
        ]
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

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = 4000)
    open var comment: String? = null

    @Transient
    fun isReplacement(userId: Long?): Boolean {
        val replacementDO = PfCaches.instance.getEmployeeIfNotInitialized(replacement)
        return userId != null && replacementDO?.user?.id == userId
    }

    fun hasOverlap(other: AvailabilityDO): Boolean {
        return Companion.hasOverlap(startDate, endDate, other.startDate, other.endDate)
    }

    fun isInBetween(date: LocalDate): Boolean {
        val start = startDate ?: return false
        val end = endDate ?: return false
        return date in start..end
    }

    companion object {
        internal const val FIND_CURRENT_AND_FUTURE = "AvailabilityDO_FindCurrentAndFuture"

        const val ENTITY_GRAPH_WITH_OTHER_REPLACEMENTIDS = "AvailabilityDO.withOtherReplacementIds"

        private fun hasOverlap(begin1: LocalDate?, end1: LocalDate?, begin2: LocalDate?, end2: LocalDate?): Boolean {
            if (begin1 == null || end1 == null || begin2 == null || end2 == null) {
                return false
            }
            return begin1 <= end2 && end1 >= begin2
        }
    }
}
