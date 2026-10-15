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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "projectforge.einvoice.seller")
open class EInvoiceSellerConfig {
    var name: String = ""
    var street: String = ""
    var zip: String = ""
    var city: String = ""
    var country: String = "DE"
    var vatId: String = ""
    var taxNumber: String = ""
    var email: String = ""
    var phone: String = ""
    var contactName: String = ""
    var bankAccounts: MutableList<BankAccountConfig> = mutableListOf()

    fun isConfigured(): Boolean {
        return name.isNotBlank() && street.isNotBlank() && zip.isNotBlank() && city.isNotBlank()
                && (vatId.isNotBlank() || taxNumber.isNotBlank())
    }

    fun findBankAccount(iban: String?): BankAccountConfig? {
        if (iban.isNullOrBlank()) return null
        return bankAccounts.find { it.iban == iban }
    }
}

open class BankAccountConfig {
    var name: String = ""
    var iban: String = ""
    var bic: String = ""

    val displayName: String
        get() = if (name.isNotBlank()) "$name - $iban" else iban
}
