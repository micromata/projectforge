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

package org.projectforge.business.common

class AutoCompletionUtils {
  companion object {
    @JvmStatic
    fun filter(list: List<String>?, search: String?): List<String> {
      list ?: return emptyList()
      if (search.isNullOrBlank()) {
        return list
      }
      val strings = search.split(" ")
      return list.filter { containsAll(it, strings) }.sorted()
    }

    internal fun containsAll(str: String, strings: List<String>): Boolean {
      strings.forEach {
        if (!str.contains(it, ignoreCase = true)) {
          return false
        }
      }
      return true
    }
  }
}
