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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.carddav.model.Contact
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
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class CardDavService {
    @Autowired
    private lateinit var addressService: AddressService

    @Autowired
    private lateinit var deleteRequestHandler: DeleteRequestHandler

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var getRequestHandler: GetRequestHandler

    @Autowired
    private lateinit var restAuthenticationUtils: RestAuthenticationUtils

    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    @PostConstruct
    fun init() {
        domain = domainService.domainWithContextPath
    }

    fun dispatch(request: HttpServletRequest, response: HttpServletResponse) {
        val requestWrapper = RequestWrapper(request)
        log.debug { "****************** ${request.method}:${request.requestURI}, auth=${requestWrapper.basicAuth}, body=[${requestWrapper.body}]" }
        log.debug { "Request-Info: ${RequestLog.asJson(request, true)}" }
        if (request.method == "OPTIONS") {
            // No login required for OPTIONS.
            OptionsRequestHandler.handleDynamicOptions(requestWrapper, response)
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

    private fun dispathAuthenticated(requestWrapper: RequestWrapper, response: HttpServletResponse, userDO: PFUserDO) {
        log.debug { "Dispatching authenticated request: ${RequestLog.asString(requestWrapper.request)}" }
        val request = requestWrapper.request
        val method = request.method
        // Runs under /carddav as well as under /
        // Normalize URI for further processing:
        val normalizedRequestURI = CardDavUtils.normalizedUri(request.requestURI)
        val writerContext = WriterContext(requestWrapper, response, userDO)
        if (method == "PROPFIND") {
            if (normalizedRequestURI == "index.html") {
                // PROPFIND call to /index.html after authentication is a typical behavior of many WebDAV or CardDAV clients.
                // Alternatives: Not found (404) or Forbidden (403)
                ResponseUtils.setValues(response, HttpStatus.MULTI_STATUS)
                return
            }
            writerContext.props = CardDavUtils.handleProps(requestWrapper, response)
            writerContext.contactList = getContactList(userDO)
            if (normalizedRequestURI.startsWith("principals")) {
                PropFindRequestHandler.handlePropFindPrincipalsCall(writerContext)
            } else {
                PropFindRequestHandler.handlePropFindCall(writerContext)
            }
        } else if (method == "REPORT") {
            // /carddav/users/admin/joe/addressbooks
            writerContext.props = CardDavUtils.handleProps(requestWrapper, response)
            writerContext.contactList = getContactList(userDO)
            ReportRequestHandler.handleSyncReportCall(writerContext)
            /*if (normalizedRequestURI.startsWith("users/")) {
                val contactId = normalizedRequestURI.removePrefix("users/").removeSuffix(".vcf")
                getContact(user, contactId)
            }*/
        } else if (method == "GET") {
            // /carddav/users/admin/addressbooks/ProjectForge-129.vcf
            writerContext.contactList = getContactList(userDO)
            getRequestHandler.handleGetCall(writerContext)
        } else if (method == "DELETE") {
            // /carddav/users/admin/addressbooks/ProjectForge-129.vcf
            deleteRequestHandler.handleDeleteCall(writerContext)
        } else {
            log.warn { "Method not supported: $method" }
            ResponseUtils.setValues(
                response, HttpStatus.METHOD_NOT_ALLOWED, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "Method not supported: $method"
            )
        }
    }

    private fun getContactList(userDO: PFUserDO): List<Contact> {
        return addressService.getContactList(userDO)
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

    companion object {
        internal var domain: String = "localhost"
    }
}
