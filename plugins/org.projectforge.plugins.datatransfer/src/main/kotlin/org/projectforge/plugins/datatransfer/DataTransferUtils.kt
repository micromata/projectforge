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

package org.projectforge.plugins.datatransfer

import org.projectforge.Constants
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.TimeLeft
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.time.PFDateTime
import java.util.*

object DataTransferUtils {
  /** The id of the logged-in user's own personal box in [areaViewPath], resolved by the file view. */
  const val PERSONAL_BOX_ID = -1L

  /**
   * The file view of an area in projectforge-next, without leading slash, e.g. `next/datatransfer/42`.
   * Used by the menu, the links of the notification mails and the area's internal link.
   */
  fun areaViewPath(areaId: Long?): String {
    return "${Constants.NEXT_APP_PATH}$ROUTE/${areaId ?: 0}"
  }

  /** The route of the data transfer pages in projectforge-next (see `NextMigration.MIGRATED`). */
  const val ROUTE = "datatransfer"

  fun expiryTimeLeft(attachment: Attachment, expiryDays: Int?, locale: Locale? = null): String {
    return expiryTimeLeft(attachment.lastUpdate ?: attachment.created, expiryDays, locale)
  }

  private fun expiryTimeLeft(lastUpdate: Date?, expiryDays: Int?, locale: Locale?): String {
    lastUpdate ?: return ""
    val expiryDate = PFDateTime.from(lastUpdate).plusDays((expiryDays ?: 30).toLong()).utilDate
    return if (locale != null) {
      TimeLeft.getMessage(
        expiryDate,
        locale,
        pastMessage = I18nHelper.getLocalizedMessage(locale, "plugins.datatransfer.expired")
      )
    } else {
      TimeLeft.getMessage(expiryDate, pastMessage = translate("plugins.datatransfer.expired"))
    }
  }
}
