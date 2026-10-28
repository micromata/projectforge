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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn

/** The answers of one user ([owner]) to a [poll]: at most one per user and poll. */
@Entity
@Indexed
@Table(
    name = "t_poll_response",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_poll_response_poll_owner", columnNames = ["poll_fk", "owner_fk"])],
)
@AUserRightId(value = "poll.response", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollResponseDO : DefaultBaseDO() {

    @get:PropertyInfo(i18nKey = "poll.title")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_fk", nullable = false)
    open var poll: PollDO? = null

    @get:PropertyInfo(i18nKey = "poll.response.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk", nullable = false)
    open var owner: PFUserDO? = null

    /** The answers, json of a list of [PollAnswer] (see [PollJson]). */
    @get:PropertyInfo(i18nKey = "poll.answers")
    @get:Column(name = "answers", length = 100000)
    open var answers: String? = null
}
