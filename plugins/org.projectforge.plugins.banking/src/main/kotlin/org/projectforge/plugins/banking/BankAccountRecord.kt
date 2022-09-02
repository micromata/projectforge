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

package org.projectforge.plugins.banking

import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.importer.ImportPairEntry
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.reflect.KProperty

class BankAccountRecord(
  var bankAccount: BankAccount? = null,
  var amount: BigDecimal? = null,
  var date: LocalDate? = null,
  var valueDate: LocalDate? = null,
  var type: String? = null,
  var subject: String? = null,
  var comment: String? = null,
  var currency: String? = null,
  var debteeId: String? = null,
  var mandateReference: String? = null,
  var customerReference: String? = null,
  var collectionReference: String? = null,
  var info: String? = null,
  var receiverSender: String? = null,
  var iban: String? = null,
  var bic: String? = null,
) : BaseDTO<BankAccountRecordDO>(), ImportPairEntry.Modified<BankAccountRecord> {
  override fun copyFrom(src: BankAccountRecordDO) {
    super.copyFrom(src)
    src.bankAccount?.let { bankAccountDO ->
      val account = BankAccount()
      account.copyFromMinimal(bankAccountDO)
      account.name = bankAccountDO.name
      account.iban = bankAccountDO.iban
      account.bic = bankAccountDO.bic
      bankAccount = account
    }
  }

  override val properties: Array<KProperty<*>>
    get() = arrayOf(
      BankAccountRecordDO::amount,
      // ||updateNeeded(date, dest.date) // Can't occur (the records will never match)
      BankAccountRecordDO::valueDate,
      BankAccountRecordDO::type,
      BankAccountRecordDO::subject,
      // || isModified(comment, other.comment) // Comment isn't part of import.
      BankAccountRecordDO::currency,
      BankAccountRecordDO::debteeId,
      BankAccountRecordDO::mandateReference,
      BankAccountRecordDO::customerReference,
      BankAccountRecordDO::collectionReference,
      BankAccountRecordDO::info,
      BankAccountRecordDO::receiverSender,
      BankAccountRecordDO::iban,
      BankAccountRecordDO::bic,
    )

  /**
   * Try to find pairs of imported and database records of one date by finding the best fits using scores.
   */
  fun matchScore(dest: BankAccountRecordDO): Int {
    if (date != dest.date) {
      return -1 // Can't match
    }
    var score = 0
    score += getScore(amount, dest.amount)
    score += getScore(subject, dest.subject)
    score += getScore(currency, dest.currency)
    score += getScore(debteeId, dest.debteeId)
    score += getScore(mandateReference, dest.mandateReference)
    score += getScore(customerReference, dest.customerReference)
    score += getScore(collectionReference, dest.collectionReference)
    score += getScore(info, dest.info)
    score += getScore(receiverSender, dest.receiverSender)
    score += getScore(iban, dest.iban)
    score += getScore(bic, dest.bic)
    return score
  }

  private fun getScore(value: String?, dest: String?): Int {
    val str1 = normalizeString(value)
    val str2 = normalizeString(dest)
    if (str1.isEmpty() || str2.isEmpty()) {
      return 0
    }
    return if (normalizeString(value) == normalizeString(dest)) {
      1
    } else {
      0
    }
  }

  /**
   * Returns the given string by taking only a-z, A-Z (tolower) and digits.
   */
  private fun normalizeString(str: String?): String {
    str ?: return ""
    val sb = StringBuilder()
    for (c in str) {
      if (c in 'A'..'Z') {
        sb.append(c.lowercaseChar())
      } else if (Character.isDigit(c) || c in 'a'..'z') {
        sb.append(c)
      }
    }
    return sb.toString()
  }

  private fun getScore(value: BigDecimal?, dest: BigDecimal?): Int {
    return if (value == null || dest == null) {
      0
    } else if (value.compareTo(dest) == 0) {
      1
    } else {
      0
    }
  }

  private fun truncate(value: BigDecimal): BigDecimal {
    return if (value < BigDecimal.ZERO) {
      value.setScale(0, RoundingMode.CEILING) // -1,23 -> -1
    } else {
      value.setScale(0, RoundingMode.FLOOR)   // +1,23 -> +1
    }
  }
}
