/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object StringHelper2 {
  @JvmStatic
  @JvmOverloads
  fun getWildCard(stringArray: Array<String?>?, wildCard: String = ""): String {
    if (stringArray.isNullOrEmpty()) {
      return wildCard
    }
    if (stringArray.any { it.isNullOrEmpty() }) {
      return wildCard
    }
    val first = stringArray[0]!!
    if (stringArray.size == 1) {
      return first
    }
    for (n in first.length downTo 0) {
      val str = first.take(n)
      var misMatchFound = false
      for (i in 1 until stringArray.size) {
        if (!stringArray[i]!!.startsWith(str)) {
          misMatchFound = true
          continue
        }
      }
      if (!misMatchFound) {
        return "$str$wildCard"
      }
    }
    return wildCard
  }
}
