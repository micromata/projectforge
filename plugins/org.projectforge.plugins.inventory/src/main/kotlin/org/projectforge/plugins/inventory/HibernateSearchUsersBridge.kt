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

import org.hibernate.search.bridge.TwoWayStringBridge
import org.projectforge.rest.dto.User

/**
 * User names bridge for hibernate search to search in the user id's (comma separated values of user id's).
 *
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchUsersBridge : TwoWayStringBridge {
  /**
   * Get all full names and user names of all users represented by the string of user id's.
   */
  override fun objectToString(value: Any): String {
    if (value is String) return value
    val item = value as InventoryItemDO
    if (item.ownerIds.isNullOrBlank()) {
      return ""
    }
    return User.toSearchString(item.ownerIds)
  }

  override fun stringToObject(stringValue: String): Any? {
    // Not supported.
    return null
  }
}
