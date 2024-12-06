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
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.carddav.model.AddressBook
import org.projectforge.carddav.model.User
import org.projectforge.carddav.service.AddressService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.projectforge.web.rest.RestAuthenticationInfo
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class CardDavService {
    @Autowired
    private lateinit var addressService: AddressService

    @Autowired
    private lateinit var restAuthenticationUtils: RestAuthenticationUtils

    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    fun dispatch(request: HttpServletRequest, response: HttpServletResponse) {
        if (request.method == "OPTIONS") {
            // No login required for OPTIONS.
            handleDynamicOptions(request, response)
            return
        }
        val authInfo = authenticate(request, response)
        val user = authInfo.user
        if (user == null) {
            log.error { "Authentication failed: ${RequestLog.asString(request)}" }
            response.status = HttpStatus.UNAUTHORIZED.value()
            return
        }
        try {
            // Register user in threadlocal for further usage:
            restAuthenticationUtils.registerUser(request, authInfo, UserTokenType.DAV_TOKEN)
            dispathAuthenticated(request, response, user)
        } finally {
            restAuthenticationUtils.unregister(request, response, authInfo)
        }
    }

    private fun dispathAuthenticated(request: HttpServletRequest, response: HttpServletResponse, user: PFUserDO) {
        // val path = request.requestURI
        val method = request.method
        if (method == "PROPFIND") {
            handlePropfindUsers(request, response, user)
        }
        response.status = HttpStatus.METHOD_NOT_ALLOWED.value()
    }

    private fun authenticate(request: HttpServletRequest, response: HttpServletResponse): RestAuthenticationInfo {
        val authInfo = RestAuthenticationInfo(request, response)
        log.debug { "Trying to authenticate user (${RequestLog.asString(authInfo.request)})..." }
        restAuthenticationUtils.basicAuthentication(
            authInfo,
            UserTokenType.DAV_TOKEN,
            true
        ) { userString, authenticationToken ->
            val authenticatedUser = userAuthenticationsService.getUserByToken(
                authInfo.request,
                userString,
                UserTokenType.DAV_TOKEN,
                authenticationToken
            )
            if (authenticatedUser == null) {
                val msg = "Can't authenticate user '$userString' by given token. User name and/or token invalid (${
                    RequestLog.asString(authInfo.request)
                }."
                log.error(msg)
                SecurityLogging.logSecurityWarn(
                    authInfo.request,
                    this::class.java,
                    "${UserTokenType.DAV_TOKEN.name} AUTHENTICATION FAILED",
                    msg
                )
            } else {
                log.debug {
                    "Registering authenticated user: ${
                        RequestLog.asString(
                            authInfo.request,
                            authenticatedUser.username
                        )
                    }"
                }
                log.info {
                    "Authenticated user registered: ${
                        RequestLog.asString(
                            authInfo.request,
                            authenticatedUser.username
                        )
                    }"
                }
            }
            authenticatedUser
        }
        return authInfo
    }

    /**
     * Handles the initial OPTIONS request to indicate supported methods and DAV capabilities.
     * This is the initial request to determine the allowed methods and DAV capabilities by the client.
     *
     * @return ResponseEntity with allowed methods and DAV capabilities in the headers.
     */
    private fun handleDynamicOptions(request: HttpServletRequest, response: HttpServletResponse) {
        val requestedPath = request.requestURI
        // Indicate DAV capabilities
        response.addHeader("DAV", "1, 2, 3, addressbook")

        // Indicate allowed HTTP methods
        // add("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE, PROPFIND, REPORT")
        response.addHeader("Allow", "OPTIONS, GET, PROPFIND")

        // Expose additional headers for client visibility
        response.addHeader("Access-Control-Expose-Headers", "DAV, Allow")

        // Additional headers for user-specific paths
        if (requestedPath.contains("/users/")) {
            // Example: You might add user-specific behavior here
            response.addHeader("Content-Type", "application/xml")
        }
        response.status = HttpStatus.OK.value()
    }

    /**
     * Handle PROPFIND requests. Get the address book metadata for the given user.
     * @param userDO The user for which the address book is requested.
     * @param request The HTTP request.
     * @return The response entity.
     */
    private fun handlePropfindUsers(request: HttpServletRequest, response: HttpServletResponse, userDO: PFUserDO) {
        val sb = StringBuilder()
        CardDavXmlWriter.appendMultiStatusStart(sb, request.serverName)
        val user = User(userDO.username)
        val addressBook = AddressBook(user)
        addressService.getContactList(addressBook).forEach { contact ->
            CardDavXmlWriter.appendPropfindContact(sb, user, contact)
        }
        CardDavXmlWriter.appendMultiStatusEnd(sb)
        response.contentType = MediaType.APPLICATION_XML_VALUE
        response.status = HttpStatus.MULTI_STATUS.value()
        response.writer.write(sb.toString())
    }

    /**
     * Handle PROPFIND requests. Get the address book metadata for the given user.
     * @param userDO The user for which the address book is requested.
     * @param request The HTTP request.
     * @return The response entity.
     */
    fun handlePropfindAddressBook(
        request: HttpServletRequest,
        response: HttpServletResponse,
        userDO: PFUserDO
    ): ResponseEntity<String> {
        if (!request.method.equals("PROPFIND", ignoreCase = true)) {
            log.error { "Method not allowed: ${request.method}, PROPFIND expected." }
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }
        val sb = StringBuilder()
        CardDavXmlWriter.appendMultiStatusStart(sb, request.serverName)
        val user = User(userDO.username)
        val addressBook = AddressBook(user)
        addressService.getContactList(addressBook).forEach { contact ->
            CardDavXmlWriter.appendPropfindContact(sb, user, contact)
        }
        CardDavXmlWriter.appendMultiStatusEnd(sb)
        return ResponseEntity.status(HttpStatus.MULTI_STATUS)
            .contentType(MediaType.APPLICATION_XML)
            .body(sb.toString())
    }

    fun getContact(userDO: PFUserDO, contactId: String): ResponseEntity<String> {
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
