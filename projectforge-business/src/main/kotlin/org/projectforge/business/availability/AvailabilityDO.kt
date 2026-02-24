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

package org.projectforge.business.availability

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.time.LocalDate

/**
 * Represents an availability entry for an employee. An availability entry defines the time period
 * and the type of availability (e.g., parental leave, sick leave, workation).
 */
@Entity
@Indexed
@Table(
    name = "t_employee_availability",
    indexes = [jakarta.persistence.Index(
        name = "idx_fk_t_employee_availability_employee_id",
        columnList = "employee_id"
    )]
)
@NamedQueries(
    NamedQuery(
        name = AvailabilityDO.FIND_CURRENT_AND_FUTURE,
        query = "from AvailabilityDO where endDate>=:endDate and deleted=false",
    ),
)
@AUserRightId(value = "EMPLOYEE_AVAILABILITY", checkAccess = false)
open class AvailabilityDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "availability.employee")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var employee: EmployeeDO? = null

    @PropertyInfo(i18nKey = "availability.startDate")
    @get:Column(name = "start_date", nullable = false)
    open var startDate: LocalDate? = null

    @PropertyInfo(i18nKey = "availability.endDate")
    @get:Column(name = "end_date", nullable = false)
    open var endDate: LocalDate? = null

    @PropertyInfo(i18nKey = "availability.type")
    @FullTextField
    @get:Column(name = "type", length = 100)
    open var type: String? = null

    @PropertyInfo(i18nKey = "availability.status")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "availability_status", length = 30)
    open var status: AvailabilityStatus? = null

    @PropertyInfo(i18nKey = "availability.location")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "location", length = 30)
    open var location: AvailabilityLocation? = null

    @PropertyInfo(i18nKey = "availability.description")
    @FullTextField
    @get:Column(name = "description", length = 4000)
    open var description: String? = null

    companion object {
        internal const val FIND_CURRENT_AND_FUTURE = "AvailabilityDO_FindCurrentAndFuture"
    }
}
