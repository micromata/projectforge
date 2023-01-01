/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.dvelop

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungListFilter
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * This class tries to extract trading partners from incoming and outgoing invoices and customers from
 * the database for creating an initial import for D-velop.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class MigrateTradingPartners {
  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  fun extractTradingVendors() {
    val filter = EingangsrechnungListFilter()
    filter.fromDate = PFDay.now().beginOfYear.minusYears(5).date
    extractVendors(eingangsrechnungDao.getList(filter))
  }

  internal fun extractVendors(list: List<EingangsrechnungDO>) {
    list.forEach { invoice ->
      val kreditor = invoice.kreditor
      if (kreditor.isNullOrBlank()) {
        return@forEach // Continue
      }
      val konto = invoice.konto ?: return@forEach // Continue
      val number = konto.nummer ?: return@forEach // Continue
      val bezeichnung = invoice.konto?.bezeichnung ?: ""
      if (bezeichnung.contains("divers", ignoreCase = true)) {
        // Mehrere Kreditoren unter einem Konto vereint.

      } else {

      }
    }
  }
}
