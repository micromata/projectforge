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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.PersistenceBehavior
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

/**
 * An ad hoc booking grant: the given user may book own time sheets on the tasks of its [positions] (each exactly
 * this task, not its descendants, optionally on some of its cost units only) within the period, without being member
 * of a group with access to them. For a colleague helping out for a short time, who mustn't see anything else of the
 * project (see [TaskBookingGrantCache]).
 *
 * Granted by somebody who may edit the time sheets of others on all of its tasks (project management, finance), and
 * active right away. Who granted is the user of its insert in the history (see
 * [TaskBookingGrantDao.selectGrantedByUserIds]). A revoked grant is marked as deleted: the user keeps reading the own
 * time sheets booked by it, but books no more.
 */
@Entity
@Table(
    name = "t_task_booking_grant",
    indexes = [
        jakarta.persistence.Index(name = "idx_fk_t_task_booking_grant_user_id", columnList = "user_id"),
    ]
)
open class TaskBookingGrantDO : DefaultBaseDO() {
    @PropertyInfo(i18nKey = "user")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    @PropertyInfo(i18nKey = "task.bookingGrant.validFrom")
    @get:Column(name = "valid_from", nullable = false)
    open var validFrom: LocalDate? = null

    @PropertyInfo(i18nKey = "task.bookingGrant.validUntil")
    @get:Column(name = "valid_until", nullable = false)
    open var validUntil: LocalDate? = null

    @PropertyInfo(i18nKey = "comment")
    @get:Column(name = "comment", length = 4000)
    open var comment: String? = null

    /**
     * The tasks the user may book on, at least one.
     */
    @PropertyInfo(i18nKey = "task.bookingGrant.positions")
    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        fetch = FetchType.LAZY, mappedBy = "grant",
    )
    @get:OrderBy("id")
    open var positions: MutableList<TaskBookingGrantTaskDO>? = null

    fun addPosition(position: TaskBookingGrantTaskDO) {
        position.grant = this
        positions = (positions ?: mutableListOf()).also { it.add(position) }
    }
}
