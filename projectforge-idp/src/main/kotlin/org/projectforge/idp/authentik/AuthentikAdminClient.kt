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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
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

    // HttpComponentsClientHttpRequestFactory is required for PATCH support —
    // Java's default HttpURLConnection does not support the PATCH method.
    // Content compression is disabled to avoid response truncation when the server
    // returns Content-Length based on the compressed size but the body is decompressed.
    private val restTemplate = RestTemplate(
        HttpComponentsClientHttpRequestFactory(
            org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                .disableContentCompression()
                .build()
        )
    )
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val apiBaseUrl: String
        get() = "${authentikConfig.serverUrl}/api/v3"

    // -------------------------------------------------------------------------
    // IdpAdminClient implementation
    // -------------------------------------------------------------------------

    override fun getAllUsers(): List<IdpUser> = apiCall("fetch all users") {
        getAllPaginated<AuthentikUser>("$apiBaseUrl/core/users/").map { it.toIdpUser() }
    }

    override fun getAllGroups(): List<IdpGroup> = apiCall("fetch all groups") {
        getAllPaginated<AuthentikGroup>("$apiBaseUrl/core/groups/").map { it.toIdpGroup() }
    }

    override fun getUserGroups(userId: String): List<IdpGroup> = apiCall("fetch groups for user '$userId'") {
        val user = getAuthentikUser(userId.toInt()) ?: return@apiCall emptyList()
        val groupPks = user.groups ?: return@apiCall emptyList()
        groupPks.mapNotNull { groupPk ->
            try {
                getAuthentikGroup(groupPk)?.toIdpGroup()
            } catch (ex: Exception) {
                log.error("Error fetching group '$groupPk' for user '$userId': ${ex.message}", ex)
                null
            }
        }
    }

    override fun getGroupMembers(groupId: String): List<IdpUser> = apiCall("fetch members of group '$groupId'") {
        val group = getAuthentikGroup(groupId) ?: return@apiCall emptyList()
        val userPks = group.users ?: return@apiCall emptyList()
        userPks.mapNotNull { userPk ->
            try {
                getAuthentikUser(userPk)?.toIdpUser()
            } catch (ex: Exception) {
                log.error("Error fetching user pk=$userPk from group '$groupId': ${ex.message}", ex)
                null
            }
        }
    }

    override fun getUserById(userId: String): IdpUser? = apiCall("fetch user '$userId'") {
        getAuthentikUser(userId.toInt())?.toIdpUser()
    }

    override fun findUserByUsername(username: String): IdpUser? = apiCall("find user by username '$username'") {
        val url = UriComponentsBuilder.fromUriString("$apiBaseUrl/core/users/")
            .queryParam("username", username)
            .build().toUriString()
        val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java)
        val body = response.body ?: return@apiCall null
        val page = parseJson<PaginatedResponse<AuthentikUser>>(body, "find user '$username'")
        page.results.firstOrNull()?.toIdpUser()
    }

    override fun createUser(user: IdpUser): String = apiCall("create user '${user.username}'") {
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
        val created = parseJson<AuthentikUser>(body, "create user '${user.username}'")
        created.pk?.toString()
            ?: throw IllegalStateException("Authentik did not return a pk after creating user '${user.username}'")
    }

    override fun updateUser(userId: String, user: IdpUser): Unit = apiCall("update user '$userId'") {
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

    override fun createGroup(group: IdpGroup): String = apiCall("create group '${group.name}'") {
        val url = "$apiBaseUrl/core/groups/"
        val payload = mapOf(
            "name" to group.name,
            "attributes" to (group.attributes?.mapValues { it.value.firstOrNull() } ?: emptyMap<String, String>()),
        )
        val response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(payload, jsonBearerHeaders()), String::class.java)
        val body = response.body ?: throw IllegalStateException("Empty response from Authentik after creating group '${group.name}'")
        val created = parseJson<AuthentikGroup>(body, "create group '${group.name}'")
        created.pk
            ?: throw IllegalStateException("Authentik did not return a pk after creating group '${group.name}'")
    }

    override fun updateGroup(groupId: String, group: IdpGroup): Unit = apiCall("update group '$groupId'") {
        val url = "$apiBaseUrl/core/groups/$groupId/"
        val payload = mutableMapOf<String, Any?>(
            "name" to group.name,
        )
        if (group.attributes != null) {
            payload["attributes"] = group.attributes.mapValues { it.value.firstOrNull() }
        }
        restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
    }

    override fun getGroup(groupId: String): IdpGroup = apiCall("fetch group '$groupId'") {
        val group = getAuthentikGroup(groupId)
            ?: throw IllegalStateException("Authentik group '$groupId' not found")
        group.toIdpGroup()
    }

    override fun addUserToGroup(userId: String, groupId: String): Unit = apiCall("add user '$userId' to group '$groupId'") {
        val group = getAuthentikGroup(groupId) ?: return@apiCall
        val currentUsers = group.users?.toMutableList() ?: mutableListOf()
        val userPk = userId.toInt()
        if (userPk !in currentUsers) {
            currentUsers.add(userPk)
            val url = "$apiBaseUrl/core/groups/$groupId/"
            val payload = mapOf("users" to currentUsers)
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
        }
    }

    override fun removeUserFromGroup(userId: String, groupId: String): Unit = apiCall("remove user '$userId' from group '$groupId'") {
        val group = getAuthentikGroup(groupId) ?: return@apiCall
        val currentUsers = group.users?.toMutableList() ?: return@apiCall
        val userPk = userId.toInt()
        if (currentUsers.remove(userPk)) {
            val url = "$apiBaseUrl/core/groups/$groupId/"
            val payload = mapOf("users" to currentUsers)
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
        }
    }

    override fun resetPassword(userId: String, password: CharArray): Unit = apiCall("reset password for user '$userId'") {
        val url = "$apiBaseUrl/core/users/$userId/set_password/"
        val payload = mapOf("password" to String(password))
        restTemplate.exchange(url, HttpMethod.POST, HttpEntity(payload, jsonBearerHeaders()), Void::class.java)
        log.info { "Password reset sent to Authentik for user id '$userId'" }
    }

    override fun isConfigured(): Boolean = authentikConfig.isConfigured()

    override fun providerName(): String = "Authentik"

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Central error handling for all Authentik API calls.
     * Translates Spring HTTP exceptions into descriptive error messages.
     */
    private fun <T> apiCall(operation: String, block: () -> T): T {
        try {
            return block()
        } catch (ex: ResourceAccessException) {
            val cause = ex.cause
            val detail = if (cause is java.net.ConnectException || cause is java.net.UnknownHostException) {
                "server not reachable at '${authentikConfig.serverUrl}'"
            } else {
                "I/O error communicating with '${authentikConfig.serverUrl}': ${cause?.message ?: ex.message}"
            }
            throw IllegalStateException(
                "Authentik $detail while trying to $operation", ex
            )
        } catch (ex: HttpClientErrorException.Unauthorized) {
            throw IllegalStateException(
                "Authentik API authentication failed (401 Unauthorized) while trying to $operation. Check projectforge.authentik.apiToken.", ex
            )
        } catch (ex: HttpClientErrorException.Forbidden) {
            throw IllegalStateException(
                "Authentik API access denied (403 Forbidden) while trying to $operation. Check API token permissions.", ex
            )
        } catch (ex: HttpClientErrorException) {
            throw IllegalStateException(
                "Authentik API error (HTTP ${ex.statusCode}) while trying to $operation: ${ex.responseBodyAsString}", ex
            )
        }
    }

    private fun getAuthentikUser(pk: Int): AuthentikUser? {
        val url = "$apiBaseUrl/core/users/$pk/"
        val body = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java).body
            ?: return null
        return parseJson(body, "user pk=$pk")
    }

    private fun getAuthentikGroup(pk: String): AuthentikGroup? {
        val url = "$apiBaseUrl/core/groups/$pk/"
        val body = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(bearerHeaders()), String::class.java).body
            ?: return null
        return parseJson(body, "group pk=$pk")
    }

    private inline fun <reified T> parseJson(body: String, context: String): T {
        try {
            return objectMapper.readValue<T>(body)
        } catch (ex: Exception) {
            val preview = if (body.length > 500) body.take(500) + "... (${body.length} chars total)" else body
            throw IllegalStateException(
                "Failed to parse Authentik response for $context (${body.length} chars): $preview", ex
            )
        }
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
            val paginatedResponse = parseJson<PaginatedResponse<T>>(body, "paginated $baseUrl page=$page")
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
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    data class PaginatedResponse<T>(
        val pagination: Pagination = Pagination(),
        val results: List<T> = emptyList(),
    )

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    data class Pagination(
        val next: Int? = null,
        val previous: Int? = null,
        val count: Int = 0,
        val current: Int = 1,
        @com.fasterxml.jackson.annotation.JsonProperty("total_pages")
        val totalPages: Int = 1,
        @com.fasterxml.jackson.annotation.JsonProperty("start_index")
        val startIndex: Int = 1,
        @com.fasterxml.jackson.annotation.JsonProperty("end_index")
        val endIndex: Int = 0,
    )
}
