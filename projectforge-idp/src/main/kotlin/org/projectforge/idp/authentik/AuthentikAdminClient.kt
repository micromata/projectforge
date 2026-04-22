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

package org.projectforge.idp.authentik

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.projectforge.idp.IdpAdminClient
import org.projectforge.idp.model.IdpGroup
import org.projectforge.idp.model.IdpUser
import org.projectforge.idp.authentik.model.AuthentikGroup
import org.projectforge.idp.authentik.model.AuthentikUser
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
 * Authentik implementation of [IdpAdminClient].
 * Uses Authentik's Core API v3 with API token authentication.
 *
 * API documentation: https://goauthentik.io/docs/developer-docs/api/
 *
 * Key differences from Keycloak:
 * - Uses API token (Bearer) instead of OAuth2 client_credentials
 * - Users have `pk` (integer) instead of UUID `id`
 * - Groups contain a `users` array (membership stored on group, not separate endpoint)
 * - Pagination uses `page`/`page_size` parameters with a `pagination.next` URL
 * - Password set via POST /api/v3/core/users/{id}/set_password/
 *
 * Activated when projectforge.idp.provider=authentik.
 */
@Service
@ConditionalOnProperty(name = ["projectforge.idp.provider"], havingValue = "authentik")
open class AuthentikAdminClient(
    private val authentikConfig: AuthentikConfig
) : IdpAdminClient {

    private val restTemplate = RestTemplate()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val apiBaseUrl: String
        get() = "${authentikConfig.serverUrl}/api/v3"

    // -------------------------------------------------------------------------
    // IdpAdminClient implementation
    // -------------------------------------------------------------------------

    override fun getAllUsers(): List<IdpUser> =
        getAllPaginated<AuthentikUser>("$apiBaseUrl/core/users/").map { it.toIdpUser() }

    override fun getAllGroups(): List<IdpGroup> =
        getAllPaginated<AuthentikGroup>("$apiBaseUrl/core/groups/").map { it.toIdpGroup() }

    override fun getUserGroups(userId: String): List<IdpGroup> {
        // Authentik stores group membership on the group (users array), not on the user.
        // We fetch the user to get their groups list, then look up each group.
        val user = getAuthentikUser(userId.toInt()) ?: return emptyList()
        val groupPks = user.groups ?: return emptyList()
        return groupPks.mapNotNull { groupPk ->
            try {
                getAuthentikGroup(groupPk)?.toIdpGroup()
            } catch (ex: Exception) {
                log.error("Error fetching group '$groupPk' for user '$userId': ${ex.message}", ex)
                null
            }
        }
    }

    override fun getGroupMembers(groupId: String): List<IdpUser> {
        // Authentik groups have a `users` array with user PKs.
        val group = getAuthentikGroup(groupId) ?: return emptyList()
        val userPks = group.users ?: return emptyList()
        return userPks.mapNotNull { userPk ->
            try {
                getAuthentikUser(userPk)?.toIdpUser()
            } catch (ex: Exception) {
                log.error("Error fetching user pk=$userPk from group '$groupId': ${ex.message}", ex)
                null
            }
        }
    }

    override fun getUserById(userId: String): IdpUser? =
        getAuthentikUser(userId.toInt())?.toIdpUser()

    override fun findUserByUsername(username: String): IdpUser? {
        val url = UriComponentsBuilder.fromUriString("$apiBaseUrl/core/users/")
            .queryParam("username", username)
            .build().toUriString()
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java)
        val body = response.body ?: return null
        val page = objectMapper.readValue<PaginatedResponse<AuthentikUser>>(body)
        return page.results.firstOrNull()?.toIdpUser()
    }

    override fun createUser(user: IdpUser): String {
        val url = "$apiBaseUrl/core/users/"
        val payload = mapOf(
            "username" to user.username,
            "name" to "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifBlank { user.username },
            "email" to (user.email ?: ""),
            "is_active" to user.enabled,
            "attributes" to (user.attributes?.mapValues { it.value.firstOrNull() } ?: emptyMap<String, String>()),
        )
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(payload, jsonBearerHeaders()), String::class.java)
        val body = response.body ?: throw IllegalStateException("Empty response from Authentik after creating user '${user.username}'")
        val created = objectMapper.readValue<AuthentikUser>(body)
        return created.pk?.toString()
            ?: throw IllegalStateException("Authentik did not return a pk after creating user '${user.username}'")
    }

    override fun updateUser(userId: String, user: IdpUser) {
        val url = "$apiBaseUrl/core/users/$userId/"
        val payload = mutableMapOf<String, Any?>(
            "username" to user.username,
            "name" to "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifBlank { user.username },
            "email" to (user.email ?: ""),
            "is_active" to user.enabled,
        )
        if (user.attributes != null) {
            payload["attributes"] = user.attributes.mapValues { it.value.firstOrNull() }
        }
        restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
    }

    override fun createGroup(group: IdpGroup): String {
        val url = "$apiBaseUrl/core/groups/"
        val payload = mapOf(
            "name" to group.name,
            "attributes" to (group.attributes?.mapValues { it.value.firstOrNull() } ?: emptyMap<String, String>()),
        )
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(payload, jsonBearerHeaders()), String::class.java)
        val body = response.body ?: throw IllegalStateException("Empty response from Authentik after creating group '${group.name}'")
        val created = objectMapper.readValue<AuthentikGroup>(body)
        return created.pk
            ?: throw IllegalStateException("Authentik did not return a pk after creating group '${group.name}'")
    }

    override fun updateGroup(groupId: String, group: IdpGroup) {
        val url = "$apiBaseUrl/core/groups/$groupId/"
        val payload = mutableMapOf<String, Any?>(
            "name" to group.name,
        )
        if (group.attributes != null) {
            payload["attributes"] = group.attributes.mapValues { it.value.firstOrNull() }
        }
        restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
    }

    override fun getGroup(groupId: String): IdpGroup {
        val group = getAuthentikGroup(groupId)
            ?: throw IllegalStateException("Authentik group '$groupId' not found")
        return group.toIdpGroup()
    }

    override fun addUserToGroup(userId: String, groupId: String) {
        // Authentik: PATCH group to add user to the users array
        val group = getAuthentikGroup(groupId) ?: return
        val currentUsers = group.users?.toMutableList() ?: mutableListOf()
        val userPk = userId.toInt()
        if (userPk !in currentUsers) {
            currentUsers.add(userPk)
            val url = "$apiBaseUrl/core/groups/$groupId/"
            val payload = mapOf("users" to currentUsers)
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
        }
    }

    override fun removeUserFromGroup(userId: String, groupId: String) {
        val group = getAuthentikGroup(groupId) ?: return
        val currentUsers = group.users?.toMutableList() ?: return
        val userPk = userId.toInt()
        if (currentUsers.remove(userPk)) {
            val url = "$apiBaseUrl/core/groups/$groupId/"
            val payload = mapOf("users" to currentUsers)
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
        }
    }

    override fun resetPassword(userId: String, password: CharArray) {
        val url = "$apiBaseUrl/core/users/$userId/set_password/"
        val passwordString = String(password)
        try {
            val payload = mapOf("password" to passwordString)
            restTemplate.exchange(url, HttpMethod.POST, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
        } finally {
            log.info { "Password reset sent to Authentik for user id '$userId'" }
        }
    }

    override fun isConfigured(): Boolean = authentikConfig.isConfigured()

    override fun providerName(): String = "Authentik"

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun getAuthentikUser(pk: Int): AuthentikUser? {
        val url = "$apiBaseUrl/core/users/$pk/"
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java)
        return response.body?.let { objectMapper.readValue<AuthentikUser>(it) }
    }

    private fun getAuthentikGroup(pk: String): AuthentikGroup? {
        val url = "$apiBaseUrl/core/groups/$pk/"
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java)
        return response.body?.let { objectMapper.readValue<AuthentikGroup>(it) }
    }

    private fun bearerHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(authentikConfig.apiToken)
    }

    private fun jsonBearerHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(authentikConfig.apiToken)
        contentType = MediaType.APPLICATION_JSON
    }

    /**
     * Fetches all items from a paginated Authentik endpoint.
     * Authentik uses page/page_size parameters and returns a pagination object with a next URL.
     */
    private inline fun <reified T> getAllPaginated(baseUrl: String): List<T> {
        val result = mutableListOf<T>()
        var page = 1
        val pageSize = authentikConfig.pageSize
        while (true) {
            val url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .build().toUriString()
            val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java)
            val body = response.body ?: break
            val paginatedResponse = objectMapper.readValue<PaginatedResponse<T>>(body)
            result.addAll(paginatedResponse.results)
            if (paginatedResponse.pagination.next == null || paginatedResponse.results.size < pageSize) break
            page++
        }
        return result
    }

    // -------------------------------------------------------------------------
    // DTO conversion helpers
    // -------------------------------------------------------------------------

    private fun AuthentikUser.toIdpUser(): IdpUser {
        val (firstName, lastName) = splitName(name)
        // Convert Authentik's flat attributes to IdP's List<String> format
        val idpAttrs = attributes?.mapValues { (_, v) ->
            when (v) {
                is List<*> -> v.map { it?.toString() ?: "" }
                else -> listOf(v?.toString() ?: "")
            }
        }
        return IdpUser(
            id = pk?.toString(),
            username = username,
            firstName = firstName,
            lastName = lastName,
            email = email,
            enabled = isActive,
            attributes = idpAttrs,
        )
    }

    private fun AuthentikGroup.toIdpGroup(): IdpGroup {
        val idpAttrs = attributes?.mapValues { (_, v) ->
            when (v) {
                is List<*> -> v.map { it?.toString() ?: "" }
                else -> listOf(v?.toString() ?: "")
            }
        }
        return IdpGroup(
            id = pk,
            name = name,
            attributes = idpAttrs,
        )
    }

    /**
     * Splits a display name into first/last name.
     * Authentik stores a single `name` field, not separate first/last.
     */
    private fun splitName(name: String?): Pair<String?, String?> {
        if (name.isNullOrBlank()) return null to null
        val parts = name.trim().split("\\s+".toRegex(), limit = 2)
        return parts[0] to parts.getOrNull(1)
    }

    /**
     * Authentik paginated response wrapper.
     */
    data class PaginatedResponse<T>(
        val pagination: Pagination = Pagination(),
        val results: List<T> = emptyList(),
    )

    data class Pagination(
        val next: Int? = null,
        val previous: Int? = null,
        val count: Int = 0,
        val current: Int = 1,
        @com.fasterxml.jackson.annotation.JsonProperty("total_pages")
        val totalPages: Int = 1,
    )
}
