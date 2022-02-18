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

package org.projectforge.plugins.datatransfer

import org.projectforge.framework.i18n.TimeLeft
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import java.util.*

object DataTransferUtils {
  fun expiryTimeLeft(lastUpdate: Date?, expiryDays: Int?): String {
    lastUpdate ?: return ""
    val expiryDate = PFDateTime.from(lastUpdate).plusDays((expiryDays ?: 30).toLong()).utilDate
    return TimeLeft.getMessage(expiryDate, pastMessage = translate("plugins.datatransfer.expired"))
  }
}
