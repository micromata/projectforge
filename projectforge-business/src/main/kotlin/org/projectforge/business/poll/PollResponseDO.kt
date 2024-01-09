/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.annotation.DependsOn
import javax.persistence.*

@Entity
@Indexed
@Table(name = "t_poll_response")
@AUserRightId(value = "poll.response", checkAccess = false)
@DependsOn("org.projectforge.framework.persistence.user.entities.PFUserDO")
open class PollResponseDO : DefaultBaseDO() {

    @get:PropertyInfo(i18nKey = "poll.response.poll")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_fk", nullable = false)
    open var poll: PollDO? = null

    @get:PropertyInfo(i18nKey = "poll.response.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk", nullable = false)
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "poll.responses")
    @get:Column(name = "responses", nullable = true, length = 1000)
    open var responses: String? = null
}
