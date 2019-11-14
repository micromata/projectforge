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

package org.projectforge.plugins.poll.event

import org.hibernate.search.annotations.*
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.plugins.poll.PollDO
import java.sql.Timestamp
import javax.persistence.*

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_POLL_EVENT", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_poll_event_tenant_id", columnList = "tenant_id")])
open class PollEventDO : DefaultBaseDO() {

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "poll_fk")
    open var poll: PollDO? = null

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column
    open var startDate: Timestamp? = null

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column
    open var endDate: Timestamp? = null

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (endDate == null) 0 else endDate!!.hashCode()
        result = prime * result + if (poll == null) 0 else poll!!.hashCode()
        result = prime * result + if (startDate == null) 0 else startDate!!.hashCode()
        return result
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (other !is PollEventDO) {
            return false
        }
        if (endDate == null) {
            if (other.endDate != null) {
                return false
            }
        } else if (!endDate!!.equals(other.endDate)) {
            return false
        }
        if (poll == null) {
            if (other.poll != null) {
                return false
            }
        } else if (poll != other.poll) {
            return false
        }
        if (startDate == null) {
            if (other.startDate != null) {
                return false
            }
        } else if (!startDate!!.equals(other.startDate)) {
            return false
        }
        return true
    }
}
