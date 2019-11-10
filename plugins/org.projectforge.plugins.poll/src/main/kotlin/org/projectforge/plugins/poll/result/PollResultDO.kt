/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.poll.result

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.plugins.poll.attendee.PollAttendeeDO
import org.projectforge.plugins.poll.event.PollEventDO
import javax.persistence.*

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_POLL_RESULT", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_poll_result_tenant_id", columnList = "tenant_id")])
open class PollResultDO : DefaultBaseDO() {

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_event_fk")
    open var pollEvent: PollEventDO? = null

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_attendee_fk")
    open var pollAttendee: PollAttendeeDO? = null

    @get:Column
    open var result: Boolean = false
}
