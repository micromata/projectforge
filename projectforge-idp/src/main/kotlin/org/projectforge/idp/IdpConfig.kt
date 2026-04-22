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

package org.projectforge.idp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Shared configuration properties for the IdP integration.
 *
 * The [provider] discriminator selects which [IdpAdminClient] implementation is activated
 * (e.g. "keycloak" or "authentik"). Provider-specific connection details live under
 * their own prefix (projectforge.keycloak.*, projectforge.authentik.*).
 *
 * ## Betriebsphasen
 *
 * **Phase 1 — PF als Master, kein Passwort-Sync** (`syncPasswords=false`, default):
 *   PF befüllt den IdP mit Usern/Gruppen/Zuordnungen. Passwörter bleiben ausschließlich in PF.
 *   projectforge.login.handlerClass=IdpMasterLoginHandler
 *
 * **Phase 2 — PF als Master, mit Passwort-Sync** (`syncPasswords=true`):
 *   Zusätzlich werden Passwortänderungen und der erste Login zum IdP übertragen.
 *   projectforge.login.handlerClass=IdpMasterLoginHandler
 *
 * **Phase 3 — IdP als Master, SSO**:
 *   Der IdP ist führendes System. Gruppen-/User-Sync vom IdP nach PF.
 *   projectforge.login.handlerClass=IdpLoginHandler
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.idp")
open class IdpConfig {

    /** Identity provider type: "keycloak" or "authentik". */
    var provider: String = "keycloak"

    /** Page size for paginated IdP API calls. */
    var pageSize: Int = 100

    /**
     * User attribute mappings: key = PF field name, value = IdP attribute name.
     *
     * Supported PF field names:
     *   jiraUsername, mobilePhone, organization, description, gender,
     *   nickname, locale, timeZone, personalPhoneIdentifiers
     */
    var userAttributes: Map<String, String> = emptyMap()

    /**
     * Group attribute mappings: key = PF field name, value = IdP attribute name.
     *
     * Supported PF field names: description, organization
     */
    var groupAttributes: Map<String, String> = emptyMap()

    /**
     * Phase 1→2 switch:
     * - false (default, Phase 1): users/groups/assignments are synced to IdP, passwords are NOT.
     * - true (Phase 2): additionally pushes passwords to IdP on login and on password change.
     */
    var syncPasswords: Boolean = false

    /**
     * IdP attribute name for the WLAN (Samba NT) password hash.
     * If set, the NT hash is written to this IdP user attribute on every WLAN password change.
     */
    var wlanPasswordAttribute: String? = null
}
