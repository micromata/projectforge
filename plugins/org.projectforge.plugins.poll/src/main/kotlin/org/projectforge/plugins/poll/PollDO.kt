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

package org.projectforge.plugins.poll

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_POLL", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_poll_tenant_id", columnList = "tenant_id")])
open class PollDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "plugins.teamcal.owner")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.poll.new.title")
    @get:Column
    open var title: String? = null

    @PropertyInfo(i18nKey = "plugins.poll.new.location")
    @get:Column
    open var location: String? = null

    @PropertyInfo(i18nKey = "plugins.poll.new.description")
    @get:Column
    open var description: String? = null

    @get:Column
    open var active: Boolean = false

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (id == null) 0 else id!!.hashCode()
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
        if (javaClass != other.javaClass)
            return false
        val o = other as PollDO?
        if (id == null) {
            return if (o!!.id != null)
                false
            else
                super.equals(o)
        } else if (id != o!!.id)
            return false
        return true
    }
}
