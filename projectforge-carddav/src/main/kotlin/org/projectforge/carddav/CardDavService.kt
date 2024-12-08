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
import org.projectforge.rest.utils.ResponseUtils
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
        val requestWrapper = RequestWrapper(request)
        log.debug { "****************** ${request.method}:${request.requestURI}, auth=${requestWrapper.basicAuth}, body=[${requestWrapper.body}]" }
        log.debug { "Request-Info: ${RequestLog.asJson(request, true)}" }
        if (request.method == "OPTIONS") {
            // No login required for OPTIONS.
            handleDynamicOptions(requestWrapper, response)
            return
        }
        if (request.method == "PROPFIND" && request.requestURI.startsWith("/.well-known/carddav")) {
            // Sometimes the clients tries to find the carddav service. Here it is:
            log.debug { "PROPFIND: ${request.requestURI} -> moved permanently]" }
            ResponseUtils.setValues(response, HttpStatus.MOVED_PERMANENTLY)
            response.addHeader("Location", "/carddav")
            return
        }
        val authInfo = authenticate(request, response)
        val user = authInfo.user
        if (user == null) {
            log.error { "Authentication failed: ${RequestLog.asString(request)}" }
            ResponseUtils.setValues(
                response, HttpStatus.UNAUTHORIZED, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "Authentication is required to access this resource.",
            )
            return
        }
        try {
            // Register user in thread local for further usage:
            restAuthenticationUtils.registerUser(request, authInfo, UserTokenType.DAV_TOKEN)
            dispathAuthenticated(requestWrapper, response, user)
        } finally {
            restAuthenticationUtils.unregister(request, response, authInfo)
        }
    }

    private fun dispathAuthenticated(requestWrapper: RequestWrapper, response: HttpServletResponse, user: PFUserDO) {
        val request = requestWrapper.request
        val method = request.method
        // Runs under /carddav as well as under /
        // Normalize URI for further processing:
        val normalizedRequestURI = request.requestURI.removePrefix("/carddav").removePrefix("/").removeSuffix("/")
        if (method == "PROPFIND") {
            if (normalizedRequestURI == "index.html") {
                // PROPFIND call to /index.html after authentication is a typical behavior of many WebDAV or CardDAV clients.
                // Alternatives: Not found (404) or Forbidden (403)
                ResponseUtils.setValues(response, HttpStatus.MULTI_STATUS)
                return
            } else {
                PropFindUtils.handlePropFindCall(requestWrapper, response, user)
            }
        } else if (method == "REPORT") {
            PropFindUtils.handleSyncReportCall(requestWrapper, response, user)
            /*if (normalizedRequestURI.startsWith("users/")) {
                val contactId = normalizedRequestURI.removePrefix("users/").removeSuffix(".vcf")
                getContact(user, contactId)
            }*/
        }
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
    private fun handleDynamicOptions(requestWrapper: RequestWrapper, response: HttpServletResponse) {
        val requestedPath = requestWrapper.requestURI
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
        ResponseUtils.setValues(response, status = HttpStatus.OK)
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
        CardDavXmlWriter.appendMultiStatusStart(sb)
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
}
