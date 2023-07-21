/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.poll

import org.hibernate.search.annotations.Indexed
import org.projectforge.business.poll.filter.PollAssignment
import org.projectforge.business.poll.filter.PollState
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn
import java.time.LocalDate
import javax.persistence.*


@Entity
@Indexed
@Table(name = "t_poll")
@AUserRightId(value = "poll", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "poll.title")
    @get:Column(name = "title", nullable = false, length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "poll.description")
    @get:Column(name = "description", length = 10000)
    open var description: String? = null

    @get:PropertyInfo(i18nKey = "poll.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk", nullable = false)
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "poll.location")
    @get:Column(name = "location")
    open var location: String? = null

    @PropertyInfo(i18nKey = "poll.deadline")
    @get:Column(name = "deadline", nullable = false)
    open var deadline: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.date")
    @get:Column(name = "date")
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "poll.attendees")
    @get:Column(name = "attendeeIds", nullable = true)
    open var attendeeIds: String? = null

    @PropertyInfo(i18nKey = "poll.attendee_groups")
    @get:Column(name = "groupAttendeeIds", nullable = true)
    open var groupAttendeeIds: String? = null

    @PropertyInfo(i18nKey = "poll.full_access_groups")
    @get:Column(name = "full_access_group_ids", length = 4000, nullable = true)
    open var fullAccessGroupIds: String? = null

    @PropertyInfo(i18nKey = "poll.full_access_user")
    @get:Column(name = "full_access_user_ids", length = 4000, nullable = true)
    open var fullAccessUserIds: String? = null

    @PropertyInfo(i18nKey = "poll.inputFields")
    @get:Column(name = "inputFields", length = 1000)
    open var inputFields: String? = null

    @PropertyInfo(i18nKey = "poll.state")
    @get:Column(name = "state", nullable = false)
    open var state: State = State.RUNNING

    @Transient
    fun getPollAssignment(): MutableList<PollAssignment> {
        val currentUserId = ThreadLocalUserContext.userId!!
        val assignmentList = mutableListOf<PollAssignment>()
        if (currentUserId == this.owner?.id) {
            assignmentList.add(PollAssignment.OWNER)
        }
        val accessUserIds = toIntArray(this.fullAccessUserIds)
        if (accessUserIds?.contains(currentUserId) == true) {
            assignmentList.add(PollAssignment.ACCESS)
        }

        if (!this.fullAccessUserIds.isNullOrBlank()) {
            val accessUserIds = toIntArray(this.fullAccessUserIds)
            if (accessUserIds?.contains(currentUserId) == true) {
                assignmentList.add(PollAssignment.ACCESS)
            }
        }
        if (!this.attendeeIds.isNullOrBlank()) {
            val attendeeUserIds = toIntArray(this.attendeeIds)
            if (attendeeUserIds?.contains(currentUserId) == true) {
                assignmentList.add(PollAssignment.ATTENDEE)
            }
        }
        if (assignmentList.isEmpty())
            assignmentList.add(PollAssignment.OTHER)

        return assignmentList
    }

    @Transient
    fun getPollStatus(): PollState {
        return if (this.state == State.FINISHED) {
            PollState.FINISHED
        } else {
            PollState.RUNNING
        }
    }

    enum class State {
        RUNNING, FINISHED
    }

    companion object {
        internal fun toIntArray(str: String?): IntArray? {
            if (str.isNullOrBlank()) return null
            return StringHelper.splitToInts(str, ",", false)
        }
    }
}
