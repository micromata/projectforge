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

package org.projectforge.keycloak.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for the Keycloak integration.
 *
 * ## Betriebsphasen (KeycloakMasterLoginHandler)
 *
 * **Phase 1 — PF als Master, kein Passwort-Sync** (`syncPasswords=false`, default):
 *   PF befüllt Keycloak mit Usern/Gruppen/Zuordnungen. Passwörter bleiben ausschließlich in PF.
 *   projectforge.login.handlerClass=KeycloakMasterLoginHandler
 *   projectforge.keycloak.syncPasswords=false
 *
 * **Phase 2 — PF als Master, mit Passwort-Sync** (`syncPasswords=true`):
 *   Zusätzlich werden Passwortänderungen und der erste Login nach Keycloak übertragen.
 *   projectforge.login.handlerClass=KeycloakMasterLoginHandler
 *   projectforge.keycloak.syncPasswords=true
 *
 * **Phase 3 — Keycloak als Master, SSO** (separater Handler, spätere Implementierung):
 *   Keycloak ist führendes System. Gruppen-/User-Sync von Keycloak nach PF.
 *   Passwortänderung in PF deaktiviert. Anmeldung ausschließlich über Keycloak SSO.
 *   projectforge.login.handlerClass=KeycloakLoginHandler  (+ OIDC-Konfiguration, TODO)
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.keycloak")
open class KeycloakConfig {

    /** Keycloak server base URL, e.g. https://keycloak.example.com */
    var serverUrl: String = ""

    /** Realm name */
    var realm: String = ""

    /** Service account client ID for Admin API access */
    var clientId: String = ""

    /** Service account client secret */
    var clientSecret: String = ""

    /** Page size for paginated Keycloak API calls */
    var pageSize: Int = 100

    /**
     * Phase 1→2 switch for [KeycloakMasterLoginHandler]:
     * - false (default, Phase 1): users/groups/assignments are synced to Keycloak, passwords are NOT.
     * - true (Phase 2): additionally pushes passwords to Keycloak on login and on password change.
     */
    var syncPasswords: Boolean = false

    fun isConfigured() = serverUrl.isNotBlank() && realm.isNotBlank()
            && clientId.isNotBlank() && clientSecret.isNotBlank()
}
