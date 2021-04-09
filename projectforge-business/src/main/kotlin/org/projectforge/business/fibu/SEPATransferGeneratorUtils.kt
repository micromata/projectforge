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

package org.projectforge.business.fibu

object SEPATransferGeneratorUtils {
  @JvmStatic
  fun eraseUnsuportedChars(orig: String?): String {
    orig ?: return ""
    // erases all the ASCII control characters
    var text = orig.replace("[\\p{Cntrl}&&[^\r\n\t]]".toRegex(), "")

    // removes non-printable characters from Unicode
    text = text.replace("[\\p{C}&&[^\r\n\t]]".toRegex(), "")

    return text.trim { it <= ' ' }
  }
}
