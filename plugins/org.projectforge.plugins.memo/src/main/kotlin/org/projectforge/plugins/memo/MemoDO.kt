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

package org.projectforge.plugins.memo

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField

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
@Table(name = "T_PLUGIN_MEMO", indexes = [Index(name = "idx_fk_t_plugin_memo_owner_fk", columnList = "owner_fk")])
open class MemoDO : AbstractBaseDO<Long>() {

    @get:Column(name = "pk")
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Id
    @PropertyInfo(i18nKey = "id")
    override var id: Long? = null

    @PropertyInfo(i18nKey = "plugins.memo.subject")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var subject: String? = null

    @PropertyInfo(i18nKey = "plugins.memo.owner")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    open var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.memo.memo")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var memo: String? = null

    val ownerId: Long?
        @Transient
        get() = owner?.id
}
