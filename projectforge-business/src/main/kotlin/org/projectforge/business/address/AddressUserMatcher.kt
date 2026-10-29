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

package org.projectforge.business.address

import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Tells which address of the address book belongs to a user. There is no stored relation between users and addresses,
 * so it is derived from the name (as the birthday butler does) and, for the ranking of suggestions, from the e-mail
 * address and the organization.
 */
object AddressUserMatcher {
    /**
     * First name and last name of the address equal those of the user (trimmed, ignoring case).
     */
    fun nameMatches(address: AddressDO, user: PFUserDO): Boolean {
        return address.firstName?.trim()?.equals(user.firstname?.trim(), ignoreCase = true) == true &&
                address.name?.trim()?.equals(user.lastname?.trim(), ignoreCase = true) == true
    }

    /**
     * One of the e-mail addresses of the user (also alternative and former ones) is the business or private e-mail
     * address of the address.
     */
    fun emailMatches(address: AddressDO, user: PFUserDO): Boolean {
        val addressEmails = listOfNotNull(address.email, address.privateEmail)
            .map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (addressEmails.isEmpty()) {
            return false
        }
        return PFUserDO.allEmails(user).any { it.lowercase() in addressEmails }
    }

    /**
     * The addresses belonging to the user, best match first: (1) name and e-mail match, (2) name matches and the
     * organization contains the given one (if any), (3) name matches. Addresses not up to date or of an inactive
     * contact are skipped.
     */
    fun suggest(user: PFUserDO, addresses: Collection<AddressDO>, organization: String? = null): List<AddressDO> {
        return addresses
            .filter { it.addressStatus == AddressStatus.UPTODATE && it.contactStatus == ContactStatus.ACTIVE }
            .filter { nameMatches(it, user) }
            .sortedBy { address ->
                when {
                    emailMatches(address, user) -> 0
                    !organization.isNullOrBlank() && address.organization?.contains(organization, ignoreCase = true) == true -> 1
                    else -> 2
                }
            }
    }
}
