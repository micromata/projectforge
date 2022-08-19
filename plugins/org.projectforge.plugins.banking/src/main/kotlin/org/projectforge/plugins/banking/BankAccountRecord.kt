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

import org.projectforge.rest.dto.BaseDTO
import java.math.BigDecimal
import java.time.LocalDate

class BankAccountRecord(
  var accountName: String? = null,
  var accountIban: String? = null,
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
) : BaseDTO<BankAccountRecordDO>() {
  override fun copyFrom(src: BankAccountRecordDO) {
    super.copyFrom(src)
    accountIban = src.bankAccount?.iban
    accountName = src.bankAccount?.name
  }

  /**
   * Checks if the given dest account needs an update (differs from this).
   */
  fun updateNeeded(dest: BankAccountRecordDO): Boolean {
    return updateNeeded(amount, dest.amount)
        // ||updateNeeded(date, dest.date) // Can't occur (the records will never match)
        || updateNeeded(valueDate, dest.valueDate)
        || updateNeeded(type, dest.type)
        || updateNeeded(subject, dest.subject)
        || updateNeeded(currency, dest.currency)
        || updateNeeded(debteeId, dest.debteeId)
        || updateNeeded(mandateReference, dest.mandateReference)
        || updateNeeded(customerReference, dest.customerReference)
        || updateNeeded(collectionReference, dest.collectionReference)
        || updateNeeded(info, dest.info)
        || updateNeeded(receiverSender, dest.receiverSender) // Shouldn't occur
        || updateNeeded(iban, dest.iban)
        || updateNeeded(bic, dest.bic)
  }

  /**
   * Try to find pairs of imported and data base records of one date by finding the best fits using scores.
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
    if (value == null) {
      return if (dest == null) {
        1
      } else {
        0
      }
    }
    return if (value.subtract(dest).abs() <= BigDecimal.ONE) {
      1 // Both amounts are nearly equal.
    } else {
      0
    }
  }

  private fun updateNeeded(value: Any?, dest: Any?): Boolean {
    value ?: return false
    return value != dest
  }
}
