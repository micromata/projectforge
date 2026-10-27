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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.projectforge.business.address.AddressDao
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.task.TaskDao
import org.projectforge.Constants
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.html.Html
import org.projectforge.common.html.HtmlDocument
import org.projectforge.common.html.HtmlElement
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.RestCsrfProtection
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.security.My2FARequestHandler
import org.projectforge.security.RegisterUser4Thread
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Base64
import java.util.Properties
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Prototype of a session-backed authorization server. Never accepts passwords or usernames.
 * Audit log entries name users by id and clients by their (public) client id, never tokens, codes or client names.
 */
@RestController
@ConditionalOnProperty(name = ["projectforge.mcp.enabled"], havingValue = "true")
@AccessChecked("OAuth endpoints validate clients/PKCE; browser consent requires the existing PF session and CSRF; tokens remain bound to that session")
class McpSessionOAuth(
    private val login: LoginService,
    private val csrf: SessionCsrfService,
    private val twoFactor: My2FARequestHandler,
    projectDao: ProjektDao,
    customerDao: KundeDao,
    taskDao: TaskDao,
    timesheetDao: TimesheetDao,
    addressDao: AddressDao,
    invoiceDao: RechnungDao,
    @param:Value("\${projectforge.mcp.resource}") val resource: String,
    @Value("\${projectforge.mcp.client-registry-file:}") registryFile: String = "",
) {
    val origin = URI(resource).let { "${it.scheme}://${it.rawAuthority}" }
    val issuer = "$origin/mcp/oauth"
    private fun digest(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(UTF_8)))
    private fun now() = System.currentTimeMillis()
    private fun encode(value: String) = URLEncoder.encode(value, UTF_8)
    val scopeLabels = linkedMapOf(
        "projectforge.profile.read" to "Angemeldeten Benutzer anzeigen",
        "projectforge.projects.read" to "Projekte lesen",
        "projectforge.customers.read" to "Kunden lesen",
        "projectforge.tasks.read" to "Aufgaben lesen",
        "projectforge.timesheets.read" to "Zeitbuchungen lesen",
        "projectforge.addresses.read" to "Adressen lesen",
        "projectforge.invoices.read" to "Rechnungen lesen",
    )
    private val daos = mapOf("projects" to projectDao, "customers" to customerDao, "tasks" to taskDao,
        "timesheets" to timesheetDao, "addresses" to addressDao, "invoices" to invoiceDao)
    private val paths = mapOf("projects" to "project", "customers" to "customer", "tasks" to "task",
        "timesheets" to "timesheet", "addresses" to "address", "invoices" to "outgoingInvoice")
    data class Client(val client_id: String, val client_name: String, val redirect_uris: List<String>,
        val token_endpoint_auth_method: String = "none", val grant_types: List<String> = listOf("authorization_code", "refresh_token"),
        val response_types: List<String> = listOf("code"))
    data class Pending(val client: Client, val redirect: String, val challenge: String, val state: String?,
        val scopes: Set<String>, val userId: Long, val expires: Long)
    /** Compared by identity. Holds only the hashes of its current access and refresh token (one of each). */
    class Grant(val clientId: String, val session: HttpSession, val userId: Long, val scopes: Set<String>, val expires: Long) {
        val id = randomId()
        @Volatile var accessHash: String? = null
        @Volatile var refreshHash: String? = null
        @Volatile var revoked = false
    }
    /** Used codes are kept until they expire, so a replayed code can revoke the grant issued for it. */
    class Code(val grant: Grant, val challenge: String, val redirect: String, val expires: Long) { @Volatile var used = false }
    data class Access(val grant: Grant, val expires: Long)
    data class Identity(val context: UserContext, val scopes: Set<String>)
    private val clients = ConcurrentHashMap<String, Client>()
    // Registrations not yet allowed by a logged-in user, oldest first. Only allowed clients are persisted.
    private val unconfirmed = LinkedHashSet<String>()
    // Registration times per remote address within the rate limit window.
    private val registrations = HashMap<String, ArrayDeque<Long>>()
    private val codes = ConcurrentHashMap<String, Code>()
    private val accesses = ConcurrentHashMap<String, Access>()
    // Active grants by id. A refresh token is "<grant id>.<secret>", so an outdated one still finds its grant.
    private val grants = ConcurrentHashMap<String, Grant>()
    private val registry = registryFile.takeIf { it.isNotBlank() }?.let { Path.of(it) }

    init {
        registry?.takeIf { Files.exists(it) }?.let { path ->
            val properties = Properties().apply { Files.newInputStream(path).use { load(it) } }
            properties.stringPropertyNames().filter { it.endsWith(".name") }.forEach { key ->
                val id = key.removeSuffix(".name")
                val redirects = properties.getProperty("$id.redirects", "").split("\n")
                require(redirects.isNotEmpty() && redirects.all(::validRedirect)) { "Invalid saved MCP callback" }
                clients[id] = Client(id, properties.getProperty(key), redirects)
            }
        }
    }

    private fun saveClients() {
        val path = registry ?: return
        val properties = Properties()
        clients.values.filter { it.client_id !in unconfirmed }.forEach { client ->
            properties.setProperty("${client.client_id}.name", client.client_name)
            properties.setProperty("${client.client_id}.redirects", client.redirect_uris.joinToString("\n"))
        }
        Files.createDirectories(path.toAbsolutePath().parent)
        val temporary = Files.createTempFile(path.toAbsolutePath().parent, "mcp-clients-", ".tmp")
        try {
            Files.newOutputStream(temporary).use { properties.store(it, "Public native MCP client registrations; no credentials") }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temporary) }
    }

    @GetMapping("/.well-known/oauth-authorization-server/mcp/oauth", "/mcp/oauth/.well-known/openid-configuration")
    fun metadata() = mapOf(
        "issuer" to issuer, "authorization_endpoint" to "$issuer/authorize", "token_endpoint" to "$issuer/token",
        "registration_endpoint" to "$issuer/register", "revocation_endpoint" to "$issuer/revoke",
        "response_types_supported" to listOf("code"), "grant_types_supported" to listOf("authorization_code", "refresh_token"),
        "token_endpoint_auth_methods_supported" to listOf("none"), "code_challenge_methods_supported" to listOf("S256"),
        "scopes_supported" to scopeLabels.keys.toList(),
    )

    @PostMapping("/mcp/oauth/register")
    @Synchronized
    fun register(request: HttpServletRequest, @RequestBody body: Map<String, Any?>): ResponseEntity<Any> {
        val redirects = (body["redirect_uris"] as? List<*>)?.filterIsInstance<String>() ?: return bad("invalid_redirect_uri")
        // This prototype deliberately supports native loopback clients only, not arbitrary web callbacks.
        if (redirects.isEmpty() || redirects.size > 5 || !redirects.all(::validRedirect)) return bad("invalid_redirect_uri")
        // Registration is unauthenticated: limit it per client address and keep unconfirmed registrations in a bounded
        // pool that drops the oldest one.
        val address = WebUtils.getClientIp(request) ?: ""
        val time = now()
        registrations.values.forEach { times -> while (times.firstOrNull()?.let { it < time - REGISTRATION_WINDOW } == true) times.removeFirst() }
        registrations.values.removeIf { it.isEmpty() }
        val times = registrations.getOrPut(address) { ArrayDeque() }
        if (times.size >= MAX_REGISTRATIONS_PER_ADDRESS) {
            log.warn { "MCP: client registration rate limit reached for $address." }
            return ResponseEntity.status(429).header("Cache-Control", "no-store").body(mapOf("error" to "slow_down"))
        }
        times.addLast(time)
        if (unconfirmed.size >= MAX_UNCONFIRMED_CLIENTS) unconfirmed.first().let { unconfirmed.remove(it); clients.remove(it) }
        val client = Client(randomId(), (body["client_name"] as? String ?: "MCP-Client").take(100), redirects)
        clients[client.client_id] = client
        unconfirmed += client.client_id
        log.info { "MCP: client ${client.client_id} registered from $address." }
        return ResponseEntity.status(201).body(client)
    }

    /** Keeps a client after a logged-in user allowed it (again, if it was dropped from the unconfirmed pool meanwhile). */
    @Synchronized
    private fun confirm(client: Client): Boolean {
        if (clients.containsKey(client.client_id) && client.client_id !in unconfirmed) return true
        if (clients.size - unconfirmed.size >= MAX_CONFIRMED_CLIENTS) return false
        unconfirmed.remove(client.client_id)
        clients[client.client_id] = client
        saveClients()
        return true
    }

    private fun validRedirect(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1", "[::1]") && uri.rawUserInfo == null && uri.fragment == null &&
            (uri.port == -1 || uri.port in 1..65535)
    }.getOrDefault(false)

    /** RFC 8252 section 7.3: native loopback-IP clients may choose a new listener port on each login. */
    private fun matchesRedirect(registered: String, requested: String): Boolean {
        if (registered == requested) return true
        // Both URIs have already passed validRedirect. Only IP literals get the port exception;
        // localhost still requires an exact match. Never normalize paths or decode query strings.
        val expected = URI(registered)
        val actual = URI(requested)
        return expected.host in setOf("127.0.0.1", "[::1]") && expected.scheme == actual.scheme &&
            expected.host == actual.host && expected.rawPath == actual.rawPath && expected.rawQuery == actual.rawQuery
    }

    @GetMapping("/mcp/oauth/authorize")
    fun authorize(request: HttpServletRequest, response: HttpServletResponse, @RequestParam params: Map<String, String>): ResponseEntity<Any> {
        val client = clients[params["client_id"]] ?: return bad("invalid_client")
        val redirect = params["redirect_uri"] ?: return bad("invalid_redirect_uri")
        if (!validRedirect(redirect) || client.redirect_uris.none { matchesRedirect(it, redirect) }) return bad("invalid_redirect_uri")
        val challenge = params["code_challenge"] ?: return bad("invalid_request")
        if (params["response_type"] != "code" || params["code_challenge_method"] != "S256" || !challenge.matches(Regex("[A-Za-z0-9_-]{43}"))) return bad("invalid_request")
        if (params["resource"] != null && params["resource"] != resource) return bad("invalid_target")
        val requested = params["scope"]?.split(" ")?.filter { it.isNotBlank() }?.toSet() ?: scopeLabels.keys
        if (!scopeLabels.keys.containsAll(requested)) return bad("invalid_scope")
        val context = login.checkLogin(request, response)
        if (context == null) {
            val returnUrl = "/mcp/oauth/authorize?" + params.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
            return ResponseEntity.status(302).header("Location", "${Constants.NEXT_LOGIN_URL}?${Constants.NEXT_LOGIN_RETURN_URL_PARAM}=${encode(returnUrl)}").build()
        }
        val effective = available(context).intersect(requested)
        val pendingId = randomId()
        val session = request.getSession(false)
        // One pending consent per browser session. Replacing it invalidates an older form.
        session.setAttribute("mcp.consent", pendingId to Pending(client, redirect, challenge, params["state"], effective, context.user!!.id!!, now() + 300_000))
        val csrfToken = csrf.createServerData(request).csrfToken!!
        val user = context.user!!
        val form = HtmlElement("form").apply {
            attr("method", "post"); attr("action", "/mcp/oauth/consent")
            add(hidden("pending", pendingId)); add(hidden("csrf", csrfToken))
            effective.forEach { scope ->
                add(HtmlElement("label").apply {
                    add(HtmlElement("input", childrenAllowed = false).apply {
                        attr("type", "checkbox"); attr("name", "scope"); attr("value", scope); attr("checked", "checked")
                    })
                    add(" ${scopeLabels.getValue(scope)}")
                    add(HtmlElement("small", content = scope))
                })
            }
            val unavailable = requested - effective
            if (unavailable.isNotEmpty()) {
                add(Html.P("Aufgrund deiner aktuellen Rechte oder 2FA-Freigabe nicht verfügbar: ${unavailable.joinToString(", ") { scopeLabels.getValue(it) }}."))
            }
            add(Html.P("Es gelten weiterhin deine ProjectForge-Rechte für jeden einzelnen Datensatz. Schreibzugriffe sind ausgeschlossen."))
            add(button("allow", "Lesezugriff erlauben")); add(button("deny", "Abbrechen"))
        }
        val document = HtmlDocument("ProjectForge – Zugriff freigeben", lang = "de").apply {
            head.add(HtmlElement("style", content = CONSENT_STYLE))
            add(HtmlElement("main").apply {
                add(Html.P("ProjectForge · Lesezugriff"))
                add(Html.H1("${client.client_name} verbinden"))
                add(Html.P().apply {
                    addClassname("identity")
                    add("Angemeldet als ")
                    // An empty element would be rendered as "<strong />", which browsers read as an open tag.
                    user.userDisplayName?.takeIf { it.isNotBlank() }?.let { add(HtmlElement("strong", content = it)) }
                    add(" (${user.username ?: ""})")
                })
                add(Html.P("Deine bestehende Anmeldung wird verwendet. Wähle, welche Daten diese Anwendung in deinem Namen lesen darf."))
                add(form)
            })
        }
        val content = document.toString()
        return ResponseEntity.ok().header("Content-Type", "text/html; charset=utf-8").header("Cache-Control", "no-store")
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self' ${URI(redirect).let { "${it.scheme}://${it.rawAuthority}" }}; frame-ancestors 'none'")
            .body(content)
    }

    @PostMapping("/mcp/oauth/consent", consumes = ["application/x-www-form-urlencoded"])
    fun consent(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Any> {
        val context = login.checkLogin(request, response) ?: return ResponseEntity.status(401).body(mapOf("error" to "login_required"))
        if (!RestCsrfProtection.isSameSiteRequest(request) || !csrf.checkToken(request, request.getParameter("csrf"))) return ResponseEntity.status(403).body(mapOf("error" to "invalid_csrf"))
        val session = request.getSession(false)
        @Suppress("UNCHECKED_CAST")
        val entry = session.getAttribute("mcp.consent") as? Pair<String, Pending> ?: return bad("invalid_request")
        val pending = entry.second
        if (entry.first != request.getParameter("pending") || pending.expires < now() || pending.userId != context.user?.id) return bad("invalid_request")
        session.removeAttribute("mcp.consent")
        if (request.getParameter("decision") != "allow") {
            log.info { "MCP: user #${pending.userId} denied access for client ${pending.client.client_id}." }
            return redirect(pending.redirect, pending.state, "error", "access_denied")
        }
        val selected = request.getParameterValues("scope")?.toSet() ?: emptySet()
        if (selected.isEmpty() || !pending.scopes.containsAll(selected) || !available(context).containsAll(selected)) return bad("invalid_scope")
        if (!confirm(pending.client)) return bad("temporarily_unavailable")
        prune()
        if (codes.size >= 1000) return bad("temporarily_unavailable")
        val code = randomId()
        val grant = Grant(pending.client.client_id, session, pending.userId, selected, now() + 3_600_000)
        codes[digest(code)] = Code(grant, pending.challenge, pending.redirect, now() + 60_000)
        log.info { "MCP: user #${pending.userId} allowed client ${pending.client.client_id}: ${selected.sorted().joinToString(" ")}." }
        return redirect(pending.redirect, pending.state, "code", code)
    }

    private fun redirect(uri: String, state: String?, key: String, value: String): ResponseEntity<Any> {
        val query = "$key=${encode(value)}&iss=${encode(issuer)}" + (state?.let { "&state=${encode(it)}" } ?: "")
        return ResponseEntity.status(302).header("Location", uri + (if (URI(uri).rawQuery == null) "?" else "&") + query).header("Cache-Control", "no-store").build()
    }

    @PostMapping("/mcp/oauth/token", consumes = ["application/x-www-form-urlencoded"])
    @Synchronized
    fun token(request: HttpServletRequest, response: HttpServletResponse, @RequestParam params: Map<String, String>): ResponseEntity<Any> {
        prune()
        if (params["resource"] != null && params["resource"] != resource) return bad("invalid_target")
        val client = params["client_id"] ?: return bad("invalid_client")
        val grant: Grant
        when (params["grant_type"]) {
            "authorization_code" -> {
                val hash = digest(params["code"] ?: "")
                val code = codes[hash] ?: return bad("invalid_grant")
                // A code used twice was intercepted: revoke everything issued for it (RFC 9700, section 4.5).
                if (code.used) {
                    log.warn { "MCP: authorization code of user #${code.grant.userId} for client ${code.grant.clientId} was used again; grant revoked." }
                    revoke(code.grant); return bad("invalid_grant")
                }
                val verifier = params["code_verifier"] ?: return bad("invalid_grant")
                if (code.grant.clientId != client || code.redirect != params["redirect_uri"] || !verifier.matches(Regex("[A-Za-z0-9._~-]{43,128}")) || digest(verifier) != code.challenge) return bad("invalid_grant")
                code.used = true
                grant = code.grant
            }
            "refresh_token" -> {
                val token = params["refresh_token"] ?: ""
                grant = grants[token.substringBefore('.')] ?: return bad("invalid_grant")
                if (grant.clientId != client) return bad("invalid_grant")
                // An outdated refresh token of a rotated grant was copied: revoke the grant (RFC 9700, section 4.14.2).
                if (grant.refreshHash != digest(token)) {
                    log.warn { "MCP: outdated refresh token of user #${grant.userId} for client ${grant.clientId} was used; grant revoked." }
                    revoke(grant); return bad("invalid_grant")
                }
            }
            else -> return bad("unsupported_grant_type")
        }
        if (identity(grant, request, response) == null) {
            log.info { "MCP: refused tokens for user #${grant.userId} and client ${grant.clientId}: session ended, user inactive or grant expired." }
            return bad("invalid_grant")
        }
        if (!grants.containsKey(grant.id)) {
            // One user holds at most a few grants; a new one replaces the oldest instead of filling the global limit.
            grants.values.filter { it.userId == grant.userId }.sortedBy { it.expires }
                .dropLast(MAX_GRANTS_PER_USER - 1).forEach {
                    log.info { "MCP: oldest grant of user #${it.userId} for client ${it.clientId} replaced by a new one." }
                    revoke(it)
                }
            if (grants.size >= MAX_GRANTS) {
                log.warn { "MCP: global grant limit reached, refused tokens for user #${grant.userId}." }
                return bad("temporarily_unavailable")
            }
            log.info { "MCP: issued tokens for user #${grant.userId} and client ${grant.clientId}: ${grant.scopes.sorted().joinToString(" ")}." }
        }
        // Rotation: the grant's previous access token ends with the new one, so a grant holds one of each.
        grant.accessHash?.let(accesses::remove)
        val access = randomId(); val refresh = "${grant.id}.${randomId()}"
        grant.accessHash = digest(access); grant.refreshHash = digest(refresh)
        accesses[digest(access)] = Access(grant, now() + 300_000)
        grants[grant.id] = grant
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(mapOf("access_token" to access,
            "refresh_token" to refresh, "token_type" to "Bearer", "expires_in" to 300, "scope" to grant.scopes.joinToString(" ")))
    }

    @PostMapping("/mcp/oauth/revoke", consumes = ["application/x-www-form-urlencoded"])
    @Synchronized
    fun revoke(@RequestParam params: Map<String, String>): Map<String, Any> {
        val token = params["token"] ?: ""
        val grant = accesses[digest(token)]?.grant
            ?: grants[token.substringBefore('.')]?.takeIf { it.refreshHash == digest(token) }
        if (grant != null && grant.clientId == params["client_id"]) {
            log.info { "MCP: client ${grant.clientId} revoked its grant of user #${grant.userId}." }
            revoke(grant)
        }
        return emptyMap()
    }

    @Synchronized
    private fun revoke(grant: Grant) {
        grant.revoked = true
        grants.remove(grant.id, grant)
        grant.accessHash?.let(accesses::remove)
    }

    fun authenticate(token: String, request: HttpServletRequest, response: HttpServletResponse): Identity? {
        val access = accesses[digest(token)] ?: return null
        if (access.expires < now()) { accesses.remove(digest(token)); return null }
        return identity(access.grant, request, response)
    }

    private fun identity(grant: Grant, request: HttpServletRequest, response: HttpServletResponse): Identity? {
        if (grant.revoked || grant.expires < now()) return null
        val sessionRequest = object : HttpServletRequestWrapper(request) {
            override fun getSession(create: Boolean): HttpSession = grant.session
            override fun getSession(): HttpSession = grant.session
        }
        val context = try { login.checkLogin(sessionRequest, response) } catch (_: IllegalStateException) { null } ?: return null
        if (context.user?.id != grant.userId) return null
        return Identity(context, grant.scopes.intersect(available(context)))
    }

    fun readMfaValid(entity: String, id: Long? = null): Boolean {
        val path = paths[entity] ?: return false
        return twoFactor.getRemainingPeriod("${Rest.URL}/$path/${id ?: "list"}")?.let { it > 0 } ?: true
    }

    fun available(context: UserContext): Set<String> {
        try {
            RegisterUser4Thread.registerUser(context)
            return setOf("projectforge.profile.read") + daos.filter { (entity, dao) ->
                dao.hasLoggedInUserSelectAccess(false) && readMfaValid(entity)
            }.keys.map { "projectforge.$it.read" }
        } finally { RegisterUser4Thread.unregister() }
    }

    @Synchronized
    private fun prune() {
        val time = now(); codes.entries.removeIf { it.value.expires < time }
        accesses.entries.removeIf { it.value.expires < time }; grants.values.filter { it.expires < time }.forEach(::revoke)
    }
    private fun bad(error: String): ResponseEntity<Any> = ResponseEntity.badRequest().header("Cache-Control", "no-store").body(mapOf("error" to error))

    companion object {
        // 43 alphanumeric characters carry about 256 bits, as much as the 32 random bytes of a PKCE verifier.
        private fun randomId() = NumberHelper.getSecureRandomAlphanumeric(43)
        private fun hidden(name: String, value: String) = HtmlElement("input", childrenAllowed = false).apply {
            attr("type", "hidden"); attr("name", name); attr("value", value)
        }
        private fun button(decision: String, label: String) = HtmlElement("button", content = label).apply {
            attr("name", "decision"); attr("value", decision)
        }
        private const val CONSENT_STYLE = "body{font:16px system-ui;background:#f4f6f8;color:#17313a;max-width:580px;margin:6vh auto;padding:20px}" +
            "main{background:white;border-radius:14px;padding:30px}h1{font-size:26px}" +
            "label{display:block;border:1px solid #dce5e7;border-radius:8px;padding:12px;margin:10px 0}" +
            "small{display:block;color:#607780;margin-left:25px;font-size:12px}" +
            "button{font:inherit;border:0;border-radius:7px;padding:12px 18px;cursor:pointer}" +
            "button[value=allow]{background:#087d78;color:white}.identity{background:#e5f5ee;padding:12px;border-radius:7px}"
        internal const val REGISTRATION_WINDOW = 600_000L
        internal const val MAX_REGISTRATIONS_PER_ADDRESS = 10
        internal const val MAX_UNCONFIRMED_CLIENTS = 500
        internal const val MAX_CONFIRMED_CLIENTS = 1000
        internal const val MAX_GRANTS_PER_USER = 10
        internal const val MAX_GRANTS = 5000
    }
}
