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

package org.projectforge.keycloak.client

import mu.KotlinLogging
import org.projectforge.keycloak.config.KeycloakConfig
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.time.Instant

private val log = KotlinLogging.logger {}

/**
 * Fetches and caches OAuth2 client_credentials tokens from Keycloak.
 * Tokens are renewed automatically 30 seconds before expiry.
 */
@Service
open class KeycloakTokenClient(private val keycloakConfig: KeycloakConfig) {

    private val restTemplate = RestTemplate()

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var tokenExpiresAt: Instant = Instant.EPOCH

    /**
     * Returns a valid Bearer token for the Keycloak Admin API.
     * Thread-safe: renews the token if expired or about to expire.
     */
    @Synchronized
    fun getAccessToken(): String {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken!!
        }
        log.debug("Fetching new Keycloak access token for realm '${keycloakConfig.realm}'")
        val tokenUrl = "${keycloakConfig.serverUrl}/realms/${keycloakConfig.realm}/protocol/openid-connect/token"
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", keycloakConfig.clientId)
            add("client_secret", keycloakConfig.clientSecret)
        }
        val response = restTemplate.postForObject(
            tokenUrl,
            HttpEntity(body, headers),
            Map::class.java
        ) ?: throw IllegalStateException("Empty token response from Keycloak at $tokenUrl")

        val accessToken = response["access_token"] as? String
            ?: throw IllegalStateException("No 'access_token' in Keycloak token response")
        val expiresIn = (response["expires_in"] as? Number)?.toLong() ?: 60L

        cachedToken = accessToken
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 30)
        log.debug("Keycloak access token obtained, expires in ${expiresIn}s")
        return accessToken
    }
}
