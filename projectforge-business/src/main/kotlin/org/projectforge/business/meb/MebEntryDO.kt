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

package org.projectforge.business.meb

import org.hibernate.search.annotations.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_MEB_ENTRY", indexes = [javax.persistence.Index(name = "idx_fk_t_meb_entry_owner_fk", columnList = "owner_fk"), javax.persistence.Index(name = "idx_fk_t_meb_entry_tenant_id", columnList = "tenant_id")])
class MebEntryDO : AbstractBaseDO<Int>() {

    private var id: Int? = null

    @PropertyInfo(i18nKey = "meb.owner")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "meb.sender")
    @Field
    @get:Column(length = 255, nullable = false)
    var sender: String? = null

    @PropertyInfo(i18nKey = "meb.message")
    @Field
    @get:Column(length = 4000)
    var message: String? = null

    @PropertyInfo(i18nKey = "date")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(nullable = false)
    var date: Date? = null

    @PropertyInfo(i18nKey = "status")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    var status: MebEntryStatus? = null

    val ownerId: Int?
        @Transient
        get() = if (owner == null) {
            null
        } else {
            owner!!.id
        }

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
