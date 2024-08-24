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

package org.projectforge.business.address

import jakarta.persistence.*
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.Constants
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsTypeBinder
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

/**
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@TypeBinding(binder = TypeBinderRef(type = HibernateSearchUsersGroupsTypeBinder::class))
//ClassBridge(name = "usersgroups", impl = ::class)
@Table(name = "T_ADDRESSBOOK")
open class AddressbookDO : BaseUserGroupRightsDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = "$title"

    @PropertyInfo(i18nKey = "addressbook.title")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var title: String? = null

    @PropertyInfo(i18nKey = "addressbook.owner")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "owner_fk")
    override var owner: PFUserDO? = null


    @PropertyInfo(i18nKey = "addressbook.description")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    /**
     * @see Object.hashCode
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder().append(this.id)
        return hcb.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is AddressbookDO) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }
}
