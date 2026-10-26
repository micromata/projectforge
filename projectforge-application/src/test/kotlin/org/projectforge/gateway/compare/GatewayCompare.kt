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

package org.projectforge.gateway.compare

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Properties
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Sends the same CardDAV and ICS requests to the main instance and to the gateway and compares the answers, see
 * doc/HOWTO-TEST-GATEWAY.md, *Comparison with the main instance*.
 *
 * Usage: GatewayCompareKt [config file], default ~/ProjectForge/gateway-compare.properties:
 * ```
 * main.url=https://projectforge.example.com
 * gateway.url=https://gateway.example.com
 * # CardDAV: user name and DAV token (My account, authentication passwords)
 * carddav.username=joe
 * carddav.token=...
 * # ICS: subscription urls as shown by the subscription dialog of the main instance
 * ics.1=https://projectforge.example.com/export/ProjectForge.ics?user=42&q=...
 * ics.2=...
 * # Optional:
 * output.dir=~/ProjectForge/gateway-compare
 * # Trust any certificate (self-signed test installations only)
 * tls.insecure=false
 * # Requests with a wrong token, which must be refused. Test installations only: on the main instance they count as
 * # failed logins of the user (LoginProtection: increasing delay for the user and the ip) and are logged as errors.
 * negative.enabled=false
 * ```
 * The raw answers (with personal data of the addresses and calendars) are written to the output dir only, the
 * report shows no tokens.
 */
fun main(args: Array<String>) {
    val configFile = File(args.firstOrNull() ?: "${System.getProperty("user.home")}/ProjectForge/gateway-compare.properties")
    if (!configFile.exists()) {
        System.err.println("Config file not found: ${configFile.absolutePath} (see KDoc of GatewayCompare.kt).")
        return
    }
    val config = GatewayCompareConfig.load(configFile)
    val report = GatewayCompare(config).run()
    println(report.summary())
    println("Report: ${report.file.absolutePath}")
}

class GatewayCompareConfig(
    val mainUrl: String,
    val gatewayUrl: String,
    val carddavUsername: String?,
    val carddavToken: String?,
    val icsUrls: List<String>,
    val outputDir: File,
    val tlsInsecure: Boolean,
    val negativeEnabled: Boolean,
) {
    companion object {
        fun load(file: File): GatewayCompareConfig {
            val props = Properties().apply { file.inputStream().use { load(it) } }
            fun get(key: String) = props.getProperty(key)?.trim()?.ifBlank { null }
            val home = System.getProperty("user.home")
            return GatewayCompareConfig(
                mainUrl = requireNotNull(get("main.url")) { "main.url missing" }.trimEnd('/'),
                gatewayUrl = requireNotNull(get("gateway.url")) { "gateway.url missing" }.trimEnd('/'),
                carddavUsername = get("carddav.username"),
                carddavToken = get("carddav.token"),
                icsUrls = props.stringPropertyNames().filter { it.startsWith("ics.") }
                    .sortedBy { it.removePrefix("ics.").toIntOrNull() ?: Int.MAX_VALUE }.mapNotNull { get(it) },
                outputDir = File((get("output.dir") ?: "$home/ProjectForge/gateway-compare").replaceFirst("~", home)),
                tlsInsecure = get("tls.insecure") == "true",
                negativeEnabled = get("negative.enabled") == "true",
            )
        }
    }
}

class GatewayCompare(private val config: GatewayCompareConfig) {
    class Answer(val status: Int, val headers: Map<String, List<String>>, val body: ByteArray) {
        val text: String get() = String(body, Charsets.UTF_8)
        fun header(name: String): String? =
            headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.joinToString(", ")
    }

    enum class Result { OK, DIFF, ERROR }

    class Case(val name: String, val result: Result, val details: String, val diff: List<String> = emptyList())

    class Report(val file: File, val cases: List<Case>) {
        fun summary(): String = Result.entries.joinToString(", ") { r -> "${cases.count { it.result == r }} $r" }
    }

