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
 * Data Transfer Object for parsed address text.
 * Contains recognized fields from free text input (e.g., email signatures).
 */
data class ParsedAddressData(
    var title: String? = null,
    var firstName: String? = null,
    var name: String? = null,
    var form: String? = null,
    var positionText: String? = null,
    var organization: String? = null,
    var division: String? = null,
    var addressText: String? = null,
    var addressText2: String? = null,
    var zipCode: String? = null,
    var city: String? = null,
    var state: String? = null,
    var country: String? = null,
    var businessPhone: String? = null,
    var mobilePhone: String? = null,
    var fax: String? = null,
    var privatePhone: String? = null,
    var privateMobilePhone: String? = null,
    var email: String? = null,
    var privateEmail: String? = null,
    var website: String? = null,
    var comment: String? = null
)
