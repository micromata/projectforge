/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.common.i18n.I18nEnum

/**
 * Can't use LabelValueBean because XStream doesn't support generics (does it?).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
enum class PaymentType(val key: String) : I18nEnum {
    BANK_TRANSFER("bankTransfer"), DEBIT("debit"), CREDIT_CARD("creditCard"), CASH("cash"), SALARY("salary"), CREDIT("credit");

    override val i18nKey: String?
        get() = "fibu.payment.type.$key"

    fun isIn(vararg type: PaymentType?): Boolean {
        for (t in type) {
            if (this == t) {
                return true
            }
        }
        return false
    }

    companion object {
        fun safeValueOf(name: String?): PaymentType? {
            name ?: return null
            return PaymentType.entries.firstOrNull { it.name == name }
        }
    }
}
