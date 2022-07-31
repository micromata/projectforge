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
package org.projectforge.business.fibu.kost

import org.projectforge.framework.utils.NumberHelper.parseInteger

object KostHelper {
  /**
   * @param kostString Format ######## or #.###.##.## is supported.
   * @return int[4] or null if string is not parse-able.
   */
  @JvmStatic
  fun parseKostString(kostString: String?): IntArray? {
    if (kostString == null) {
      return null
    }
    val str = kostString.trim { it <= ' ' }
    if (str.matches("\\d{8}".toRegex())) {
      // 12345678
      val result = IntArray(4)
      result[0] = parseInteger(kostString.substring(0, 1))!!
      result[1] = parseInteger(kostString.substring(1, 4))!!
      result[2] = parseInteger(kostString.substring(4, 6))!!
      result[3] = parseInteger(kostString.substring(6, 8))!!
      return result
    } else if (str.matches("\\d{1}\\.\\d{3}\\.\\d{2}\\.\\d{2}".toRegex())) {
      val result = IntArray(4)
      result[0] = parseInteger(kostString.substring(0, 1))!!
      result[1] = parseInteger(kostString.substring(2, 5))!!
      result[2] = parseInteger(kostString.substring(6, 8))!!
      result[3] = parseInteger(kostString.substring(9, 11))!!
      return result
    }
    return null
  }
}
