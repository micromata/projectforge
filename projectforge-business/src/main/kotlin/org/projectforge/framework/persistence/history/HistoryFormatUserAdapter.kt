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

package org.projectforge.framework.persistence.history

import mu.KotlinLogging
import org.projectforge.business.user.UserRightDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.context.ApplicationContext

private val log = KotlinLogging.logger {}

class HistoryFormatUserAdapter(
  val historyService: HistoryFormatService,
  val applicationContext: ApplicationContext,
) : HistoryFormatAdapter {

  private val userRightDao = applicationContext.getBean(UserRightDao::class.java)

  override fun convertEntries(item: Any, entries: MutableList<HistoryFormatService.DisplayHistoryEntryDTO>) {
    if (item !is PFUserDO) {
      log.warn { "Can't hanlde history entries for entity of type ${item::class.java.name}" }
      return
    }
    item.rights?.forEach { right ->
      userRightDao.getHistoryEntries(right)?.forEach { entry ->
        val dto = historyService.convert(entry)
        dto.diffEntries.forEach { diffEntry ->
          val propertyName = diffEntry.property
          if (propertyName != null) {
            diffEntry.property = "${right.rightIdString}:$propertyName"
          } else {
            diffEntry.property = right.rightIdString.toString()
          }
        }
        entries.add(dto)
      }
    }
  }
}
