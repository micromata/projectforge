/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.utils.NumberHelper.extractPhonenumber
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.rest.config.Rest
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

/**
 * This rest service is available without login credentials but with an access key and only if SMS functionality
 * is configured as well as authentication key..
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
    fun send(@RequestParam("phoneNumber") phoneNumber: String,
             @RequestParam("text") text: String,
             @RequestParam("authKey") authKey: String,
             @RequestParam("verboseLog") verboseLog: Boolean?)
            : ResponseEntity<String> {
        if (!smsSenderConfig.isSmsConfigured() || messagingServiceConfig.authkey.isNullOrBlank()) {
            log.warn { "SMS service not available (not configured). Rejecting rest call." }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Service not available.")
        }
        if (messagingServiceConfig.authkey != authKey) {
            log.warn { "Unautorized call of sms service (wrong key used)." }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Service not available.") // Return same state (for less information for potential hackers.
        }
        val number = extractPhonenumber(phoneNumber,
                Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX))

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
    fun post(@RequestBody postData: PostData)
            : ResponseEntity<String> {
        return send(postData.phoneNumber, postData.text, postData.authKey, postData.verboseLog)
    }
}
