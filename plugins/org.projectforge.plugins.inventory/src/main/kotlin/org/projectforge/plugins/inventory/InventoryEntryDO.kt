/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.inventory

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "T_PLUGIN_INVENTORY_ENTRY",
  indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_inventory_entry_owner_fk", columnList = "owner_fk")]
)
@NamedQueries(
  NamedQuery(
    name = InventoryEntryDO.FIND_OF_OWNER,
    query = "from InventoryEntryDO where owner.id=:ownerId and deleted=false"
  ),
)
open class InventoryEntryDO : DefaultBaseDO() {

  @PropertyInfo(i18nKey = "plugins.inventory.item")
  @Field
  @get:Column(length = 1000, nullable = false)
  open var item: String? = null

  @PropertyInfo(i18nKey = "plugins.inventory.owner")
  @IndexedEmbedded(depth = 1)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "owner_fk")
  open var owner: PFUserDO? = null

  @PropertyInfo(i18nKey = "plugins.inventory.externalOwner")
  @get:Column(name = "external_owner", length = Constants.LENGTH_SUBJECT)
  open var externalOwner: String? = null

  @PropertyInfo(i18nKey = "comment")
  @Field
  @get:Column(length = Constants.LENGTH_COMMENT)
  open var comment: String? = null

  val ownerId: Int?
    @Transient
    get() = owner?.id

  companion object {
    const val FIND_OF_OWNER = "InventoryEntryDO_FindInventryByOwner"
  }
}
