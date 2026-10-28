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
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterElement.FilterType
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class McpControllerTest {
    private val mapper = ObjectMapper()
    private val full = PFUserDO().apply { id = 1L; username = "full-access" }
    private val ordinary = PFUserDO().apply { id = 2L; username = "normal-user" }
    private val allScopes = setOf("projectforge.profile.read", "projectforge.tasks.read", "projectforge.timesheets.read",
        "projectforge.addresses.read")
    private val oauth = mock<McpSessionOAuth>().also {
        whenever(it.issuer).thenReturn("https://projectforge.example.org/mcp/oauth")
        whenever(it.scopeLabels).thenReturn(linkedMapOf("projectforge.profile.read" to "", "projectforge.tasks.read" to ""))
        whenever(it.readMfaValid(any(), anyOrNull())).thenReturn(true)
        whenever(it.authenticate(eq("full"), any(), any())).thenReturn(McpSessionOAuth.Identity(UserContext(full, true), allScopes))
        whenever(it.authenticate(eq("ordinary"), any(), any())).thenReturn(McpSessionOAuth.Identity(UserContext(ordinary, true), allScopes))
        whenever(it.authenticate(eq("profile-only"), any(), any()))
            .thenReturn(McpSessionOAuth.Identity(UserContext(full, true), setOf("projectforge.profile.read")))
        whenever(it.authenticate(eq("tasks-only"), any(), any()))
            .thenReturn(McpSessionOAuth.Identity(UserContext(full, true), setOf("projectforge.tasks.read")))
    }
    private data class Row(val id: Long, val title: String, val description: String? = null)
    private val timesheetFilters = listOf(
        UIFilterElement("period", FilterType.DATE),
        UIFilterElement("startTime", FilterType.TIMESTAMP),
        UIFilterObjectElement("user", autoCompletion = AutoCompletion.getAutoCompletion4Users()),
        UIFilterObjectElement("task"),
        UIFilterListElement("status", values = listOf(UISelectValue("OPEN", "Open"), UISelectValue("CLOSED", "Closed"))),
        UIFilterListElement("projects"),
        UIFilterElement("description"),
        UIFilterElement("onlyBillable", FilterType.BOOLEAN),
    )
    // ProjectForge's permission check (in the rest classes and DAOs): only the full access user may read the task.
    private val datasets = mock<McpDatasets>().also {
        whenever(it.names).thenReturn(linkedSetOf("projects", "customers", "tasks", "timesheets", "addresses", "invoices"))
        whenever(it.search(eq("tasks"), any())).thenAnswer {
            if (ThreadLocalUserContext.requiredLoggedInUser.id != 1L) throw AccessException("access.denied")
            listOf(Row(42L, "Visible task"))
        }
        whenever(it.get("tasks", 42L)).thenReturn(Row(42L, "Visible task"))
        whenever(it.search(eq("timesheets"), any())).thenReturn(emptyList())
        whenever(it.filterElements("timesheets")).thenReturn(timesheetFilters)
        whenever(it.defaultSort(any())).thenReturn(SortProperty("startTime", SortOrder.DESCENDING))
        whenever(it.sortable(any(), any())).thenAnswer { invocation -> invocation.getArgument<String>(1) in setOf("id", "startTime", "description") }
    }
    private val controller = McpController(mapper, oauth, datasets, "https://projectforge.example.org/mcp")

    private fun request(token: String? = "full", origin: String? = null) = MockHttpServletRequest().apply {
        token?.let { addHeader("Authorization", "Bearer $it") }
        origin?.let { addHeader("Origin", it) }
    }

    private fun call(tool: String, arguments: String = "{}") =
        mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$tool","arguments":$arguments}}""")

    private fun rpc(body: JsonNode, request: MockHttpServletRequest = request()) = controller.rpc(request, body, MockHttpServletResponse())

    private fun result(body: JsonNode, token: String = "full"): JsonNode =
        mapper.valueToTree<JsonNode>(rpc(body, request(token)).body).path("result")

    /** The JSON a tool returned as its text content. */
    private fun content(body: JsonNode, token: String = "full"): JsonNode =
        mapper.readTree(result(body, token).path("content").path(0).path("text").asText())

    @Test
    fun `only valid session tokens from the own origin are accepted`() {
        val describe = call("projectforge_describe")
        assertEquals(401, rpc(describe, request(null)).statusCode.value())
        assertEquals(401, rpc(describe, request("unknown")).statusCode.value())
        assertTrue(rpc(describe, request(null)).headers.getFirst("WWW-Authenticate")!!.contains("resource_metadata="))
        assertEquals(403, rpc(describe, request(origin = "https://evil.example")).statusCode.value())
        assertEquals(200, rpc(describe, request(origin = "https://projectforge.example.org")).statusCode.value())
        assertTrue(result(describe).toString().contains("full-access"))
        full.deactivated = true
        try {
            assertEquals(403, rpc(describe).statusCode.value())
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
        verify(datasets, times(2)).search(eq("tasks"), any())
    }

    @Test
    fun `scopes not granted and an expired second factor refuse reads before the database is asked`() {
        val search = call("projectforge_search", """{"entity":"tasks"}""")
        assertEquals(403, rpc(search, request("profile-only")).statusCode.value())
        val tools = mapper.valueToTree<JsonNode>(rpc(mapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""), request("profile-only")).body)
        assertFalse(tools.toString().contains("\"tasks\""))
        assertFalse(content(call("projectforge_describe"), "profile-only").toString().contains("\"tasks\""))
        whenever(oauth.readMfaValid("tasks", 42L)).thenReturn(false)
        val get = result(call("projectforge_get", """{"entity":"tasks","id":42}"""))
        assertTrue(get.path("isError").asBoolean())
        assertTrue(get.toString().contains("second factor"))
        verify(datasets, never()).search(any(), any())
        verify(datasets, never()).get(any(), any())
    }

    @Test
    fun `missing scopes challenge the client with required and already granted scopes`() {
        val cases = listOf(
            call("projectforge_search", """{"entity":"tasks"}""") to "profile-only",
            call("projectforge_get", """{"entity":"tasks","id":42}""") to "profile-only",
            call("projectforge_describe", """{"entity":"tasks"}""") to "profile-only",
            call("projectforge_describe") to "tasks-only",
        )
        cases.forEach { (body, token) ->
            val response = rpc(body, request(token))
            assertEquals(403, response.statusCode.value())
            assertEquals(
                "Bearer error=\"insufficient_scope\", scope=\"projectforge.profile.read projectforge.tasks.read\", " +
                    "resource_metadata=\"https://projectforge.example.org/.well-known/oauth-protected-resource/mcp\"",
                response.headers.getFirst("WWW-Authenticate"),
            )
            assertEquals("no-store", response.headers.getFirst("Cache-Control"))
            assertEquals("insufficient_scope", mapper.valueToTree<JsonNode>(response.body).path("error").asText())
            assertNull(ThreadLocalUserContext.loggedInUser)
        }
        verify(datasets, never()).search(any(), any())
        verify(datasets, never()).get(any(), any())
        // The same read succeeds with the needed scope; malformed entities must not enter a challenge header.
        assertFalse(result(call("projectforge_get", """{"entity":"tasks","id":42}""")).path("isError").asBoolean())
        val unknown = rpc(call("projectforge_search", """{"entity":"unknown"}"""), request("profile-only"))
        assertEquals(-32602, mapper.valueToTree<JsonNode>(unknown.body).path("error").path("code").asInt())
        assertNull(unknown.headers.getFirst("WWW-Authenticate"))
    }

    @Test
    fun `filters become the entries the list page sends, me is the logged-in user`() {
        val search = call("projectforge_search", """{"entity":"timesheets","query":"release","offset":10,"limit":5,
            "sort":[{"field":"description"}],"filters":{"period":{"from":"2026-10-19","to":"2026-10-25"},"startTime":"2026-10-20",
            "user":"me","task":4711,"status":["OPEN"],"projects":[12,"pg:group"],"description":"Review","onlyBillable":true}}""")
        assertFalse(result(search).path("isError").asBoolean())
        val filter = argumentCaptor<MagicFilter>().also { verify(datasets).search(eq("timesheets"), it.capture()) }.firstValue
        assertEquals("release", filter.searchString)
        assertEquals(16, filter.maxRows)
        assertEquals(listOf("description" to SortOrder.ASCENDING, "id" to SortOrder.DESCENDING), filter.sortProperties.map { it.property to it.sortOrder })
        val entries = filter.entries.associateBy { it.field }
        assertEquals("2026-10-19" to "2026-10-25", entries["period"]!!.value.let { it.fromValue to it.toValue })
        assertEquals("2026-10-20T00:00:00" to "2026-10-20T23:59:59", entries["startTime"]!!.value.let { it.fromValue to it.toValue })
        assertEquals(1L, entries["user"]!!.value.id)
        assertEquals(4711L, entries["task"]!!.value.id)
        assertEquals(listOf("OPEN"), entries["status"]!!.value.values!!.toList())
        assertEquals(listOf("12", "pg:group"), entries["projects"]!!.value.values!!.toList())
        assertEquals("Review", entries["description"]!!.value.value)
        assertEquals("true", entries["onlyBillable"]!!.value.value)
        // Without a sort the entity's default applies, the id last for stable pages.
        rpc(call("projectforge_search", """{"entity":"timesheets","filters":{"user":"me"}}"""))
        val default = argumentCaptor<MagicFilter>().also { verify(datasets, times(2)).search(eq("timesheets"), it.capture()) }.secondValue
        assertEquals(listOf("startTime" to SortOrder.DESCENDING, "id" to SortOrder.DESCENDING), default.sortProperties.map { it.property to it.sortOrder })
        assertNull(ThreadLocalUserContext.loggedInUser)
    }

    @Test
    fun `unknown filter fields, invalid values and unsortable fields are refused before the database is asked`() {
        listOf(
            """{"unknown":"x"}""", """{"privatePhone":"123"}""", """{"user":"someone"}""", """{"task":"me"}""", """{"task":-1}""",
            """{"status":["DELETED"]}""", """{"status":[]}""", """{"period":"last week"}""", """{"period":{}}""",
            """{"period":{"from":"2026-10-19","until":"2026-10-25"}}""", """{"period":"2026-10-19T08:00"}""",
            """{"startTime":"2026-10-20 08:00"}""", """{"onlyBillable":"yes"}""", """{"description":""}""", """{"description":{"a":1}}""",
        ).forEach { filters ->
            val response = rpc(call("projectforge_search", """{"entity":"timesheets","filters":$filters}"""))
            assertEquals(-32602, mapper.valueToTree<JsonNode>(response.body).path("error").path("code").asInt(), filters)
        }
        listOf("""[{"field":"privatePhone"}]""", """[{"field":"task.title"}]""", """[{"field":"id","desc":"yes"}]""").forEach { sort ->
            val response = rpc(call("projectforge_search", """{"entity":"timesheets","sort":$sort}"""))
            assertEquals(-32602, mapper.valueToTree<JsonNode>(response.body).path("error").path("code").asInt(), sort)
        }
        verify(datasets, never()).search(any(), any())
    }

    @Test
    fun `results leave out hidden and empty fields, reduce attachments to their metadata and keep only the requested fields`() {
        val address = mapOf("id" to 7, "name" to "Miller", "organization" to "ACME", "privatePhone" to "0815", "birthday" to "1970-01-01",
            "imageData" to "AAAA", "comment" to "", "website" to null, "addressbookList" to listOf(mapOf("id" to 1, "writeAccess" to true)),
            "attachmentsCounter" to 1, "attachmentsIds" to "f1",
            "attachments" to listOf(mapOf("fileId" to "f1", "name" to "contract.pdf", "size" to 1234, "password" to "secret",
                "location" to "/repo/f1", "checksum" to "abc", "sizeHumanReadable" to "1 KB")))
        whenever(datasets.get("addresses", 7L)).thenReturn(address)
        whenever(datasets.search(eq("addresses"), any())).thenReturn(listOf(address))
        val get = content(call("projectforge_get", """{"entity":"addresses","id":7}"""))
        assertEquals(mapper.readTree("""{"id":7,"name":"Miller","organization":"ACME","addressbookList":[{"id":1}],"attachmentsCounter":1,
            "attachments":[{"fileId":"f1","name":"contract.pdf","size":1234}]}"""), get)
        val search = content(call("projectforge_search", """{"entity":"addresses","fields":["name","privatePhone"]}"""))
        assertEquals(mapper.readTree("""[{"id":7,"name":"Miller"}]"""), search.path("items"))
        assertTrue(McpDatasets.hidden("addresses", "privateEmail"))
        assertFalse(McpDatasets.hidden("timesheets", "privateEmail"))
        assertTrue(McpDatasets.hidden("timesheets", "attachmentsIds"))
    }

    @Test
    fun `attachments come as text, image or embedded resource, never encrypted or too large`() {
        fun file(name: String, mimeType: String, content: ByteArray?, encrypted: Boolean = false) =
            McpDatasets.AttachmentFile(name, mimeType, content?.size?.toLong() ?: 10_000_000, encrypted, content)
        whenever(datasets.hasAttachments("addresses")).thenReturn(true)
        whenever(datasets.attachment(eq("addresses"), eq(7L), any(), any())).thenAnswer { invocation ->
            when (invocation.getArgument<String>(2)) {
                "txt" -> file("notes.txt", "text/plain", "Call back".toByteArray())
                "png" -> file("logo.png", "image/png", byteArrayOf(1, 2, 3))
                "pdf" -> file("contract.pdf", "application/pdf", byteArrayOf(4, 5))
                "zip" -> file("secret.zip", "application/zip", null, encrypted = true)
                "big" -> file("video.mp4", "video/mp4", null)
                else -> null
            }
        }
        fun attachment(fileId: String) = result(call("projectforge_attachment", """{"entity":"addresses","id":7,"fileId":"$fileId"}"""))

        val text = attachment("txt")
        assertEquals("notes.txt", mapper.readTree(text.path("content").path(0).path("text").asText()).path("name").asText())
        assertEquals("Call back", text.path("content").path(1).path("text").asText())
        assertEquals("AQID", attachment("png").path("content").path(1).path("data").asText())
        val pdf = attachment("pdf").path("content").path(1)
        assertEquals("resource", pdf.path("type").asText())
        assertEquals("projectforge://addresses/7/attachments/pdf", pdf.path("resource").path("uri").asText())
        assertEquals("BAU=", pdf.path("resource").path("blob").asText())
        listOf("zip", "big", "unknown").forEach { assertTrue(attachment(it).path("isError").asBoolean(), it) }
        assertTrue(mapper.valueToTree<JsonNode>(rpc(call("projectforge_attachment",
            """{"entity":"addresses","id":7,"fileId":"../x"}""")).body).path("error").path("code").asInt() == -32602)
        // Records without attachments (tasks) don't ask the database.
        assertEquals(-32602, mapper.valueToTree<JsonNode>(rpc(call("projectforge_attachment",
            """{"entity":"tasks","id":42,"fileId":"txt"}""")).body).path("error").path("code").asInt())
        verify(datasets, never()).attachment(eq("tasks"), any(), any(), any())
    }

    @Test
    fun `describe tells the filter fields, their values and the fields to sort by`() {
        val describe = content(call("projectforge_describe", """{"entity":"timesheets"}"""))
        val filters = describe.path("filters").associateBy { it.path("field").asText() }
        assertEquals(timesheetFilters.map { it.id }.toSet(), filters.keys)
        assertTrue(filters.getValue("user").path("me").asBoolean())
        assertFalse(filters.getValue("task").has("me"))
        assertEquals("OPEN", filters.getValue("status").path("values").path(0).path("value").asText())
        assertEquals("ids of the records", filters.getValue("projects").path("values").asText())
        assertEquals(listOf("id", "startTime", "description"), describe.path("sortable").map { it.asText() })
        assertEquals("startTime", describe.path("defaultSort").path("field").asText())
        val overview = content(call("projectforge_describe"))
        assertEquals("full-access", overview.path("user").path("username").asText())
        assertEquals(listOf("tasks", "timesheets", "addresses"), overview.path("entities").map { it.path("entity").asText() })
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
