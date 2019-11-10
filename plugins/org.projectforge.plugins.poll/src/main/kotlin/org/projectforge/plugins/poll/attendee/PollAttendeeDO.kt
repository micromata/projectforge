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

package org.projectforge.plugins.poll.attendee

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.poll.PollDO
import javax.persistence.*

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_POLL_ATTENDEE", indexes = [Index(name = "idx_fk_t_plugin_poll_attendee_tenant_id", columnList = "tenant_id")])
open class PollAttendeeDO : DefaultBaseDO() {

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk")
    open var user: PFUserDO? = null

    @get:Column
    open var email: String? = null

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_fk")
    open var poll: PollDO? = null

    @get:Column
    open var secureKey: String? = null

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (email == null) 0 else email!!.hashCode()
        result = prime * result + if (poll == null) 0 else poll!!.hashCode()
        result = prime * result + if (secureKey == null) 0 else secureKey!!.hashCode()
        result = prime * result + if (user == null) 0 else user!!.hashCode()
        return result
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is PollAttendeeDO)
            return false
        if (email == null) {
            if (other.email != null)
                return false
        } else if (email != other.email)
            return false
        if (poll == null) {
            if (other.poll != null)
                return false
        } else if (poll != other.poll)
            return false
        if (secureKey == null) {
            if (other.secureKey != null)
                return false
        } else if (secureKey != other.secureKey)
            return false
        if (user == null) {
            if (other.user != null)
                return false
        } else if (user != other.user)
            return false
        return true
    }
}
