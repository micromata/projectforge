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


package org.projectforge.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.projectforge.business.address.AddressDao
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class McpControllerTest {
    private val mapper = ObjectMapper()
    private val full = PFUserDO().apply { id = 1L; username = "full-access" }
    private val ordinary = PFUserDO().apply { id = 2L; username = "normal-user" }
    private val allScopes = setOf("projectforge.profile.read", "projectforge.tasks.read")
    private val oauth = mock<McpSessionOAuth>().also {
        whenever(it.issuer).thenReturn("http://localhost:18080/mcp/oauth")
        whenever(it.scopeLabels).thenReturn(linkedMapOf("projectforge.profile.read" to "", "projectforge.tasks.read" to ""))
        whenever(it.readMfaValid(any(), anyOrNull())).thenReturn(true)
        whenever(it.authenticate(eq("full"), any(), any())).thenReturn(McpSessionOAuth.Identity(UserContext(full, true), allScopes))
        whenever(it.authenticate(eq("ordinary"), any(), any())).thenReturn(McpSessionOAuth.Identity(UserContext(ordinary, true), allScopes))
        whenever(it.authenticate(eq("profile-only"), any(), any()))
            .thenReturn(McpSessionOAuth.Identity(UserContext(full, true), setOf("projectforge.profile.read")))
        whenever(it.authenticate(eq("tasks-only"), any(), any()))
            .thenReturn(McpSessionOAuth.Identity(UserContext(full, true), setOf("projectforge.tasks.read")))
    }
    // ProjectForge's permission check: only the full access user may read the task.
    private val taskDao = mock<TaskDao>().also {
        whenever(it.select(any<QueryFilter>(), eq(true))).thenAnswer {
            if (ThreadLocalUserContext.requiredLoggedInUser.id != 1L) throw AccessException("access.denied")
            listOf(TaskDO().apply { id = 42L; title = "Visible task" })
        }
        whenever(it.find(eq(42L), eq(true), eq(false))).thenReturn(TaskDO().apply { id = 42L; title = "Visible task" })
    }
    private val controller = McpController(mapper, oauth, mock<ProjektDao>(), mock<KundeDao>(), taskDao, mock<TimesheetDao>(),
        mock<AddressDao>(), mock<RechnungDao>(), "http://localhost:18080/mcp", true)

    private fun request(token: String? = "full", origin: String? = null, remote: String = "127.0.0.1") = MockHttpServletRequest().apply {
        remoteAddr = remote
        token?.let { addHeader("Authorization", "Bearer $it") }
        origin?.let { addHeader("Origin", it) }
    }

    private fun call(tool: String, arguments: String = "{}") =
        mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$tool","arguments":$arguments}}""")

    private fun rpc(body: JsonNode, request: MockHttpServletRequest = request()) = controller.rpc(request, body, MockHttpServletResponse())

    private fun result(body: JsonNode, token: String = "full"): JsonNode =
        mapper.valueToTree<JsonNode>(rpc(body, request(token)).body).path("result")

    @Test
    fun `only valid session tokens from the local host and own origin are accepted`() {
        val whoami = call("projectforge_whoami")
        assertEquals(401, rpc(whoami, request(null)).statusCode.value())
        assertEquals(401, rpc(whoami, request("unknown")).statusCode.value())
        assertTrue(rpc(whoami, request(null)).headers.getFirst("WWW-Authenticate")!!.contains("resource_metadata="))
        assertEquals(403, rpc(whoami, request(origin = "https://evil.example")).statusCode.value())
        assertEquals(403, rpc(whoami, request(remote = "10.0.0.1")).statusCode.value())
        assertEquals(200, rpc(whoami, request(origin = "http://localhost:18080")).statusCode.value())
        assertTrue(result(whoami).toString().contains("full-access"))
        full.deactivated = true
        try {
            assertEquals(403, rpc(whoami).statusCode.value())
        } finally {
            full.deactivated = false
        }
    }

    @Test
    fun `reads are refused for an ordinary user by ProjectForge permissions and allowed for the user they are meant for`() {
        val search = call("projectforge_search", """{"entity":"tasks"}""")
        val allowed = result(search)
        assertFalse(allowed.path("isError").asBoolean())
        assertTrue(allowed.toString().contains("Visible task"))
        assertNull(ThreadLocalUserContext.loggedInUser)
        val denied = result(search, "ordinary")
        assertTrue(denied.path("isError").asBoolean())
        assertFalse(denied.toString().contains("Visible task"))
        assertNull(ThreadLocalUserContext.loggedInUser)
        verify(taskDao, times(2)).select(any<QueryFilter>(), eq(true))
    }

    @Test
    fun `scopes not granted and an expired second factor refuse reads before the database is asked`() {
        val search = call("projectforge_search", """{"entity":"tasks"}""")
        assertEquals(403, rpc(search, request("profile-only")).statusCode.value())
        val tools = mapper.valueToTree<JsonNode>(rpc(mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""), request("profile-only")).body)
        assertFalse(tools.toString().contains("\"tasks\""))
        whenever(oauth.readMfaValid("tasks", 42L)).thenReturn(false)
        val get = result(call("projectforge_get", """{"entity":"tasks","id":42}"""))
        assertTrue(get.path("isError").asBoolean())
        assertTrue(get.toString().contains("second factor"))
        verify(taskDao, never()).select(any<QueryFilter>(), any<Boolean>())
        verify(taskDao, never()).find(any<Long>(), any<Boolean>(), any<Boolean>())
    }

    @Test
    fun `missing scopes challenge the client with required and already granted scopes`() {
        val cases = listOf(
            call("projectforge_search", """{"entity":"tasks"}""") to "profile-only",
            call("projectforge_get", """{"entity":"tasks","id":42}""") to "profile-only",
            call("projectforge_whoami") to "tasks-only",
        )
        cases.forEach { (body, token) ->
            val response = rpc(body, request(token))
            assertEquals(403, response.statusCode.value())
            assertEquals(
                "Bearer error=\"insufficient_scope\", scope=\"projectforge.profile.read projectforge.tasks.read\", " +
                    "resource_metadata=\"http://localhost:18080/.well-known/oauth-protected-resource/mcp\"",
                response.headers.getFirst("WWW-Authenticate"),
            )
            assertEquals("no-store", response.headers.getFirst("Cache-Control"))
            assertEquals("insufficient_scope", mapper.valueToTree<JsonNode>(response.body).path("error").asText())
            assertNull(ThreadLocalUserContext.loggedInUser)
        }
        verify(taskDao, never()).select(any<QueryFilter>(), any<Boolean>())
        verify(taskDao, never()).find(any<Long>(), any<Boolean>(), any<Boolean>())
        // The same read succeeds with the needed scope; malformed entities must not enter a challenge header.
        assertFalse(result(call("projectforge_get", """{"entity":"tasks","id":42}""")).path("isError").asBoolean())
        val unknown = rpc(call("projectforge_search", """{"entity":"unknown"}"""), request("profile-only"))
        assertEquals(-32602, mapper.valueToTree<JsonNode>(unknown.body).path("error").path("code").asInt())
        assertNull(unknown.headers.getFirst("WWW-Authenticate"))
    }

    @Test
    fun `invalid protocol version headers are refused on every MCP transport method`() {
        val ping = mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}""")
        val invalidVersions = listOf(listOf("1999-01-01"), listOf("invalid"), listOf(""),
            listOf("2025-06-18", "1999-01-01"), listOf("2025-06-18, 2025-11-25"))
        invalidVersions.forEach { versions ->
            listOf("POST", "GET", "DELETE").forEach { method ->
                val request = request().apply {
                    this.method = method
                    versions.forEach { addHeader("MCP-Protocol-Version", it) }
                }
                val response = if (method == "POST") rpc(ping, request)
                    else controller.unsupported(request, MockHttpServletResponse())
                assertEquals(400, response.statusCode.value(), "$method: $versions")
                assertEquals("no-store", response.headers.getFirst("Cache-Control"))
            }
        }
        verify(oauth, never()).authenticate(any(), any(), any())
        assertNull(ThreadLocalUserContext.loggedInUser)
    }

    @Test
    fun `supported and absent protocol headers work and initialization still negotiates versions`() {
        val ping = mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}""")
        val versions = listOf("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25")
        (versions + listOf(null)).forEach { version ->
            val request = request().apply { version?.let { addHeader("MCP-Protocol-Version", it) } }
            assertEquals(200, rpc(ping, request).statusCode.value())
            assertEquals(405, controller.unsupported(request, MockHttpServletResponse()).statusCode.value())
        }
        (versions + "2099-01-01").forEach { version ->
            val initialize = mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"$version"}}""")
            val response = rpc(initialize)
            assertEquals(200, response.statusCode.value())
            assertEquals(if (version in versions) version else "2025-06-18",
                mapper.valueToTree<JsonNode>(response.body).path("result").path("protocolVersion").asText())
        }
    }
}
