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

package org.projectforge.rest.dto

import org.projectforge.business.utils.CurrencyFormatter
import org.projectforge.framework.utils.MarkdownBuilder
import java.math.BigDecimal

class BankAccountRecordStatistics {
  var income: BigDecimal = BigDecimal.ZERO
    protected set
  var outgo: BigDecimal = BigDecimal.ZERO
    protected set
  val cashflow: BigDecimal
    get() = income.add(outgo)

  val asMarkdown: String
    get() {
      val md = MarkdownBuilder()

      md.appendPipedValue("plugins.banking.account.record.income", CurrencyFormatter.format(income))
      md.appendPipedValue(
        "plugins.banking.account.record.outgo",
        CurrencyFormatter.format(outgo),
        MarkdownBuilder.Color.RED,
      )
      if (cashflow < BigDecimal.ZERO) {
        md.appendPipedValue(
          "plugins.banking.account.record.cashflow",
          CurrencyFormatter.format(cashflow),
          MarkdownBuilder.Color.RED,
        )
      } else {
        md.appendPipedValue("plugins.banking.account.record.cashflow", CurrencyFormatter.format(cashflow))
      }
      return md.toString()
    }

  fun add(amount: BigDecimal?) {
    amount ?: return
    if (amount < BigDecimal.ZERO) {
      outgo += amount
    } else {
      income += amount
    }
  }

}
