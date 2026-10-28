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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.projectforge.business.address.AddressDao
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.task.TaskDao
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.login.LoginService
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.security.My2FARequestHandler
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64

class McpSessionOAuthTest {
    @Test
    fun `scopes follow current permissions and actual MFA freshness without minting another login`() {
        val project = mock<ProjektDao>()
        val invoice = mock<RechnungDao>()
        val mfa = mock<My2FARequestHandler>()
        val login = mock<LoginService>()
        whenever(mfa.getRemainingPeriod(any<String>())).thenReturn(null)
        whenever(project.hasLoggedInUserSelectAccess(false)).thenReturn(true)
        whenever(invoice.hasLoggedInUserSelectAccess(false)).thenReturn(false)
        val context = UserContext(PFUserDO().apply { id = 1L; username = "normal-user" }, true)
        val oauth = McpSessionOAuth(login, SessionCsrfService(), mfa, project, mock<KundeDao>(), mock<TaskDao>(),
            mock<TimesheetDao>(), mock<AddressDao>(), invoice, "https://projectforge.example.org/mcp")
        assertEquals(setOf("projectforge.profile.read", "projectforge.projects.read"), oauth.available(context))
        assertNull(ThreadLocalUserContext.loggedInUser)
        whenever(project.hasLoggedInUserSelectAccess(false)).thenReturn(false)
        assertEquals(setOf("projectforge.profile.read"), oauth.available(context))
        whenever(project.hasLoggedInUserSelectAccess(false)).thenReturn(true)
        whenever(mfa.getRemainingPeriod("/rs/project/list")).thenReturn(0L)
        assertEquals(setOf("projectforge.profile.read"), oauth.available(context))
        whenever(mfa.getRemainingPeriod("/rs/project/list")).thenReturn(60_000L)
        assertTrue("projectforge.projects.read" in oauth.available(context))
        whenever(mfa.getRemainingPeriod("/rs/project/42")).thenReturn(0L)
        assertFalse(oauth.readMfaValid("projects", 42))
        assertNull(context.lastSuccessful2FA, "The adapter must never fabricate a second factor")
        assertNull(ThreadLocalUserContext.loggedInUser)
        val external = oauth.register(MockHttpServletRequest(), mapOf("redirect_uris" to listOf("https://evil.example/callback")))
        assertEquals(400, external.statusCode.value())
        val registered = oauth.register(MockHttpServletRequest(), mapOf("client_name" to "Untrusted <client>", "redirect_uris" to listOf("http://localhost:19876/callback")))
        val client = registered.body as McpSessionOAuth.Client
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val params = mapOf("client_id" to client.client_id, "redirect_uri" to client.redirect_uris[0],
            "response_type" to "code", "code_challenge_method" to "S256", "code_challenge" to "a".repeat(43))
        // No valid session (including a pending login second factor) must go through the normal login.
        assertEquals(302, oauth.authorize(request, response, params).statusCode.value())
        request.getSession(true)
        whenever(login.checkLogin(request, response)).thenReturn(context)
        val consent = oauth.authorize(request, response, params)
        assertEquals(200, consent.statusCode.value())
        val html = consent.body as String
        assertFalse(html.contains("type=\"password\""))
        assertFalse(html.contains("Untrusted <client>"))
        assertTrue(html.contains("Untrusted &lt;client&gt;"))
        assertTrue(html.contains("normal-user"))
        assertTrue(html.contains("name=\"scope\" value=\"projectforge.projects.read\""))
        assertFalse(html.contains("name=\"scope\" value=\"projectforge.invoices.read\""))
    }

