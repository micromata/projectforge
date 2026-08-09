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

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.address.AddressDao
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.login.LoginProtection
import org.projectforge.rest.config.Rest
import org.projectforge.security.ConstantTimeCompare
import org.projectforge.security.SecurityLogging
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import jakarta.servlet.http.HttpServletRequest


/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping(Rest.PUBLIC_URL)
class PhoneLookupRest {
    private val log = org.slf4j.LoggerFactory.getLogger(PhoneLookupRest::class.java)

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var configurationService: ConfigurationService

    /**
     * This servlet may be used by e. g. Asterix scripts for displaying incoming phone callers.
     *
     * @return The first address, if found any, matching the given phone number.
     */
    @GetMapping("phoneLookup")
    fun phoneLookup(req: HttpServletRequest, @RequestParam("nr") number: String?, @RequestParam("key") key: String?): ResponseEntity<Any> {
        val clientIp = WebUtils.getClientIp(req)
        log.info("From: $clientIp, request-URL: ${req.requestURL}")
        val expectedKey = configurationService.phoneLookupKey
        if (expectedKey.isNullOrBlank()) {
            log.warn("Servlet call for receiving phone lookups ignored because phoneLookupKey isn't configured in file projectforge.properties (projectforge.phoneLookupKey).")
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }
        val loginProtection = LoginProtection.instance()
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(clientIp, null, AUTHENTICATION_TYPE)
        if (offset > 0) {
            SecurityLogging.logSecurityWarn(
                req, this::class.java, "PHONE LOOKUP ACCESS DENIED",
                "Access denied for ${offset / 1000} seconds due to failed attempts with a wrong key."
            )
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }
        if (!ConstantTimeCompare.equals(expectedKey, key)) {
            loginProtection.incrementFailedLoginTimeOffset(clientIp, null, AUTHENTICATION_TYPE)
            SecurityLogging.logSecurityWarn(
                req, this::class.java, "PHONE LOOKUP UNAUTHORIZED",
                "Given key doesn't match the configured one (projectforge.phoneLookupKey)."
            )
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }
        loginProtection.clearLoginTimeOffset(clientIp, null, null, AUTHENTICATION_TYPE)
        if (StringUtils.isBlank(number) || !StringUtils.containsOnly(number, "+1234567890 -/")) {
            log.warn("Bad request, request parameter nr not given or contains invalid characters (only +0123456789 -/ are allowed): $number")
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        val result = addressDao.internalPhoneLookUp(number!!)
        if (result != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body(result)
        } else {
            // 0 - unknown, no entry found.
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("")
        }
    }

    companion object {
        /**
         * Own namespace of [LoginProtection], so that failed attempts here neither lock out a user's login nor are
         * lifted by one.
         */
        private const val AUTHENTICATION_TYPE = "PHONE_LOOKUP"
    }
}
