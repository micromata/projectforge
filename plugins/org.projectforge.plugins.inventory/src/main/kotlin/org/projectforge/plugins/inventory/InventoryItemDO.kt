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

import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "owners", impl = HibernateSearchUsersBridge::class)
@Table(name = "T_PLUGIN_INVENTORY_ITEM")
open class InventoryItemDO : DefaultBaseDO() {

  @PropertyInfo(i18nKey = "plugins.inventory.item")
  @Field
  @get:Column(length = 1000, nullable = false)
  open var item: String? = null

  @PropertyInfo(i18nKey = "plugins.inventory.owners")
  @get:Column(name = "owner_ids", length = 10000)
  open var ownerIds: String? = null

  @PropertyInfo(i18nKey = "plugins.inventory.externalOwner")
  @Field
  @get:Column(name = "external_owners", length = 10000)
  open var externalOwners: String? = null

  @PropertyInfo(i18nKey = "comment")
  @Field
  @get:Column(length = Constants.LENGTH_COMMENT)
  open var comment: String? = null
}
