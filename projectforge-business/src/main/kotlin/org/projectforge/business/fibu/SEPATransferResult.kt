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

package org.projectforge.business.fibu

import org.projectforge.business.fibu.SEPATransferGenerator.SEPATransferError
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.translate

/**
 * This class contains the result of an SEPA transfer generation.
 *
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
class SEPATransferResult {
  var xml: ByteArray? = null
  val errors: Map<EingangsrechnungDO, List<SEPATransferError>>

  init {
    errors = HashMap()
  }

  val isSuccessful: Boolean
    get() = xml != null

  companion object {
    @JvmStatic
    fun getMissingFields(errors: List<SEPATransferError>?): String {
      errors ?: return ""
      val missingFields = mutableListOf<String>()
      errors.forEach { error ->
        val i18nKey = when (error) {
          SEPATransferError.SUM -> "fibu.common.brutto"
          SEPATransferError.BANK_TRANSFER -> PropUtils.getI18nKey(EingangsrechnungDO::class.java, "paymentType")
          SEPATransferError.IBAN -> PropUtils.getI18nKey(EingangsrechnungDO::class.java, "iban")
          SEPATransferError.BIC -> PropUtils.getI18nKey(EingangsrechnungDO::class.java, "bic")
          SEPATransferError.RECEIVER -> PropUtils.getI18nKey(EingangsrechnungDO::class.java, "receiver")
          SEPATransferError.REFERENCE -> PropUtils.getI18nKey(EingangsrechnungDO::class.java, "referenz")
          else -> null
        }
        i18nKey?.let { missingFields.add(translate(i18nKey)) }
      }
      return  missingFields.joinToString(", ")
    }

    @JvmStatic
    fun getMissingFields(result: SEPATransferResult, invoice: EingangsrechnungDO?): String {
      return getMissingFields(result.errors[invoice])
    }

    const val MISSING_FIELDS_ERROR_I18N_KEY = "fibu.rechnung.transferExport.error.missing"
  }
}
