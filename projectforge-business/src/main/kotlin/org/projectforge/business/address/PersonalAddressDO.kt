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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.business.address.PersonalAddressDO.Companion.DELETE_ALL_BY_ADDRESS_ID
import org.projectforge.business.address.PersonalAddressDO.Companion.FIND_BY_OWNER
import org.projectforge.business.address.PersonalAddressDO.Companion.FIND_BY_OWNER_AND_ADDRESS_ID
import org.projectforge.business.address.PersonalAddressDO.Companion.FIND_BY_OWNER_AND_ADDRESS_UID
import org.projectforge.business.address.PersonalAddressDO.Companion.FIND_FAVORITE_ADDRESS_IDS_BY_OWNER
import org.projectforge.business.address.PersonalAddressDO.Companion.FIND_JOINED_BY_OWNER
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import jakarta.persistence.*
import org.projectforge.framework.json.IdOnlySerializer

/**
 * Every user has his own address book (a subset of all addresses). For every address a user can define which phone
 * numbers he wants to add to his address book and/or the whole address.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "T_PERSONAL_ADDRESS",
  uniqueConstraints = [UniqueConstraint(columnNames = ["owner_id", "address_id"])],
  indexes = [Index(name = "idx_fk_t_personal_address_address_id", columnList = "address_id"),
    Index(name = "idx_fk_t_personal_address_owner_id", columnList = "owner_id")]
)
@NamedQueries(
  NamedQuery(
    name = DELETE_ALL_BY_ADDRESS_ID,
    query = "delete from PersonalAddressDO where address.id=:addressId"
  ),
  NamedQuery(
    name = FIND_FAVORITE_ADDRESS_IDS_BY_OWNER,
    query = "select pa.address.id from PersonalAddressDO pa where pa.owner.id=:ownerId and pa.favoriteCard=true and deleted=false and pa.address.deleted=false"
  ),
  NamedQuery(
    name = FIND_BY_OWNER,
    query = "from PersonalAddressDO pa where pa.owner.id = :ownerId"
  ),
  NamedQuery(
    name = FIND_BY_OWNER_AND_ADDRESS_ID,
    query = "from PersonalAddressDO where owner.id = :ownerId and address.id = :addressId"
  ),
  NamedQuery(
    name = FIND_BY_OWNER_AND_ADDRESS_UID,
    query = "from PersonalAddressDO p where owner.id = :ownerId and address.uid = :addressUid"
  ),
  NamedQuery(
    name = FIND_JOINED_BY_OWNER,
    query = "from PersonalAddressDO p join fetch p.address where p.owner.id = :ownerId and p.address.deleted=false order by p.address.name, p.address.firstName"
  )
)
class PersonalAddressDO : AbstractBaseDO<Long>() {

  @get:Id
  @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
  @get:Column(name = "pk")
  override var id: Long? = null

  /**
   * Not used as object due to performance reasons.
   */
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "address_id", nullable = false)
  @JsonSerialize(using = IdOnlySerializer::class)
  var address: AddressDO? = null

  /**
   * If true, the whole address will be exported as vCard.
   */
  @get:Column(nullable = false, name = "favorite_card")
  var isFavoriteCard: Boolean = false

  /**
   * Not used as object due to performance reasons.
   */
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "owner_id", nullable = false)
  @JsonSerialize(using = IdOnlySerializer::class)
  var owner: PFUserDO? = null

  /**
   * @return true, if any checkbox is set (isFavoriteCard, isBusinessPhone etc.)
   */
  val isFavorite: Boolean
    @Transient
    get() = isFavoriteCard

  val ownerId: Long?
    @Transient
    get() = if (this.owner == null) null else owner!!.id

  val addressId: Long?
    @Transient
    get() = if (this.address == null) null else address!!.id

  companion object {
    internal const val FIND_FAVORITE_ADDRESS_IDS_BY_OWNER = "PersonalAddressDO_FindIDsByOwner"

    /**
     * Also deleted ones.
     */
    internal const val DELETE_ALL_BY_ADDRESS_ID = "PersonalAddressDO_DeleteAllByAddressId"
    internal const val FIND_BY_OWNER = "PersonalAddressDO_FindByOwner"
    internal const val FIND_BY_OWNER_AND_ADDRESS_ID = "PersonalAddressDO_FindByOwnerAndAddressId"
    internal const val FIND_BY_OWNER_AND_ADDRESS_UID = "PersonalAddressDO_FindByOwnerAndAddressUid"
    internal const val FIND_JOINED_BY_OWNER = "PersonalAddressDO_FindJoinedByOwnerAndAddressId"
  }
}
