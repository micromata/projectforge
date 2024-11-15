/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.bridge.ValueBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.i18n.I18nHelper

private val log = KotlinLogging.logger {}

/**
 * Bridge for hibernate search to search for payment type of incoming invoices.
 *
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
class HibernateSearchPaymentTypeBridge : ValueBridge<PaymentType, String> {
    override fun toIndexedValue(
        paymentType: PaymentType?,
        valueBridgeToIndexedValueContext: ValueBridgeToIndexedValueContext?
    ): String {
        paymentType ?: return ""
        val sb = StringBuilder()
        for (locale in UserLocale.I18NSERVICE_LANGUAGES) {
            val localized: String = I18nHelper.getLocalizedMessage(locale, paymentType.i18nKey)
            sb.append("$localized ")
        }
        if (log.isDebugEnabled) {
            log.debug(sb.toString())
        }
        return sb.toString()
    }
}
