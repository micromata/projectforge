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

package org.projectforge.idp.keycloak

import mu.KotlinLogging
import org.projectforge.idp.IdpAdminClient
import org.projectforge.idp.model.IdpGroup
import org.projectforge.idp.model.IdpUser
import org.projectforge.idp.keycloak.model.KeycloakCredential
import org.projectforge.idp.keycloak.model.KeycloakGroup
import org.projectforge.idp.keycloak.model.KeycloakUser
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

private val log = KotlinLogging.logger {}

/**
 * Keycloak implementation of [IdpAdminClient].
 * Uses Spring RestTemplate with a Bearer token on every request.
 *
 * Activated when projectforge.idp.provider=keycloak (default).
 */
@Service
@ConditionalOnProperty(name = ["projectforge.idp.provider"], havingValue = "keycloak", matchIfMissing = true)
open class KeycloakAdminClient(
    private val keycloakConfig: KeycloakConfig,
    private val tokenClient: KeycloakTokenClient
) : IdpAdminClient {

    private val restTemplate = RestTemplate()

    private val adminBaseUrl: String
        get() = "${keycloakConfig.serverUrl}/admin/realms/${keycloakConfig.realm}"

    // -------------------------------------------------------------------------
    // IdpAdminClient implementation
    // -------------------------------------------------------------------------

    override fun getAllUsers(): List<IdpUser> =
        getAllKcUsers().map { it.toIdpUser() }

    override fun getAllGroups(): List<IdpGroup> =
        getAllKcGroups().map { it.toIdpGroup() }

    override fun getUserGroups(userId: String): List<IdpGroup> {
        val url = "$adminBaseUrl/users/$userId/groups"
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), Array<KeycloakGroup>::class.java)
        return response.body?.map { it.toIdpGroup() } ?: emptyList()
    }

    override fun getGroupMembers(groupId: String): List<IdpUser> =
        getAll("$adminBaseUrl/groups/$groupId/members", Array<KeycloakUser>::class.java).map { it.toIdpUser() }

    override fun getUserById(userId: String): IdpUser? {
        val url = "$adminBaseUrl/users/$userId"
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), KeycloakUser::class.java)
        return response.body?.toIdpUser()
    }

    override fun findUserByUsername(username: String): IdpUser? {
        val url = UriComponentsBuilder.fromUriString("$adminBaseUrl/users")
            .queryParam("username", username)
            .queryParam("exact", true)
            .build().toUriString()
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), Array<KeycloakUser>::class.java)
        return response.body?.firstOrNull()?.toIdpUser()
    }

    override fun createUser(user: IdpUser): String {
        val kcUser = user.toKeycloakUser()
        val url = "$adminBaseUrl/users"
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(kcUser, jsonBearerHeaders()), Void::class.java)
        val location = response.headers.location?.toString()
            ?: throw IllegalStateException("Keycloak did not return a Location header after creating user '${user.username}'")
        return location.substringAfterLast("/")
    }

    override fun updateUser(userId: String, user: IdpUser) {
        val kcUser = user.toKeycloakUser().copy(id = userId)
        val url = "$adminBaseUrl/users/$userId"
        restTemplate.exchange(url, HttpMethod.PUT, HttpEntity(kcUser, jsonBearerHeaders()), Void::class.java)
    }

    override fun createGroup(group: IdpGroup): String {
        val kcGroup = group.toKeycloakGroup()
        val url = "$adminBaseUrl/groups"
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(kcGroup, jsonBearerHeaders()), Void::class.java)
        val location = response.headers.location?.toString()
            ?: throw IllegalStateException("Keycloak did not return a Location header after creating group '${group.name}'")
        return location.substringAfterLast("/")
    }

    override fun updateGroup(groupId: String, group: IdpGroup) {
        val kcGroup = group.toKeycloakGroup().copy(id = groupId)
        val url = "$adminBaseUrl/groups/$groupId"
        restTemplate.exchange(url, HttpMethod.PUT, HttpEntity(kcGroup, jsonBearerHeaders()), Void::class.java)
    }

    override fun getGroup(groupId: String): IdpGroup {
        val url = "$adminBaseUrl/groups/$groupId"
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), KeycloakGroup::class.java)
        return response.body?.toIdpGroup() ?: throw IllegalStateException("Empty response for group id '$groupId'")
    }

    override fun addUserToGroup(userId: String, groupId: String) {
        val url = "$adminBaseUrl/users/$userId/groups/$groupId"
        restTemplate.exchange(url, HttpMethod.PUT, HttpEntity<Void>(bearerHeaders()), Void::class.java)
    }

    override fun removeUserFromGroup(userId: String, groupId: String) {
        val url = "$adminBaseUrl/users/$userId/groups/$groupId"
        restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity<Void>(bearerHeaders()), Void::class.java)
    }

    override fun resetPassword(userId: String, password: CharArray) {
        val url = "$adminBaseUrl/users/$userId/reset-password"
        val passwordString = String(password)
        try {
            val credential = KeycloakCredential(value = passwordString)
            restTemplate.exchange(url, HttpMethod.PUT, HttpEntity(credential, jsonBearerHeaders()), Void::class.java)
        } finally {
            log.info { "Password reset sent to Keycloak for user id '$userId'" }
        }
    }

    override fun isConfigured(): Boolean = keycloakConfig.isConfigured()

    override fun providerName(): String = "Keycloak"

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun getAllKcUsers(): List<KeycloakUser> =
        getAll("$adminBaseUrl/users", Array<KeycloakUser>::class.java)

    private fun getAllKcGroups(): List<KeycloakGroup> =
        getAll("$adminBaseUrl/groups", Array<KeycloakGroup>::class.java)

    private fun bearerHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(tokenClient.getAccessToken())
    }

    private fun jsonBearerHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(tokenClient.getAccessToken())
        contentType = MediaType.APPLICATION_JSON
    }

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

    // -------------------------------------------------------------------------
    // DTO conversion helpers
    // -------------------------------------------------------------------------

    private fun KeycloakUser.toIdpUser() = IdpUser(
        id = id, username = username, firstName = firstName,
        lastName = lastName, email = email, enabled = enabled,
        attributes = attributes,
    )

    private fun IdpUser.toKeycloakUser() = KeycloakUser(
        id = id, username = username, firstName = firstName,
        lastName = lastName, email = email, enabled = enabled,
        attributes = attributes,
    )

    private fun KeycloakGroup.toIdpGroup() = IdpGroup(
        id = id, name = name, attributes = attributes,
    )

    private fun IdpGroup.toKeycloakGroup() = KeycloakGroup(
        id = id, name = name, attributes = attributes,
    )
}
