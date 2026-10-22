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
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate
import java.util.*

/**
 * An ad hoc booking grant: the given user may book own time sheets on exactly this task (not on its descendants)
 * within the period, without being member of a group with access to the task. For a colleague helping out for a
 * short time, who mustn't see anything else of the project (see [TaskBookingGrantCache]).
 *
 * Granted by somebody who may edit the time sheets of others on the task (project management, finance), either
 * directly or as an invitation link that only the given user may accept ([acceptedAt]). A revoked grant is marked as
 * deleted: the user keeps reading the own time sheets booked on the task, but books no more.
 */
@Entity
@Table(
    name = "t_task_booking_grant",
    indexes = [
        jakarta.persistence.Index(name = "idx_fk_t_task_booking_grant_task_id", columnList = "task_id"),
        jakarta.persistence.Index(name = "idx_fk_t_task_booking_grant_user_id", columnList = "user_id"),
    ]
)
open class TaskBookingGrantDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "task")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var task: TaskDO? = null

    /**
     * The user the grant is for, also of an invitation link not yet accepted: nobody else may accept it.
     */
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

    /**
     * Optional: the only cost unit the user may book on. Otherwise every cost unit of the task.
     */
    @PropertyInfo(i18nKey = "fibu.kost2")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kost2_id")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kost2: Kost2DO? = null

    @PropertyInfo(i18nKey = "comment")
    @get:Column(name = "comment", length = 4000)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "task.bookingGrant.grantedBy")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "granted_by_user_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var grantedBy: PFUserDO? = null

    /**
     * Null as long as an invitation link isn't accepted by [user]: the grant gives no access until then. Set on
     * insert for a grant given directly.
     */
    @PropertyInfo(i18nKey = "task.bookingGrant.acceptedAt")
    @get:Column(name = "accepted_at")
    open var acceptedAt: Date? = null

    /**
     * The SHA-256 hash (hex) of the token of the invitation link, null for a grant given directly and after the
     * link is accepted. The token itself is never stored.
     */
    @JsonIgnore
    @NoHistory
    @get:Column(name = "invitation_token_hash", length = 64, unique = true)
    open var invitationTokenHash: String? = null

    @PropertyInfo(i18nKey = "task.bookingGrant.invitationExpiresAt")
    @NoHistory
    @get:Column(name = "invitation_expires_at")
    open var invitationExpiresAt: Date? = null

    open val accepted: Boolean
        @Transient
        get() = acceptedAt != null
}
