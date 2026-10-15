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

package org.projectforge.business.user

/**
 * Supported tokens
 *
 * @author Kai Reinhard
 */
enum class UserTokenType {
    CALENDAR_REST,
    DAV_TOKEN,
    REST_CLIENT,

    /**
     * **Not** stored in [org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO]
     * anymore: there is one token per device now, see [StayLoggedInTokenDO]. The constant remains, because
     * it names a login channel and is used as such outside of the authentications table: as the namespace of
     * [org.projectforge.business.login.LoginProtection], in the user's access log (`UserAccessLogEntries`)
     * and in the i18n keys. [UserAuthenticationsDao] rejects it.
     */
    STAY_LOGGED_IN_KEY,
    AUTHENTICATOR_KEY,
}