    private val runDir = File(config.outputDir, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))
    private val cases = mutableListOf<Case>()
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NEVER) // Redirects (e.g. of .well-known) are compared, not followed.
        .apply { if (config.tlsInsecure) sslContext(trustAllContext()) }
        .build()

    fun run(): Report {
        runDir.mkdirs()
        config.icsUrls.forEachIndexed { index, url -> compareIcs("ics-${index + 1}", url) }
        if (config.negativeEnabled && config.icsUrls.isNotEmpty()) {
            compareIcsNegative(config.icsUrls.first())
        }
        if (config.carddavUsername != null && config.carddavToken != null) {
            CardDavCompare(this, config.carddavUsername, config.carddavToken, config.negativeEnabled).run()
        }
        val file = File(runDir, "report.md")
        file.writeText(reportText())
        return Report(file, cases)
    }

    // ---------------------------------------------------------------------------------------------------------- ICS

    private fun compareIcs(name: String, mainUrl: String) {
        val answers = send(name, "GET", mainUrl) ?: return
        val (main, gateway) = answers
        val problems = mutableListOf<String>()
        if (main.status != gateway.status) problems += "status main=${main.status}, gateway=${gateway.status}"
        for (header in listOf("Content-Type", "Content-Disposition")) {
            if (normalizeContentType(main.header(header)) != normalizeContentType(gateway.header(header))) {
                problems += "$header main=${main.header(header)}, gateway=${gateway.header(header)}"
            }
        }
        val mainNorm = IcsNormalizer.normalize(main.text)
        val gatewayNorm = IcsNormalizer.normalize(gateway.text)
        writeNormalized(name, mainNorm, gatewayNorm)
        val diff = lineDiff(mainNorm, gatewayNorm)
        if (diff.isNotEmpty()) problems += "content differs"
        val events = "events main=${IcsNormalizer.countEvents(main.text)}, gateway=${IcsNormalizer.countEvents(gateway.text)}"
        add(name, problems, "${maskUrl(mainUrl)}: status ${main.status}, $events", diff)
    }

    /**
     * Requests, which must fail on both instances (the status codes may differ, e.g. 400 and 401).
     */
    private fun compareIcsNegative(url: String) {
        val q = Regex("[?&]q=([^&]+)").find(url)?.groupValues?.get(1) ?: return
        val tampered = q.dropLast(4) + (if (q.endsWith("AAAA")) "BBBB" else "AAAA")
        expectRefusedOnBoth("ics-wrong-q", "GET", url.replace(q, tampered))
        expectRefusedOnBoth("ics-unknown-user", "GET", url.replace(Regex("([?&]user=)\\d+"), "$1999999999"))
    }

    // ------------------------------------------------------------------------------------------------------ helpers

    /**
     * @param mainUrl The url on the main instance; on the gateway the base url is replaced.
     * @return The answers of the main instance and the gateway, or null if a request failed (case added as ERROR).
     */
    internal fun send(
        name: String,
        method: String,
        mainUrl: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        gatewayUrl: String = toGatewayUrl(mainUrl),
    ): Pair<Answer, Answer>? {
        val main = sendTo("main", name, method, mainUrl, headers, body) ?: return null
        val gateway = sendTo("gateway", name, method, gatewayUrl, headers, body) ?: return null
        return main to gateway
    }

    internal fun sendTo(
        side: String,
        name: String,
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): Answer? {
        return try {
            val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120))
                .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody())
            headers.forEach { (key, value) -> builder.header(key, value) }
            if (body != null && headers.keys.none { it.equals("Content-Type", true) }) {
                builder.header("Content-Type", "application/xml; charset=utf-8")
            }
            val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
            Answer(response.statusCode(), response.headers().map(), response.body()).also {
                writeRaw(side, name, method, url, it)
            }
        } catch (ex: Exception) {
            add(name, listOf("$side: ${ex::class.simpleName}: ${ex.message}"), "$method ${maskUrl(url)}", result = Result.ERROR)
            null
        }
    }

    internal fun expectRefusedOnBoth(name: String, method: String, mainUrl: String, headers: Map<String, String> = emptyMap(), body: String? = null) {
        val (main, gateway) = send(name, method, mainUrl, headers, body) ?: return
        val problems = mutableListOf<String>()
        if (main.status !in 400..499) problems += "main answered ${main.status}, expected 4xx"
        if (gateway.status !in 400..499) problems += "gateway answered ${gateway.status}, expected 4xx"
        add(name, problems, "refused: main=${main.status}, gateway=${gateway.status}")
    }

    internal fun add(name: String, problems: List<String>, details: String, diff: List<String> = emptyList(), result: Result? = null) {
        val r = result ?: if (problems.isEmpty()) Result.OK else Result.DIFF
        val text = if (problems.isEmpty()) details else "$details; ${problems.joinToString("; ")}"
        cases += Case(name, r, text, diff)
        println("[$r] $name: $text")
    }

    internal fun toGatewayUrl(mainUrl: String): String {
        require(mainUrl.startsWith(config.mainUrl)) { "Url doesn't start with main.url (${config.mainUrl}): ${maskUrl(mainUrl)}" }
        return config.gatewayUrl + mainUrl.removePrefix(config.mainUrl)
    }

    internal val mainUrl: String get() = config.mainUrl
    internal val gatewayUrl: String get() = config.gatewayUrl

    internal fun writeNormalized(name: String, main: String, gateway: String) {
        File(runDir, "main").apply { mkdirs() }.resolve("$name.norm.txt").writeText(main)
        File(runDir, "gateway").apply { mkdirs() }.resolve("$name.norm.txt").writeText(gateway)
    }

    private fun writeRaw(side: String, name: String, method: String, url: String, answer: Answer) {
        val dir = File(runDir, side).apply { mkdirs() }
        val headerText = answer.headers.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}: ${it.value.joinToString(", ")}" }
        val content = if (isText(answer)) answer.text else "<${answer.body.size} bytes, sha256=${sha256(answer.body)}>"
        dir.resolve("$name.txt").writeText("$method ${maskUrl(url)}\nHTTP ${answer.status}\n$headerText\n\n$content")
    }

    private fun isText(answer: Answer): Boolean {
        val type = answer.header("Content-Type") ?: return true
        return type.contains("text") || type.contains("xml") || type.contains("json") || type.contains("vcard") || type.contains("calendar")
    }

    private fun reportText(): String {
        val sb = StringBuilder()
        sb.appendLine("# Gateway comparison ${runDir.name}")
        sb.appendLine()
        sb.appendLine("Main instance: ${config.mainUrl}, gateway: ${config.gatewayUrl}. ${Report(File(""), cases).summary()}.")
        sb.appendLine("Raw answers in `main/` and `gateway/`, normalized ones as `*.norm.txt`" +
                " (`diff -u main/<case>.norm.txt gateway/<case>.norm.txt`).")
        sb.appendLine()
        sb.appendLine("| Case | Result | Details |")
        sb.appendLine("|------|--------|---------|")
        cases.forEach { sb.appendLine("| ${it.name} | ${it.result} | ${it.details.replace("|", "\\|")} |") }
        cases.filter { it.diff.isNotEmpty() }.forEach { case ->
            sb.appendLine()
            sb.appendLine("## ${case.name}")
            sb.appendLine()
            sb.appendLine("```diff")
            case.diff.take(MAX_DIFF_LINES).forEach { sb.appendLine(it) }
            if (case.diff.size > MAX_DIFF_LINES) sb.appendLine("... ${case.diff.size - MAX_DIFF_LINES} more lines")
            sb.appendLine("```")
        }
        return sb.toString()
    }

    companion object {
        private const val MAX_DIFF_LINES = 60

        /**
         * Lines missing on the gateway (-) and lines only on the gateway (+), compared as multisets: the normalized
         * answers are sorted, so the position of a line says little.
         */
        fun lineDiff(main: String, gateway: String): List<String> {
            val counts = mutableMapOf<String, Int>()
            main.lines().forEach { counts.merge(it, 1, Int::plus) }
            gateway.lines().forEach { counts.merge(it, -1, Int::plus) }
            val result = mutableListOf<String>()
            main.lines().distinct().forEach { line -> repeat(maxOf(0, counts[line] ?: 0)) { result += "-$line" } }
            gateway.lines().distinct().forEach { line -> repeat(maxOf(0, -(counts[line] ?: 0))) { result += "+$line" } }
            return result
        }

        /**
         * The encrypted parameters q of a subscription url are as secret as the token: never in the report.
         */
        fun maskUrl(url: String): String = url.replace(Regex("([?&]q=)[^&]+"), "$1***")

        internal fun normalizeContentType(value: String?): String? = value?.lowercase()?.replace(" ", "")

        internal fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        internal fun basicAuth(username: String, password: String): String =
            "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())

        private fun trustAllContext(): SSLContext {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            return SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trustAll), SecureRandom()) }
        }
    }
}