    private val verifier = "v".repeat(43)
    private val challenge = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(UTF_8)))
    private val context = UserContext(PFUserDO().apply { id = 1L; username = "normal-user" }, true)
    private val login = mock<LoginService>().also { whenever(it.checkLogin(any(), any())).thenReturn(context) }

    private fun oauth(registryFile: String = "") = McpSessionOAuth(login, SessionCsrfService(), mock<My2FARequestHandler>(),
        mock<ProjektDao>(), mock<KundeDao>(), mock<TaskDao>(), mock<TimesheetDao>(), mock<AddressDao>(), mock<RechnungDao>(),
        "https://projectforge.example.org/mcp", registryFile)

    private fun register(oauth: McpSessionOAuth, address: String = "127.0.0.1", redirect: String = "http://localhost:19876/callback") =
        oauth.register(MockHttpServletRequest().apply { remoteAddr = address }, mapOf("redirect_uris" to listOf(redirect)))

    private fun client(oauth: McpSessionOAuth) = register(oauth).body as McpSessionOAuth.Client

    private fun authorize(oauth: McpSessionOAuth, client: McpSessionOAuth.Client, session: MockHttpSession,
        redirect: String = client.redirect_uris[0]) = oauth.authorize(
        MockHttpServletRequest().apply { setSession(session) }, MockHttpServletResponse(), mapOf("client_id" to client.client_id,
            "redirect_uri" to redirect, "response_type" to "code", "code_challenge_method" to "S256",
            "code_challenge" to challenge, "scope" to "projectforge.profile.read"))

    /** Runs the browser part (authorize and allow) in a session and returns the authorization code. */
    private fun code(oauth: McpSessionOAuth, client: McpSessionOAuth.Client, session: MockHttpSession = MockHttpSession(),
        redirect: String = client.redirect_uris[0]): String {
        val html = authorize(oauth, client, session, redirect).body as String
        val location = oauth.consent(consentRequest(session, html), MockHttpServletResponse()).headers.location.toString()
        assertTrue(location.startsWith(redirect + if ('?' in redirect) "&code=" else "?code="))
        return URLDecoder.decode(Regex("[?&]code=([^&]+)").find(location)!!.groupValues[1], UTF_8)
    }

    /** The browser's post of the consent form in [html], allowing the profile scope. */
    private fun consentRequest(session: MockHttpSession, html: String, fetchSite: String? = null) =
        MockHttpServletRequest("POST", "/mcp/oauth/consent").apply {
            fun hidden(name: String) = Regex("name=\"$name\" value=\"([^\"]+)\"").find(html)!!.groupValues[1]
            setSession(session)
            fetchSite?.let { addHeader("Sec-Fetch-Site", it) }
            addParameter("pending", hidden("pending"))
            addParameter("csrf", hidden("csrf"))
            addParameter("decision", "allow")
            addParameter("scope", "projectforge.profile.read")
        }

    @Test
    fun `the consent is refused for a post of a foreign page and allowed for the own consent page`() {
        val oauth = oauth()
        val client = client(oauth)
        val session = MockHttpSession()
        val html = authorize(oauth, client, session).body as String
        // Valid csrf token and pending consent, but posted by another site: refused, the consent stays pending.
        assertEquals(403, oauth.consent(consentRequest(session, html, "cross-site"), MockHttpServletResponse()).statusCode.value())
        val allowed = oauth.consent(consentRequest(session, html, "same-origin"), MockHttpServletResponse())
        assertEquals(302, allowed.statusCode.value())
        assertTrue(allowed.headers.location.toString().contains("code="))
    }

    private fun token(oauth: McpSessionOAuth, client: McpSessionOAuth.Client, vararg params: Pair<String, String>) =
        oauth.token(MockHttpServletRequest(), MockHttpServletResponse(), mapOf("client_id" to client.client_id, *params))

    private fun exchange(oauth: McpSessionOAuth, client: McpSessionOAuth.Client, code: String,
        redirect: String = client.redirect_uris[0]) = token(oauth, client,
        "grant_type" to "authorization_code", "code" to code, "code_verifier" to verifier, "redirect_uri" to redirect)

    private fun refresh(oauth: McpSessionOAuth, client: McpSessionOAuth.Client, refreshToken: Any?) =
        token(oauth, client, "grant_type" to "refresh_token", "refresh_token" to refreshToken.toString())

    @Suppress("UNCHECKED_CAST")
    private val org.springframework.http.ResponseEntity<Any>.tokens get() = body as Map<String, Any>

    private fun valid(oauth: McpSessionOAuth, accessToken: Any?) =
        oauth.authenticate(accessToken.toString(), MockHttpServletRequest(), MockHttpServletResponse()) != null

    @Test
    fun `loopback IP registrations support new ports but code exchange stays bound to the chosen redirect`() {
        listOf("127.0.0.1", "[::1]").forEach { host ->
            listOf("", ":19876").forEach { registeredPort ->
                val oauth = oauth()
                val client = register(oauth, redirect = "http://$host$registeredPort/callback?app=desktop").body as McpSessionOAuth.Client
                // Reuse one registration on two independent authorizations with different listener ports.
                listOf(19877, 19878).forEach { port ->
                    val chosen = "http://$host:$port/callback?app=desktop"
                    val code = code(oauth, client, redirect = chosen)
                    assertEquals(400, exchange(oauth, client, code).statusCode.value(), "Registered URI is not the chosen callback")
                    assertEquals(400, exchange(oauth, client, code, "http://$host:19879/callback?app=desktop").statusCode.value())
                    val issued = exchange(oauth, client, code, chosen)
                    assertEquals(200, issued.statusCode.value())
                    assertTrue(valid(oauth, issued.tokens["access_token"]))
                }
            }
        }
    }

    @Test
    fun `the loopback port exception does not permit other URI changes`() {
        val oauth = oauth()
        val registered = "http://127.0.0.1:19876/callback?app=%64esktop"
        val client = register(oauth, redirect = registered).body as McpSessionOAuth.Client
        val invalid = listOf(
            "http://localhost:19877/callback?app=%64esktop",
            "http://[::1]:19877/callback?app=%64esktop",
            "http://127.0.0.2:19877/callback?app=%64esktop",
            "http://127.0.0.1.evil.example:19877/callback?app=%64esktop",
            "https://127.0.0.1:19877/callback?app=%64esktop",
            "http://127.0.0.1:19877/other?app=%64esktop",
            "http://127.0.0.1:19877/%63allback?app=%64esktop",
            "http://127.0.0.1:19877/callback?app=desktop",
            "http://127.0.0.1:19877/callback",
            "http://127.0.0.1:19877/callback?app=%64esktop#fragment",
            "http://user@127.0.0.1:19877/callback?app=%64esktop",
            "http://127.0.0.1:0/callback?app=%64esktop",
            "http://127.0.0.1:65536/callback?app=%64esktop",
            "not a URI",
        )
        invalid.forEach { redirect ->
            assertEquals(400, authorize(oauth, client, MockHttpSession(), redirect).statusCode.value(), redirect)
        }
        assertEquals(200, authorize(oauth, client, MockHttpSession(), registered).statusCode.value())
        val localhost = client(oauth)
        assertEquals(200, authorize(oauth, localhost, MockHttpSession()).statusCode.value())
        assertEquals(400, authorize(oauth, localhost, MockHttpSession(), "http://localhost:19877/callback").statusCode.value())
    }

    @Test
    fun `rotation keeps one access token per grant and a replayed code or refresh token revokes the grant`() {
        val oauth = oauth()
        val client = client(oauth)
        val first = exchange(oauth, client, code(oauth, client)).tokens
        assertTrue(valid(oauth, first["access_token"]))
        val second = refresh(oauth, client, first["refresh_token"])
        assertEquals(200, second.statusCode.value())
        assertFalse(valid(oauth, first["access_token"]), "Rotation ends the previous access token")
        assertTrue(valid(oauth, second.tokens["access_token"]))
        // The outdated refresh token is replayed: the whole grant ends, the current tokens too.
        assertEquals(400, refresh(oauth, client, first["refresh_token"]).statusCode.value())
        assertFalse(valid(oauth, second.tokens["access_token"]))
        assertEquals(400, refresh(oauth, client, second.tokens["refresh_token"]).statusCode.value())

        val code = code(oauth, client)
        val issued = exchange(oauth, client, code).tokens
        assertTrue(valid(oauth, issued["access_token"]))
        assertEquals(400, exchange(oauth, client, code).statusCode.value())
        assertFalse(valid(oauth, issued["access_token"]), "A replayed code revokes the tokens issued for it")
        assertEquals(400, refresh(oauth, client, issued["refresh_token"]).statusCode.value())

        val revoked = exchange(oauth, client, code(oauth, client)).tokens
        oauth.revoke(mapOf("token" to revoked["refresh_token"].toString(), "client_id" to client.client_id))
        assertFalse(valid(oauth, revoked["access_token"]))
    }

    @Test
    fun `a new grant replaces the oldest one of the same user instead of filling the global limit`() {
        val oauth = oauth()
        val client = client(oauth)
        val tokens = (0..McpSessionOAuth.MAX_GRANTS_PER_USER).map { exchange(oauth, client, code(oauth, client)).tokens }
        assertFalse(valid(oauth, tokens.first()["access_token"]))
        assertEquals(400, refresh(oauth, client, tokens.first()["refresh_token"]).statusCode.value())
        tokens.drop(1).forEach { assertTrue(valid(oauth, it["access_token"])) }
    }

    @Test
    fun `registration is limited per address and only allowed clients are kept and persisted`() {
        val registry = Files.createTempDirectory("mcp-test").resolve("clients.properties")
        val oauth = oauth(registry.toString())
        repeat(McpSessionOAuth.MAX_REGISTRATIONS_PER_ADDRESS) { assertEquals(201, register(oauth, "10.0.0.1").statusCode.value()) }
        assertEquals(429, register(oauth, "10.0.0.1").statusCode.value())
        assertEquals(201, register(oauth, "10.0.0.2").statusCode.value())
        assertFalse(Files.exists(registry), "Unconfirmed registrations are not persisted")

        val allowed = client(oauth)
        code(oauth, allowed)
        assertTrue(Files.readString(registry).contains(allowed.client_id))
        // Flooding the unconfirmed pool drops the oldest unconfirmed registrations, never an allowed client.
        val dropped = register(oauth, "10.0.1.0").body as McpSessionOAuth.Client
        (0 until McpSessionOAuth.MAX_UNCONFIRMED_CLIENTS).forEach { register(oauth, "10.1.${it / 10}.1") }
        assertEquals(400, authorize(oauth, dropped, MockHttpSession()).statusCode.value())
        assertEquals(200, authorize(oauth, allowed, MockHttpSession()).statusCode.value())
        assertEquals(200, authorize(oauth(registry.toString()), allowed, MockHttpSession()).statusCode.value(), "Allowed clients survive a restart")
    }
}
