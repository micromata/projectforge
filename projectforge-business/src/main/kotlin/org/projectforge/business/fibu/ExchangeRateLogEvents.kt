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

package org.projectforge.business.fibu

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object ExchangeRateLogEvents {
    @JvmField
    val FETCH_FAILED = LogEvent(
        code = "fibu.exchangeRate.fetchFailed",
        category = LogCategory.EXTERNAL,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The exchange rate API couldn't be called (network error, timeout). No rate is suggested; " +
                "users have to enter the rate themselves.",
        action = "Check whether the exchange rate API is reachable from this server (network, proxy) and still available.",
    )

    @JvmField
    val UNEXPECTED_RESPONSE = LogEvent(
        code = "fibu.exchangeRate.unexpectedResponse",
        category = LogCategory.EXTERNAL,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The exchange rate API answered, but the response couldn't be parsed. The API may have changed.",
        action = "Compare the response format of the exchange rate API with ExchangeRateApiService.",
    )
}
