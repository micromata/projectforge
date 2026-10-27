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

package org.projectforge.business.fibu.kost.reallocation

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.util.Date

/**
 * A reallocation run: accounting records of a period are reallocated from collective cost units (kost2) to a target
 * project. The original kost2 of a record is kept, the reallocated assignment is stored as
 * [BuchungssatzKost2ShareDO]. The time sheets behind the salary records are moved to the target project when the run
 * is applied (logged as [Kost2ReallocationTimesheetDO]).
 */
@Entity
@Indexed
@Table(
    name = "t_fibu_kost2_reallocation",
    indexes = [Index(name = "idx_fk_t_fibu_kost2_reallocation_projekt_fk", columnList = "projekt_fk")]
)
open class Kost2ReallocationDO : DefaultBaseDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = description ?: ""

    @PropertyInfo(i18nKey = "description", required = true)
    @FullTextField
    @get:Column(length = 1000, nullable = false)
    open var description: String? = null

    /** The target project. */
    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.targetProject", required = true)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projekt_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var projekt: ProjektDO? = null

    /** The target task of the time sheets selected by hand (booked directly on a collective task). */
    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.targetTask")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var task: TaskDO? = null

    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.fromYear", required = true)
    @GenericField
    @get:Column(name = "from_year", nullable = false)
    open var fromYear: Int? = null

    /** 1-based. */
    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.fromMonth", required = true)
    @get:Column(name = "from_month", nullable = false)
    open var fromMonth: Int? = null

    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.toYear", required = true)
    @GenericField
    @get:Column(name = "to_year", nullable = false)
    open var toYear: Int? = null

    /** 1-based. */
    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.toMonth", required = true)
    @get:Column(name = "to_month", nullable = false)
    open var toMonth: Int? = null

    @PropertyInfo(i18nKey = "status")
    @GenericField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: Kost2ReallocationStatus = Kost2ReallocationStatus.DRAFT

    @PropertyInfo(i18nKey = "fibu.kost2Reallocation.appliedAt")
    @get:Column(name = "applied_at")
    open var appliedAt: Date? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = 4000)
    open var comment: String? = null

    /** year * 100 + month of the first month of the period. */
    val fromYearMonth: Int?
        @Transient
        get() = yearMonth(fromYear, fromMonth)

    /** year * 100 + month of the last month of the period. */
    val toYearMonth: Int?
        @Transient
        get() = yearMonth(toYear, toMonth)

    /** Is the given month (1-based) inside the period of the run? */
    fun isInPeriod(year: Int?, month: Int?): Boolean {
        val value = yearMonth(year, month) ?: return false
        val from = fromYearMonth ?: return false
        val to = toYearMonth ?: return false
        return value in from..to
    }

    companion object {
        fun yearMonth(year: Int?, month: Int?): Int? {
            return if (year == null || month == null) null else year * 100 + month
        }
    }
}
