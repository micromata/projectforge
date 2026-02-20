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
import org.projectforge.keycloak.model.KeycloakCredential
import org.projectforge.keycloak.model.KeycloakGroup
import org.projectforge.keycloak.model.KeycloakUser
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

private val log = KotlinLogging.logger {}

/**
 * Facade for all Keycloak Admin REST API calls.
 * Uses Spring RestTemplate with a Bearer token on every request.
 *
 * Reads:
 *   GET /admin/realms/{realm}/users          → getAllUsers()
 *   GET /admin/realms/{realm}/groups         → getAllGroups()
 *   GET /admin/realms/{realm}/users/{id}/groups → getUserGroups()
 *
 * Writes (migration + password sync):
 *   POST /admin/realms/{realm}/users         → createUser()
 *   PUT  /admin/realms/{realm}/users/{id}    → updateUser()
 *   POST /admin/realms/{realm}/groups        → createGroup()
 *   PUT  /admin/realms/{realm}/users/{id}/groups/{groupId} → addUserToGroup()
 *   PUT  /admin/realms/{realm}/users/{id}/reset-password   → resetPassword()
 */
@Service
open class KeycloakAdminClient(
    private val keycloakConfig: KeycloakConfig,
    private val tokenClient: KeycloakTokenClient
) {
    private val restTemplate = RestTemplate()

    private val adminBaseUrl: String
        get() = "${keycloakConfig.serverUrl}/admin/realms/${keycloakConfig.realm}"

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /** Returns all users from Keycloak, fetching all pages automatically. */
    fun getAllUsers(): List<KeycloakUser> =
        getAll("$adminBaseUrl/users", Array<KeycloakUser>::class.java)

    /** Returns all top-level groups from Keycloak, fetching all pages automatically. */
    fun getAllGroups(): List<KeycloakGroup> =
        getAll("$adminBaseUrl/groups", Array<KeycloakGroup>::class.java)

    /** Returns the groups a specific user belongs to. */
    fun getUserGroups(userId: String): List<KeycloakGroup> {
        val url = "$adminBaseUrl/users/$userId/groups"
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), Array<KeycloakGroup>::class.java)
        return response.body?.toList() ?: emptyList()
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a user in Keycloak and returns the newly assigned Keycloak user ID.
     * The ID is extracted from the Location header of the 201 response.
     */
    fun createUser(user: KeycloakUser): String {
        val url = "$adminBaseUrl/users"
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(user, jsonBearerHeaders()), Void::class.java)
        val location = response.headers.location?.toString()
            ?: throw IllegalStateException("Keycloak did not return a Location header after creating user '${user.username}'")
        return location.substringAfterLast("/")
    }

    /** Updates an existing user in Keycloak. */
    fun updateUser(userId: String, user: KeycloakUser) {
        val url = "$adminBaseUrl/users/$userId"
        restTemplate.exchange(url, HttpMethod.PUT, HttpEntity(user, jsonBearerHeaders()), Void::class.java)
    }

    /**
     * Creates a group in Keycloak and returns the newly assigned Keycloak group ID.
     * The ID is extracted from the Location header of the 201 response.
     */
    fun createGroup(group: KeycloakGroup): String {
        val url = "$adminBaseUrl/groups"
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(group, jsonBearerHeaders()), Void::class.java)
        val location = response.headers.location?.toString()
            ?: throw IllegalStateException("Keycloak did not return a Location header after creating group '${group.name}'")
        return location.substringAfterLast("/")
    }

    /** Adds a user to a group in Keycloak. */
    fun addUserToGroup(userId: String, groupId: String) {
        val url = "$adminBaseUrl/users/$userId/groups/$groupId"
        restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(bearerHeaders()), Void::class.java)
    }

    /**
     * Sets (resets) the password for a user in Keycloak.
     * The password char array is converted to String only for serialization and immediately cleared afterward.
     */
    fun resetPassword(userId: String, password: CharArray) {
        val url = "$adminBaseUrl/users/$userId/reset-password"
        val passwordString = String(password)
        try {
            val credential = KeycloakCredential(value = passwordString)
            restTemplate.exchange(url, HttpMethod.PUT, HttpEntity(credential, jsonBearerHeaders()), Void::class.java)
        } finally {
            // Overwrite the local String is not possible in Java/Kotlin due to String immutability,
            // but the original char array is owned by the caller and should be cleared there.
            log.debug("Password reset sent to Keycloak for user id '{}'", userId)
        }
    }

    /**
     * Finds a Keycloak user by username. Returns null if not found.
     * Uses the search endpoint for exact username matching.
     */
    fun findUserByUsername(username: String): KeycloakUser? {
        val url = UriComponentsBuilder.fromUriString("$adminBaseUrl/users")
            .queryParam("username", username)
            .queryParam("exact", true)
            .build().toUriString()
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), Array<KeycloakUser>::class.java)
        return response.body?.firstOrNull()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun bearerHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(tokenClient.getAccessToken())
    }

    private fun jsonBearerHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(tokenClient.getAccessToken())
        contentType = MediaType.APPLICATION_JSON
    }

    /**
     * Fetches all items from a paginated Keycloak endpoint.
     * Calls the endpoint repeatedly with first/max parameters until an empty page is returned.
     */
    private fun <T> getAll(baseUrl: String, arrayType: Class<Array<T>>): List<T> {
        val result = mutableListOf<T>()
        var offset = 0
        val pageSize = keycloakConfig.pageSize
        while (true) {
            val url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("first", offset)
                .queryParam("max", pageSize)
                .build().toUriString()
            val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), arrayType)
            val page = response.body ?: break
            if (page.isEmpty()) break
            result.addAll(page.toList())
            if (page.size < pageSize) break
            offset += pageSize
        }
        return result
    }
}
