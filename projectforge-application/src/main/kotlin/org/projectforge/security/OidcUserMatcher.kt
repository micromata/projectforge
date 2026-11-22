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

package org.projectforge.security

import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Maps an OIDC identity to a ProjectForge user, shared by the normal login ([OAuth2UserService]) and the gateway.
 *
 * The `sub` claim is the identity. `preferred_username` is neither unique nor stable (an IdP may let users choose or
 * change it), so it only finds a user who isn't bound to an IdP identity yet: a user with another `idpExternalId` is
 * never taken over by a name.
 */
object OidcUserMatcher {
    fun resolve(userDao: UserDao, sub: String?, preferredUsername: String?): PFUserDO? {
        sub?.let { id -> userDao.getUserByIdpExternalId(id)?.let { return it } }
        if (preferredUsername.isNullOrBlank()) {
            return null
        }
        val byName = userDao.getInternalByName(preferredUsername) ?: return null
        return byName.takeIf { it.idpExternalId.isNullOrBlank() }
    }

    /** Whether a user already stored in a session is the one of the given OIDC identity. */
    fun isSameIdentity(user: PFUserDO?, sub: String?, preferredUsername: String?): Boolean {
        user ?: return false
        val boundId = user.idpExternalId
        return if (boundId.isNullOrBlank()) {
            !preferredUsername.isNullOrBlank() && user.username == preferredUsername
        } else {
            boundId == sub
        }
    }
}
