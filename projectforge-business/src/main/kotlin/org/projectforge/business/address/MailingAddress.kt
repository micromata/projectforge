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

package org.projectforge.business.address

/**
 * Lightweight DTO containing only mailing address fields for display purposes.
 * Uses the mailing address priority: postal → business → private.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
data class MailingAddress(
    var addressText: String? = null,
    var addressText2: String? = null,
    var zipCode: String? = null,
    var city: String? = null,
    var country: String? = null,
    var state: String? = null,
) {
    /**
     * Returns formatted multiline address string.
     * Format: addressText\naddressText2\nzipCode city\ncountry - state
     * Only includes lines and fields if values are given.
     */
    val formattedAddress: String?
        get() {
            val lines = mutableListOf<String>()

            // Add address text lines
            addressText?.let { if (it.isNotBlank()) lines.add(it) }
            addressText2?.let { if (it.isNotBlank()) lines.add(it) }

            // Add "zipCode city" line
            val cityLine = mutableListOf<String>()
            zipCode?.let { if (it.isNotBlank()) cityLine.add(it) }
            city?.let { if (it.isNotBlank()) cityLine.add(it) }
            if (cityLine.isNotEmpty()) {
                lines.add(cityLine.joinToString(" "))
            }

            // Add "country - state" line
            val countryLine = mutableListOf<String>()
            country?.let { if (it.isNotBlank()) countryLine.add(it) }
            state?.let { if (it.isNotBlank()) countryLine.add(it) }
            if (countryLine.isNotEmpty()) {
                lines.add(countryLine.joinToString(" - "))
            }

            return if (lines.isNotEmpty()) lines.joinToString("\n") else null
        }

    /**
     * Constructor that creates a MailingAddress from an AddressDO.
     * Uses the mailing address methods to get the first available address (postal → business → private).
     */
    constructor(src: AddressDO) : this() {
        addressText = src.mailingAddressText
        addressText2 = src.mailingAddressText2
        zipCode = src.mailingZipCode
        city = src.mailingCity
        country = src.mailingCountry
        state = src.mailingState
    }
}
