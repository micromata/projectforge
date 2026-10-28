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

package org.projectforge.business.task.bookinggrant

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * A task of a booking grant ([TaskBookingGrantDO.positions]): its user may book on exactly this task, on the given
 * cost units only ([kost2Ids]) or, if none are given, on every cost unit of the task.
 */
@Entity
@Table(
    name = "t_task_booking_grant_task",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_t_task_booking_grant_task",
        columnNames = ["grant_id", "task_id"],
    )],
    indexes = [
        jakarta.persistence.Index(name = "idx_fk_t_task_booking_grant_task_grant_id", columnList = "grant_id"),
        jakarta.persistence.Index(name = "idx_fk_t_task_booking_grant_task_task_id", columnList = "task_id"),
    ]
)
open class TaskBookingGrantTaskDO : DefaultBaseDO() {
    @JsonIgnore
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "grant_id", nullable = false)
    open var grant: TaskBookingGrantDO? = null

    @PropertyInfo(i18nKey = "task")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var task: TaskDO? = null

    /**
     * The ids of the cost units the user may book on, comma separated. Null for every cost unit of the task.
     */
    @PropertyInfo(i18nKey = "fibu.kost2")
    @get:Column(name = "kost2_ids", length = 4000)
    open var kost2Ids: String? = null

    /**
     * [kost2Ids] as set, null for every cost unit of the task.
     */
    open var kost2IdSet: Set<Long>?
        @Transient
        get() = parseKost2Ids(kost2Ids)
        set(value) {
            kost2Ids = value?.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")
        }

    companion object {
        fun parseKost2Ids(kost2Ids: String?): Set<Long>? {
            return kost2Ids?.split(',')?.mapNotNull { it.trim().toLongOrNull() }?.toSet()?.takeIf { it.isNotEmpty() }
        }
    }
}
