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

package org.projectforge.carddav

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/carddav")
class CardDavController {
    /**
     * Handle PROPFIND requests. Get the address book metadata for the given user.
     * @param user The user for which the address book is requested.
     * @param request The HTTP request.
     * @return The response entity.
     */
    @RequestMapping(value = ["/{user}/addressbook"])
    fun handlePropfind(@PathVariable user: String, request: HttpServletRequest): ResponseEntity<String> {
        if (!request.method.equals("PROPFIND", ignoreCase = true)) {
            log.error { "Method not allowed: ${request.method}, PROPFIND expected." }
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }
        val response = generatePropfindResponse(user)
        return ResponseEntity.status(HttpStatus.MULTI_STATUS)
            .contentType(MediaType.APPLICATION_XML)
            .body(response)
    }

    @RequestMapping(value = ["/**"], method = [RequestMethod.OPTIONS])
    fun handleOptions(): ResponseEntity<Void> {
        val headers = HttpHeaders().apply {
            add("DAV", "1, 2")
            add("Allow", "OPTIONS, PROPFIND, GET")
        }
        return ResponseEntity.ok().headers(headers).build()
    }

    @GetMapping("/{user}/addressbook/{contactId}")
    fun getContact(@PathVariable user: String, @PathVariable contactId: String): ResponseEntity<String> {
        val vCard = ""//contactService.getContactAsVCard(user, contactId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/vcard"))
            .body(vCard)
    }

    private fun generatePropfindResponse(user: String): String {
        return """
            <d:multistatus xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/">
                <d:response>
                    <d:href>/carddav/$user/addressbook/contact1.vcf</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getetag>"12345"</d:getetag>
                            <d:displayname>John Doe</d:displayname>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                <d:response>
                    <d:href>/carddav/user/addressbook/contact2.vcf</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getetag>"67890"</d:getetag>
                            <d:displayname>Jane Doe</d:displayname>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/carddav/$user/addressbook/</d:href>
                    <d:propstat>
                        <d:status>HTTP/1.1 200 OK</d:status>
                        <d:prop>
                            <d:displayname>$user's Address Book</d:displayname>
                        </d:prop>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()
    }

    companion object {
        internal fun writeMultiStatusStart(sb: StringBuilder, server: String) {
            sb.append("<d:multistatus xmlns:d=\"DAV:\" xmlns:cs=\"https://")
                .append(server)
                .append("/ns/\">\n")
        }

        internal fun writeMultiStatusEnd(sb: StringBuilder) {
            sb.append("</d:multistatus>\n")
        }

        internal fun writeResponse(
            sb: StringBuilder,
            user: String,
            addressId: Long,
            etag: String,
            displayName: String,
        ) {
            sb.appendLine("  <d:response>")
                .append("    <d:href>/carddav/")
                .append(user)
                .append("/addressbook/contact")
                .append(addressId)
                .appendLine(".vcf</d:href>")
                .appendLine("    <d:propstat>")
                .appendLine("      <d:prop>")
                // According to the HTTP/1.1 specification, which is also used by CardDAV, ETags must be enclosed in double quotes.
                .append("        <d:getetag>\"")
                .append(etag)
                .appendLine("\"</d:getetag>")
                .append("        <d:displayname>")
                .append(displayName)
                .appendLine("</d:displayname>")
                .appendLine("      </d:prop>")
                .appendLine("      <d:status>HTTP/1.1 200 OK</d:status>")
                .appendLine("    </d:propstat>")
                .appendLine("  </d:response>")
        }
    }

    // <d:multistatus xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/">
    //    <d:sync-token>http://example.com/sync/abcd1234</d:sync-token>
    //    <d:response>
    //        <d:href>/carddav/user/addressbook/contact1.vcf</d:href>
    //        <d:propstat>
    //            <d:prop>
    //                <d:getetag>"12345"</d:getetag>
    //                <d:displayname>John Doe</d:displayname>
    //            </d:prop>
    //            <d:status>HTTP/1.1 200 OK</d:status>
    //        </d:propstat>
    //    </d:response>
    //    <d:response>
    //        <d:href>/carddav/user/addressbook/contact2.vcf</d:href>
    //        <d:propstat>
    //            <d:prop>
    //                <d:getetag>"67890"</d:getetag>
    //                <d:displayname>Jane Doe</d:displayname>
    //            </d:prop>
    //            <d:status>HTTP/1.1 200 OK</d:status>
    //        </d:propstat>
    //    </d:response>
    //</d:multistatus>
}
