/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.testclient

import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpOptions
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
        println("Usage: CardDavTestClient <username> <dav-token>")
        return
    }
    val username = args[0]
    val davToken = args[1]
    val baseUrl = if (args.size > 2) args[2] else "http://localhost:8080/carddav"
    val client = CardDavTestClient(baseUrl, username, davToken)
    client.run()
}

// Request with method=OPTIONS for Milton (uri=/users/username/, session-id=null)
// Basic authentication failed, header 'authorization' not found. (uri=/users/username/, session-id=null)
// PROPFIND call detected: /users/username/
// PROPFIND call detected: /users/username/addressBooks/
// PROPFIND for Milton (uri=/users/username/addressBooks/default/
// REPORT for Milton (uri=/users/username/addressBooks/default/

class CardDavTestClient(private val baseUrl: String, private val username: String, private val password: String) {
    private class ResponseData(val content: String, val headers: String)

    private val client: CloseableHttpClient = HttpClients.createDefault()
    private val authHeader: String = "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    private val responseHandler = HttpClientResponseHandler { response: ClassicHttpResponse ->
        val entity: HttpEntity? = response.entity
        val content = entity?.content?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        ResponseData(content, response.headers.joinToString { "${it.name}=${it.value}" })
    }

    /**
     * Run the CardDAV test client sequence: OPTIONS -> PROPFIND -> GET
     */
    fun run() {
        try {
            sendOptionsRequest("/users/$username/")
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
                sendPropfindRequest(requestBody = body, useAuthHeader = true)
            }
            """
                <propfind xmlns="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
                  <prop>
                     <resourcetype/>
                     <getetag/>
                     <cs:getctag/>
                   </prop>
                </propfind>""".trimIndent().let { body ->
                sendPropfindRequest(requestBody = body, useAuthHeader = true)
            }
            """
                <propfind xmlns="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
                  <prop>
                    <displayname/>
                    <cs:getctag/>
                    <sync-token/>
                  </prop>
                </propfind>""".trimIndent().let { body ->
                sendPropfindRequest(requestBody = body, useAuthHeader = true)
            }
         //sendGetRequest(propfindResponse)
            //sendSyncReportRequest("/users/$username/addressBooks/default/")
        } finally {
            client.close()
        }
    }

    private fun sendOptionsRequest(path: String = "", useAuthHeader: Boolean = false) {
        val url = "$baseUrl$path"
        val optionsRequest = HttpOptions(url)
        if (useAuthHeader) {
            optionsRequest.addHeader("Authorization", authHeader)
        }
        val context = HttpCoreContext.create()
        val repsonse = client.execute(optionsRequest, context, responseHandler)
        logResponse("OPTIONS", url, repsonse)
    }

    private fun sendPropfindRequest(path: String = "", requestBody: String, useAuthHeader: Boolean = false) {
        val url = "$baseUrl$path"
        val context = HttpCoreContext.create()
        val builder = ClassicRequestBuilder.create("PROPFIND")
            .setUri(url)
            .addHeader("Depth", "1")
        if (useAuthHeader) {
            builder.addHeader("Authorization", authHeader)
        }
        builder.setEntity(StringEntity(requestBody, ContentType.APPLICATION_XML))
        println("PROPFIND call: $path")
        println("   body=[")
        println(requestBody)
        println("   ]")
        client.execute(builder.build(), context, responseHandler).also {
            logResponse("PROPFIND", url, it)
        }
    }

    private fun sendSyncReportRequest(path: String = "") {
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
            logResponse("REPORT", url, it)
        }
    }

    private fun sendGetRequest(path: String = "", useAuthHeader: Boolean = false) {
        val url = "$baseUrl$path"
        val context = HttpCoreContext.create()
        val getRequest = HttpGet(url)
        if (useAuthHeader) {
            getRequest.addHeader("Authorization", authHeader)
        }
        val result = client.execute(getRequest, context, responseHandler)
        logResponse("GET", url, result)
    }

    private fun logResponse(method: String, endpoint: String, response: ResponseData) {
        println("$method: $endpoint: response=[")
        println("   headers=[${response.headers}]")
        println("   content=[")
        println("${response.content.abbreviate(1000)}")
        println("   ]")
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
