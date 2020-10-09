/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_skillmatrix_owner_fk", columnList = "owner_fk"), javax.persistence.Index(name = "idx_fk_t_plugin_skillmatrix_tenant_id", columnList = "tenant_id")])
open class SkillDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill")
    @Field
    @get:Column(length = Constants.LENGTH_TITLE)
    open var skill: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    open var owner: PFUserDO? = null

    /**
     * 1 - basic knowledge, 2 - established knowledge, 3 - expert knowledge
     */
    @PropertyInfo(i18nKey = "plugins.skillmatrix.rating")
    @Field
    @get:Column
    open var rating: Int? = null

    /**
     * 1 - interested, 2 - vested interest, 3 - going crazy
     */
    @PropertyInfo(i18nKey = "plugins.skillmatrix.interest")
    @Field
    @get:Column
    open var interest: Int? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = Constants.LENGTH_COMMENT)
    open var comment: String? = null

    val ownerId: Int?
        @Transient
        get() = owner?.id
}
