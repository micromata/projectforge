/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object IBANUtils {
  @JvmStatic
  fun format(iban: String?): String? {
    if (iban == null || iban.trim().any { it.isWhitespace() }) {
      // Don't format iban, if already formatted (by white spaces)
      return iban
    }
    return iban.filter { !it.isWhitespace() }.chunked(4).joinToString(" ")
  }

  /**
   * Validates DE-IBANs: must have 22 chars. Whitespaces will be ignored.
   * @return true, if the IBAN is null or successfully validated (format OK).
   */
  @JvmStatic
  fun validate(iban: String?): Boolean {
    iban ?: return true
    iban.filter { !it.isWhitespace() }.let {
      if (!it.startsWith("de", true)) {
        return true
      }
      return it.length == 22
    }
  }
}
