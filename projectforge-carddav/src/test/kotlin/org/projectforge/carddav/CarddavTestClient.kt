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

package org.projectforge.carddav

import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder
import org.apache.hc.core5.http.protocol.HttpCoreContext
import org.projectforge.common.extensions.abbreviate
import java.util.*

/*
https://developers.google.com/people/carddav?hl=de
Wireshark on macOS:
To fully complete your installation and use Wireshark
    to capture from devices (like network interfaces) please run:

      sudo dseditgroup -q -o edit -a [USER] -t user access_bpf

    and change [USER] to the user that needs access to the devices.
    A reboot should not be required for this to take effect.

    A startup item has been generated that will start wireshark-chmodbpf with launchd, and will be enabled automatically on activation. Execute the following command to manually _disable_ it:

        sudo port unload wireshark-chmodbpf
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: CardDavTestClient <username> <dav-token> [--fetch-all]")
        println("  --fetch-all: Fetch all VCards from server and save to /tmp/carddav-vcards/")
        return
    }
    val username = args[0]
    val davToken = args[1]
    val fetchAll = args.contains("--fetch-all")
    val baseUrl = if (args.size == 3) args[2] else "http://localhost:8080/carddav"
    // val baseUrl = "http://localhost:8080/carddav"
    val lastSyncToken = if (args.size > 3 && !fetchAll) args[3] else null
    val client = CardDavTestClient(baseUrl, username = username, password = davToken, lastSyncToken = lastSyncToken)
    if (fetchAll) {
        client.fetchAllVCards()
    } else {
        client.run()
    }
}

// Request with method=OPTIONS for Milton (uri=/users/username/, session-id=null)
// Basic authentication failed, header 'authorization' not found. (uri=/users/username/, session-id=null)
// PROPFIND call detected: /users/username/
// PROPFIND call detected: /users/username/addressBooks/
// PROPFIND for Milton (uri=/users/username/addressBooks/default/
// REPORT for Milton (uri=/users/username/addressBooks/default/

class CardDavTestClient(private val baseUrl: String, username: String, password: String, val lastSyncToken: String?) {
    private class ResponseData(val content: String, val status: Int, val headers: String)

    private val client: CloseableHttpClient = HttpClients.createDefault()
    private val authHeader: String = "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    private val responseHandler = HttpClientResponseHandler { response: ClassicHttpResponse ->
        val entity: HttpEntity? = response.entity
        val content = entity?.content?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        ResponseData(content, response.code, response.headers.joinToString { "${it.name}=${it.value}" })
    }

    /**
     * Run the CardDAV test client sequence: OPTIONS -> PROPFIND -> GET
     */
    fun run() {
        try {
            sendRequest("OPTIONS") // "/users/$username/")
            /*"""
                <?xml version="1.0" encoding="UTF-8"?>
                <A:propfind xmlns:A="DAV:">
                  <A:prop>
                    <A:current-user-principal/>
                    <A:principal-URL/>
                    <A:resourcetype/>
                  </A:prop>
                </A:propfind>
            """.trimIndent().let { body ->
                sendRequest("PROPFIND", requestBody = body, useAuthHeader = true)
            }*/
            sendRequest("GET", path = "/users/kai/addressbooks/ProjectForge-23468098.vcf", useAuthHeader = true)
            //sendRequest("GET", path = "/users/kai/addressbooks/ProjectForge-723108.vcf", useAuthHeader = true)
            sendRequest("GET", path = "/carddav/photos/contact-163.jpg", useAuthHeader = true, logResponseContent = false)
            /*
            """
                <propfind xmlns="DAV:">
                   <prop>
                     <resourcetype/>
                     <displayname/>
                     <current-user-principal/>
                     <current-user-privilege-set/>
                   </prop>
                 </propfind>
            """.trimIndent().let { body ->
                sendRequest("PROPFIND", requestBody = body, useAuthHeader = true)
            }
            """
                <propfind xmlns="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
                  <prop>
                     <resourcetype/>
                     <getetag/>
                     <cs:getctag/>
                   </prop>
                </propfind>""".trimIndent().let { body ->
                sendRequest("PROPFIND", requestBody = body, useAuthHeader = true)
            }
            var newSyncToken: String? = null
            """
                <propfind xmlns="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
                  <prop>
                    <displayname/>
                    <cs:getctag/>
                    <sync-token/>
                  </prop>
                </propfind>""".trimIndent().let { body ->
                sendRequest("PROPFIND", requestBody = body, useAuthHeader = true).let { response ->
                    val regex = Regex("<sync-token>\\s*(.*?)\\s*</sync-token>", RegexOption.DOT_MATCHES_ALL)
                    val match = regex.find(response)
                    newSyncToken = match?.groups?.get(1)?.value?.trim()
                    if (newSyncToken != lastSyncToken) {
                        println("**************************************")
                        println("New sync token: $newSyncToken (for usage as args[3])")
                        println("**************************************")
                    }
                    if (newSyncToken == null) {
                        newSyncToken = lastSyncToken
                    }
                }
            }
            """
                <sync-collection xmlns="DAV:" xmlns:CR="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
                  <sync-token>
                        $lastSyncToken
                  </sync-token>
                  <sync-level>1</sync-level>
                  <prop>
                    <getetag/>
                    <CR:address-data/>
                  </prop>
                </sync-collection>""".trimIndent().let { body ->
                sendRequest("REPORT", requestBody = body, useAuthHeader = true)
            }
            """
                <card:addressbook-multiget xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
      <d:prop>
        <d:getetag/>
        <card:address-data/>
      </d:prop>
      <d:href>/kai/testcollection/e8efcc9a-10f4-4d1c-8bb6-718aece30586.vcf</d:href>
      <d:href>/kai/testcollection/4ebaea4c-3a96-44dd-84eb-c2207d8f38f5.vcf</d:href>
    </card:addressbook-multiget>""".trimIndent().let { body ->
                sendRequest("REPORT", requestBody = body, useAuthHeader = true)
            }*/
        } finally {
            client.close()
        }
    }

    private fun sendRequest(
        method: String,
        path: String = "",
        requestBody: String? = null,
        useAuthHeader: Boolean = false,
        logResponseContent: Boolean = true,
    ): String {
        val url = "$baseUrl$path"
        val context = HttpCoreContext.create()
        val builder = ClassicRequestBuilder.create(method)
            .setUri(url)
            .addHeader("Depth", "1")
        if (useAuthHeader) {
            builder.addHeader("Authorization", authHeader)
        }
        if (requestBody != null) {
            builder.setEntity(StringEntity(requestBody, ContentType.APPLICATION_XML))
        }
        println("$method call: $url")
        if (requestBody != null) {
            println("   body=[")
            println(requestBody)
            println("   ]")
        }
        client.execute(builder.build(), context, responseHandler).also {
            logResponse(method, url, it, logResponseContent = logResponseContent)
            return it.content
        }
    }

    private fun sendSyncReportRequest(path: String = "", logResponseContent: Boolean = true) {
        val url = "$baseUrl$path"
        val context = HttpCoreContext.create()
        val body = """
            <?xml version="1.0" encoding="UTF-8"?>´
            <d:sync-collection xmlns:d="DAV:">
                <d:prop>
                    <d:getetag />
                </d:prop>
            </d:sync-collection>
    """.trimIndent()

        val builder = ClassicRequestBuilder.create("REPORT")
            .setUri(url)
            .addHeader("Depth", "1")
            .addHeader("Authorization", authHeader)
        builder.setEntity(StringEntity(body, ContentType.APPLICATION_XML))

        val response = client.execute(builder.build(), context, responseHandler).also {
            logResponse("REPORT", url, it, logResponseContent = logResponseContent)
        }
    }

    private fun sendGetRequest(path: String = "", useAuthHeader: Boolean = false, logResponseContent: Boolean = true) {
        val url = "$baseUrl$path"
        val context = HttpCoreContext.create()
        val getRequest = HttpGet(url)
        if (useAuthHeader) {
            getRequest.addHeader("Authorization", authHeader)
        }
        val result = client.execute(getRequest, context, responseHandler)
        logResponse("GET", url, result, logResponseContent = logResponseContent)
    }

    private fun logResponse(method: String, endpoint: String, response: ResponseData, logResponseContent: Boolean) {
        println("$method: $endpoint: response=[")
        println("   status=[${response.status}], headers=[${response.headers}], content-length=${response.content.length}")
        if (response.content.isNotEmpty()) {
            if (logResponseContent) {
                println("   content=[")
                println(response.content.abbreviate(8000))
                println("   ]")
            } else {
                println("   content-size=${response.content.length}")
            }
        }
        println("]")
    }

    /**
     * Fetch all VCards from server, save them to disk, and analyze PHOTO entries.
     * This helps identify differences between JPEG and PNG image handling.
     */
    fun fetchAllVCards() {
        val outputDir = java.io.File("/tmp/carddav-vcards")
        outputDir.mkdirs()

        println("=".repeat(80))
        println("CardDAV VCard Fetcher - Analyzing PHOTO entries")
        println("=".repeat(80))
        println("Server: $baseUrl")
        println("Output: ${outputDir.absolutePath}")
        println("=".repeat(80))

        // Step 1: Get list of all contacts via REPORT
        println("\n[1] Fetching contact list via REPORT...")
        val contactUrls = fetchContactList()
        println("Found ${contactUrls.size} contacts\n")

        // Step 2: Fetch each VCard and analyze
        println("[2] Fetching and analyzing VCards...")
        val stats = mutableMapOf(
            "jpeg_url" to 0,
            "png_url" to 0,
            "gif_url" to 0,
            "embedded_jpeg" to 0,
            "embedded_png" to 0,
            "embedded_gif" to 0,
            "no_photo" to 0,
            "error" to 0
        )

        contactUrls.forEachIndexed { index, contactUrl ->
            try {
                val vcard = fetchVCardForAnalysis(contactUrl)
                val filename = contactUrl.substringAfterLast("/")
                val outputFile = java.io.File(outputDir, filename)
                outputFile.writeText(vcard)

                // Analyze PHOTO entry
                val photoInfo = analyzePhoto(vcard)
                println("  [${index + 1}/${contactUrls.size}] $filename - $photoInfo")

                // Update statistics
                when {
                    photoInfo.contains("URL") && photoInfo.contains("JPEG", ignoreCase = true) -> stats["jpeg_url"] = stats["jpeg_url"]!! + 1
                    photoInfo.contains("URL") && photoInfo.contains("PNG", ignoreCase = true) -> stats["png_url"] = stats["png_url"]!! + 1
                    photoInfo.contains("URL") && photoInfo.contains("GIF", ignoreCase = true) -> stats["gif_url"] = stats["gif_url"]!! + 1
                    photoInfo.contains("EMBEDDED") && photoInfo.contains("JPEG", ignoreCase = true) -> stats["embedded_jpeg"] = stats["embedded_jpeg"]!! + 1
                    photoInfo.contains("EMBEDDED") && photoInfo.contains("PNG", ignoreCase = true) -> stats["embedded_png"] = stats["embedded_png"]!! + 1
                    photoInfo.contains("EMBEDDED") && photoInfo.contains("GIF", ignoreCase = true) -> stats["embedded_gif"] = stats["embedded_gif"]!! + 1
                    photoInfo.contains("No PHOTO") -> stats["no_photo"] = stats["no_photo"]!! + 1
                }
            } catch (e: Exception) {
                println("  [${index + 1}/${contactUrls.size}] ERROR: ${contactUrl.substringAfterLast("/")} - ${e.message}")
                stats["error"] = stats["error"]!! + 1
            }
        }

        // Print summary
        println("\n" + "=".repeat(80))
        println("Summary:")
        println("  JPEG URLs:       ${stats["jpeg_url"]}")
        println("  PNG URLs:        ${stats["png_url"]}")
        println("  GIF URLs:        ${stats["gif_url"]}")
        println("  Embedded JPEG:   ${stats["embedded_jpeg"]}")
        println("  Embedded PNG:    ${stats["embedded_png"]}")
        println("  Embedded GIF:    ${stats["embedded_gif"]}")
        println("  No photo:        ${stats["no_photo"]}")
        println("  Errors:          ${stats["error"]}")
        println("  Total:           ${contactUrls.size}")
        println()
        println("VCards saved to: ${outputDir.absolutePath}")
        println("=".repeat(80))
    }

    private fun fetchContactList(): List<String> {
        val path = ""
        val reportXml = """
            <?xml version="1.0" encoding="utf-8" ?>
            <C:addressbook-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:carddav">
              <D:prop>
                <D:getetag/>
              </D:prop>
            </C:addressbook-query>
        """.trimIndent()

        val response = sendRequest("REPORT", path = path, requestBody = reportXml, useAuthHeader = true, logResponseContent = false)

        // Extract hrefs from XML response
        val hrefRegex = """<d:href>(.+?)</d:href>""".toRegex(RegexOption.IGNORE_CASE)
        return hrefRegex.findAll(response)
            .map { it.groupValues[1] }
            .filter { it.endsWith(".vcf") }
            .toList()
    }

    private fun fetchVCardForAnalysis(contactPath: String): String {
        // contactPath is relative, e.g., "/carddav/users/admin/addressbooks/ProjectForge-123.vcf"
        val path = contactPath.removePrefix(baseUrl)
        return sendRequest("GET", path = path, useAuthHeader = true, logResponseContent = false)
    }

    private fun analyzePhoto(vcard: String): String {
        // Check if PHOTO exists
        if (!vcard.contains("PHOTO", ignoreCase = true)) {
            return "No PHOTO"
        }

        // Extract PHOTO line(s) - VCard properties can span multiple lines
        val photoLines = mutableListOf<String>()
        var inPhoto = false
        vcard.lines().forEach { line ->
            when {
                line.startsWith("PHOTO", ignoreCase = true) -> {
                    inPhoto = true
                    photoLines.add(line)
                }
                inPhoto && (line.startsWith(" ") || line.startsWith("\t")) -> {
                    // Continuation line (folded)
                    photoLines.add(line.trim())
                }
                inPhoto -> {
                    // End of PHOTO property
                    inPhoto = false
                }
            }
        }

        val photoProperty = photoLines.joinToString("")

        // Analyze PHOTO property
        return when {
            photoProperty.contains("VALUE=URI", ignoreCase = true) || photoProperty.contains("VALUE=URL", ignoreCase = true) -> {
                // URL mode
                val urlMatch = """PHOTO[^:]*:(.+)""".toRegex(RegexOption.IGNORE_CASE).find(photoProperty)
                val url = urlMatch?.groupValues?.get(1)?.trim() ?: "unknown"
                val typeMatch = """TYPE=([^;:]+)""".toRegex(RegexOption.IGNORE_CASE).find(photoProperty)
                val type = typeMatch?.groupValues?.get(1)?.toUpperCase() ?: "UNKNOWN"
                "PHOTO URL ($type): ${url.take(60)}${if (url.length > 60) "..." else ""}"
            }
            photoProperty.contains("ENCODING=b", ignoreCase = true) ||
            photoProperty.contains("ENCODING=B", ignoreCase = true) ||
            photoProperty.contains("data:image/", ignoreCase = true) -> {
                // Embedded mode (base64 or data URI)
                val typeMatch = """TYPE=([^;:]+)""".toRegex(RegexOption.IGNORE_CASE).find(photoProperty)
                    ?: """data:image/([^;]+)""".toRegex().find(photoProperty)
                val type = typeMatch?.groupValues?.get(1)?.toUpperCase() ?: "UNKNOWN"
                val dataLength = photoProperty.length
                "PHOTO EMBEDDED ($type): ${dataLength} chars"
            }
            else -> {
                // Unknown format
                "PHOTO: ${photoProperty.take(100)}${if (photoProperty.length > 100) "..." else ""}"
            }
        }
    }

    /*
    Response PROPFIND https://projectforge.org/users/username
    <?xml version="1.0" encoding="UTF-8"?>
    <d:multistatus xmlns:d="DAV:">
        <d:response>
            <d:href>http://projectforge.org/users/username/</d:href>
            <d:propstat>
                <d:prop>
                    <d:displayname>username</d:displayname>
                    <d:getcontenttype/>
                </d:prop>
                <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
        </d:response>
        <d:response>
            <d:href>http://projectforge.org/users/username/cals/</d:href>
            <d:propstat>
                <d:prop>
                    <d:displayname>cals</d:displayname>
                    <d:getcontenttype/>
                </d:prop>
                <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
        </d:response>
        <d:response>
            <d:href>http://projectforge.org/users/username/addressBooks/</d:href>
            <d:propstat>
                <d:prop>
                    <d:displayname>addressBooks</d:displayname>
                    <d:getcontenttype/>
                </d:prop>
                <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
        </d:response>
     </d:multistatus>
     */

    /*
    Response REPORT https://projectforge.org/users/username/addressBooks/default/
    Old server: empty response
     */
}
