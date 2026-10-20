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

package org.projectforge.business.poll

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.HistoryIdList
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn
import java.time.LocalDate

/**
 * A poll: questions ([questions], json of [PollQuestion]) answered by its attendees until the [deadline].
 *
 * Attendees and the users with full access (who edit the poll and see all answers) are users and groups, stored
 * as csv lists of ids. Groups are resolved when checked ([PollDao]), so a later member of a group takes part, too.
 *
 * A [template] is a poll nobody answers: no invitation, no reminders, it is only cloned or its questions taken into
 * another poll.
 */
@Entity
@Indexed
@Table(name = "t_poll")
@AUserRightId(value = "poll", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollDO : DefaultBaseDO() {

    @get:PropertyInfo(i18nKey = "poll.title", required = true)
    @FullTextField
    @get:Column(name = "title", nullable = false, length = 1000)
    open var title: String? = null

    @get:PropertyInfo(i18nKey = "poll.description")
    @FullTextField
    @get:Column(name = "description", length = 10000)
    open var description: String? = null

    @get:PropertyInfo(i18nKey = "poll.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk", nullable = false)
    open var owner: PFUserDO? = null

    @get:PropertyInfo(i18nKey = "poll.location")
    @FullTextField
    @get:Column(name = "location", length = 1000)
    open var location: String? = null

    @get:PropertyInfo(i18nKey = "poll.deadline")
    @get:Column(name = "deadline", nullable = false)
    open var deadline: LocalDate? = null

    @get:PropertyInfo(i18nKey = "poll.attendees")
    @get:Column(name = "attendee_user_ids", length = 4000)
    @HistoryIdList(HistoryIdList.Type.USER)
    open var attendeeUserIds: String? = null

    @get:PropertyInfo(i18nKey = "poll.attendeeGroups")
    @get:Column(name = "attendee_group_ids", length = 4000)
    @HistoryIdList(HistoryIdList.Type.GROUP)
    open var attendeeGroupIds: String? = null

    @get:PropertyInfo(i18nKey = "poll.fullAccessUsers")
    @get:Column(name = "full_access_user_ids", length = 4000)
    @HistoryIdList(HistoryIdList.Type.USER)
    open var fullAccessUserIds: String? = null

    @get:PropertyInfo(i18nKey = "poll.fullAccessGroups")
    @get:Column(name = "full_access_group_ids", length = 4000)
    @HistoryIdList(HistoryIdList.Type.GROUP)
    open var fullAccessGroupIds: String? = null

    /** The questions, json of a list of [PollQuestion] (see [PollJson]). */
    @get:PropertyInfo(i18nKey = "poll.questions")
    @get:Column(name = "questions", length = 100000)
    open var questions: String? = null

    @get:PropertyInfo(i18nKey = "poll.state")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "state", length = 20, nullable = false)
    open var state: State = State.RUNNING

    @get:PropertyInfo(i18nKey = "poll.template", tooltip = "poll.template.info")
    @get:Column(name = "template", nullable = false)
    open var template: Boolean = false

    /** Answers are taken until the poll is finished: by hand, or the day after its deadline (see PollCronJobs). */
    enum class State(val key: String) : I18nEnum {
        RUNNING("running"), FINISHED("finished");

        override val i18nKey: String
            get() = "poll.state.$key"
    }

    companion object {
        /** The ids of a csv list of ids (`attendeeUserIds`, `fullAccessGroupIds`, ...). */
        fun toLongArray(str: String?): LongArray {
            if (str.isNullOrBlank()) return LongArray(0)
            return StringHelper.splitToLongs(str, ",", false)
        }
    }
}
