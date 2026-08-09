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

package org.projectforge.rest.pub

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.login.LoginProtection
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.utils.NumberHelper.extractPhonenumber
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.rest.config.Rest
import org.projectforge.security.ConstantTimeCompare
import org.projectforge.security.SecurityLogging
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

/**
 * This rest service is available without login credentials but with an access key and only if SMS functionality
 * is configured as well as authentication key.
 *
 * There is no user filter registered for [Rest.SMS_BASE_URI], so the access key is the only barrier: it is
 * therefore compared in constant time and failed attempts are throttled by [LoginProtection] (own namespace, keyed
 * by the ip address of the client).
 */
@RestController
@RequestMapping(Rest.SMS_BASE_URI)
class MessagingServiceRest {
    class PostData(val phoneNumber: String, val text: String, val authKey: String, val verboseLog: Boolean?)

    @Autowired
    private lateinit var messagingServiceConfig: MessagingServiceConfig

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    @GetMapping("send")
    fun send(request: HttpServletRequest,
             @RequestParam("phoneNumber") phoneNumber: String,
             @RequestParam("text") text: String,
             @RequestParam("authKey") authKey: String,
             @RequestParam("verboseLog") verboseLog: Boolean?)
            : ResponseEntity<String> {
        if (!smsSenderConfig.isSmsConfigured() || messagingServiceConfig.authkey.isNullOrBlank()) {
            log.warn { "SMS service not available (not configured). Rejecting rest call." }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Service not available.")
        }
        val clientIp = WebUtils.getClientIp(request)
        val loginProtection = LoginProtection.instance()
        // The ip address is passed as the user key (and not as clientIpAddress) on purpose: only the user key
        // honours AUTHENTICATION_TYPE as a namespace, and the ip map has a threshold of 1000 failed attempts.
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(clientIp, null, AUTHENTICATION_TYPE)
        if (offset > 0) {
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "SMS SERVICE ACCESS DENIED",
                "Access denied for ${offset / 1000} seconds due to failed attempts with a wrong access key."
            )
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Service not available.")
        }
        if (!ConstantTimeCompare.equals(messagingServiceConfig.authkey, authKey)) {
            loginProtection.incrementFailedLoginTimeOffset(clientIp, null, AUTHENTICATION_TYPE)
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "SMS SERVICE UNAUTHORIZED", "Wrong access key used."
            )
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Service not available.") // Return same state (for less information for potential hackers.
        }
        loginProtection.clearLoginTimeOffset(clientIp, null, null, AUTHENTICATION_TYPE)
        val number = extractPhonenumber(phoneNumber,
                Configuration.instance.getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX))

        val result =
                try {
                    SmsSender(smsSenderConfig).send(number, text)
                } catch (ex: Exception) {
                    log.error("Error while trying to send sms message: ${ex.message}", ex)
                    HttpResponseCode.UNKNOWN_ERROR
                }
        return when (result) {
            HttpResponseCode.SUCCESS -> {
                if (verboseLog == true) {
                    log.info { "Sent sms successfully to $number: $text" }
                }
                ResponseEntity.ok()
                        .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
                        .body("OK")
            }
            HttpResponseCode.MESSAGE_ERROR -> {
                ResponseEntity.badRequest()
                        .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
                        .body("message error")
            }
            HttpResponseCode.NUMBER_ERROR -> {
                ResponseEntity.badRequest()
                        .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
                        .body("number error")
            }
            HttpResponseCode.MESSAGE_TO_LARGE -> {
                ResponseEntity.badRequest()
                        .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
                        .body("message to large")
            }
            else -> {
                ResponseEntity.badRequest()
                        .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
                        .body("unknown error")
            }
        }
    }

    @PostMapping("post")
    fun post(request: HttpServletRequest, @RequestBody postData: PostData)
            : ResponseEntity<String> {
        return send(request, postData.phoneNumber, postData.text, postData.authKey, postData.verboseLog)
    }

    companion object {
        /**
         * Own namespace of [LoginProtection], so that failed attempts here neither lock out a user's login nor are
         * lifted by one.
         */
        private const val AUTHENTICATION_TYPE = "SMS_SERVICE"
    }
}
