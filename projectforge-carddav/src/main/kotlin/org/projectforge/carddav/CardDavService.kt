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
import org.projectforge.carddav.model.AddressBook
import org.projectforge.carddav.model.User
import org.projectforge.carddav.service.AddressService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
class CardDavService {
    @Autowired
    private lateinit var addressService: AddressService

    /**
     * Handles the initial OPTIONS request to indicate supported methods and DAV capabilities.
     * This is the initial request to determine the allowed methods and DAV capabilities by the client.
     *
     * @return ResponseEntity with allowed methods and DAV capabilities in the headers.
     */
    fun handleDynamicOptions(request: HttpServletRequest): ResponseEntity<Void> {
        val requestedPath = request.requestURI

        val headers = HttpHeaders().apply {
            // Indicate DAV capabilities
            add("DAV", "1, 2, 3, addressbook")

            // Indicate allowed HTTP methods
            // add("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE, PROPFIND, REPORT")
            add("Allow", "OPTIONS, GET, PROPFIND")

            // Expose additional headers for client visibility
            add("Access-Control-Expose-Headers", "DAV, Allow")

            // Additional headers for user-specific paths
            if (requestedPath.contains("/users/")) {
                // Example: You might add user-specific behavior here
                add("Content-Type", "application/xml")
            }
        }

        return ResponseEntity.ok().headers(headers).build()
    }

    /**
     * Handle PROPFIND requests. Get the address book metadata for the given user.
     * @param user The user for which the address book is requested.
     * @param request The HTTP request.
     * @return The response entity.
     */
´    fun handlePropfindUsers(@PathVariable user: String, request: HttpServletRequest): ResponseEntity<String> {
        if (!request.method.equals("PROPFIND", ignoreCase = true)) {
            log.error { "Method not allowed: ${request.method}, PROPFIND expected." }
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }
        val response = StringBuilder()
        writeMultiStatusStart(response, request.serverName)
        val addressBook = AddressBook(User(user))
        addressService.getContactList(addressBook).forEach { contact ->
            writeResponse(response, user, contact.id, contact.lastUpdated, contact.displayName)
        }
        writeMultiStatusEnd(response)
        return ResponseEntity.status(HttpStatus.MULTI_STATUS)
            .contentType(MediaType.APPLICATION_XML)
            .body(response.toString())
    }

    /**
     * Handle PROPFIND requests. Get the address book metadata for the given user.
     * @param user The user for which the address book is requested.
     * @param request The HTTP request.
     * @return The response entity.
     */
+    fun handlePropfindAddressBook(@PathVariable user: String, request: HttpServletRequest): ResponseEntity<String> {
        if (!request.method.equals("PROPFIND", ignoreCase = true)) {
            log.error { "Method not allowed: ${request.method}, PROPFIND expected." }
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }
        val response = StringBuilder()
        writeMultiStatusStart(response, request.serverName)
        val addressBook = AddressBook(User(user))
        addressService.getContactList(addressBook).forEach { contact ->
            writeResponse(response, user, contact.id, contact.lastUpdated, contact.displayName)
        }
        writeMultiStatusEnd(response)
        return ResponseEntity.status(HttpStatus.MULTI_STATUS)
            .contentType(MediaType.APPLICATION_XML)
            .body(response.toString())
    }

´    fun getContact(@PathVariable user: String, @PathVariable contactId: String): ResponseEntity<String> {
        val vcard = contactId.toLongOrNull()?.let {
            addressService.getContact(it)?.vcardData?.toString(Charsets.UTF_8)
        }
        if (vcard == null) {
            log.error { "Invalid contact id: $contactId" }
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/vcard"))
            .body(vcard)
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
            addressId: Long?,
            etag: Date?,
            displayName: String,
        ) {
            sb.appendLine("  <d:response>")
                .append("    <d:href>/carddav/")
                .append(user)
                .append("/addressbook/contact")
                .append(addressId ?: -1L)
                .appendLine(".vcf</d:href>")
                .appendLine("    <d:propstat>")
                .appendLine("      <d:prop>")
                // According to the HTTP/1.1 specification, which is also used by CardDAV, ETags must be enclosed in double quotes.
                .append("        <d:getetag>\"")
                .append(etag?.time ?: -1)
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
