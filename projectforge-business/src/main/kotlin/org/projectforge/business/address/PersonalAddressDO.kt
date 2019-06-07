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

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.UniqueConstraint

import org.hibernate.search.annotations.Indexed
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Every user has his own address book (a subset of all addresses). For every address a user can define which phone
 * numbers he wants to add to his address book and/or the whole address.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PERSONAL_ADDRESS", uniqueConstraints = [UniqueConstraint(columnNames = ["owner_id", "address_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_personal_address_address_id", columnList = "address_id"), javax.persistence.Index(name = "idx_fk_t_personal_address_owner_id", columnList = "owner_id"), javax.persistence.Index(name = "idx_fk_t_personal_address_tenant_id", columnList = "tenant_id")])
class PersonalAddressDO : AbstractBaseDO<Int>() {

    private var id: Int? = null

    /**
     * Not used as object due to performance reasons.
     */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "address_id", nullable = false)
    var address: AddressDO? = null

    /**
     * If true, the whole address will be exported as vCard.
     */
    @get:Column(nullable = false, name = "favorite_card")
    var isFavoriteCard: Boolean = false

    @get:Column(name = "business_phone", nullable = false)
    var isFavoriteBusinessPhone: Boolean = false

    @get:Column(name = "private_phone", nullable = false)
    var isFavoritePrivatePhone: Boolean = false

    @get:Column(name = "mobile_phone", nullable = false)
    var isFavoriteMobilePhone: Boolean = false

    @get:Column(name = "private_mobile_phone", nullable = false)
    var isFavoritePrivateMobilePhone: Boolean = false

    @get:Column(nullable = false, name = "fax")
    var isFavoriteFax: Boolean = false

    /**
     * Not used as object due to performance reasons.
     */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_id", nullable = false)
    var owner: PFUserDO? = null

    /**
     * @param entry
     * @return true, if any checkbox is set (isFavoriteCard, isBusinessPhone etc.)
     */
    val isFavorite: Boolean
        @Transient
        get() = (isFavoriteCard || isFavoriteBusinessPhone || isFavoriteFax || isFavoriteMobilePhone || isFavoritePrivatePhone)

    val ownerId: Int?
        @Transient
        get() = if (this.owner == null) null else owner!!.id

    val addressId: Int?
        @Transient
        get() = if (this.address == null) null else address!!.id

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
