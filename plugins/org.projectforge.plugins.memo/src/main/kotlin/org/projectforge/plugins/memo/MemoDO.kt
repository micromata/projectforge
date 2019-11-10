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

package org.projectforge.plugins.memo

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * This data object is the Java representation of a data-base entry of a memo.<br></br>
 * Changes of this object will not be added to the history of changes. After deleting a memo it will be deleted in the
 * data-base (there is no undo!).<br></br>
 * If you want to use the history of changes and undo functionality please use DefaultBaseDO as super class instead of
 * AbstractBaseDO. .
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_MEMO", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_memo_owner_fk", columnList = "owner_fk"), javax.persistence.Index(name = "idx_fk_t_plugin_memo_tenant_id", columnList = "tenant_id")])
open class MemoDO : AbstractBaseDO<Int>() {

    @PropertyInfo(i18nKey = "id")
    private var id: Int? = null

    @PropertyInfo(i18nKey = "plugins.memo.subject")
    @Field
    @get:Column(length = Constants.LENGTH_TITLE)
    open var subject: String? = null

    @PropertyInfo(i18nKey = "plugins.memo.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.memo.memo")
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var memo: String? = null

    val ownerId: Int?
        @Transient
        get() = if (owner != null) owner!!.id else null

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getId(): Int? {
        return id
    }

    override fun setId(id: Int?) {
        this.id = id
    }
}
