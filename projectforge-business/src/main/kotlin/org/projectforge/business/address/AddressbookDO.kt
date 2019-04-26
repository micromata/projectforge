/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.address

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.*
import org.hibernate.search.annotations.Index
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.ReflectionToString
import java.util.*
import javax.persistence.*

/**
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@ClassBridge(name = "usersgroups", impl = HibernateSearchUsersGroupsBridge::class)
@Table(name = "T_ADDRESSBOOK", indexes = [javax.persistence.Index(name = "idx_fk_t_addressbook_tenant_id", columnList = "tenant_id")])
class AddressbookDO : BaseUserGroupRightsDO() {

    @PropertyInfo(i18nKey = "addressbook.title")
    @Field
    @get:Column(length = Constants.LENGTH_TITLE)
    var title: String? = null

    @PropertyInfo(i18nKey = "addressbook.owner")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "addressbook.description")
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    var description: String? = null

    /**
     * @see Object.hashCode
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder().append(this.id)
        return hcb.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o !is AddressbookDO) {
            return false
        }
        return Objects.equals(this.id, o.id)
    }

    /**
     * @param user
     * @return
     */
    override fun toString(): String {
        return object : ReflectionToString(this) {
        }.toString()
    }

    companion object {
        private val serialVersionUID = 2869412345643084605L
    }
}
