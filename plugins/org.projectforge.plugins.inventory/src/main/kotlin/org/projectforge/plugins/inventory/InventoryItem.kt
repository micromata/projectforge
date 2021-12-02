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

import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.User

/**
 * Data transfer object (DTO) for exchange with rest client (React app).
 */
class InventoryItem(
  var item: String? = null,
  var owners: List<User>? = null,
  var ownersAsString: String? = null,
  var externalOwners: String? = null,
  var comment: String? = null,
) : BaseDTO<InventoryItemDO>() {
  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyFrom(src: InventoryItemDO) {
    super.copyFrom(src)
    owners = User.toUserList(src.ownerIds)
  }

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyTo(dest: InventoryItemDO) {
    super.copyTo(dest)
    dest.ownerIds = User.toIntList(owners)
  }
}
