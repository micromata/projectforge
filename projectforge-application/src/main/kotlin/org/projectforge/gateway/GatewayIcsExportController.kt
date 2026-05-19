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

package org.projectforge.gateway

import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.gateway.sync.GatewayIcsCache
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Gateway replacement for CalendarSubscriptionServiceRest.
 * Serves pre-generated ICS data from the cache instead of generating on-the-fly.
 */
@RestController
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewayIcsExportController(
    private val icsCache: GatewayIcsCache,
    private val userAuthenticationsService: UserAuthenticationsService,
) {

    @GetMapping("/export/ProjectForge.ics")
    fun exportCalendar(
        @RequestParam("user") userId: Long?,
        @RequestParam("q") q: String?,
    ): ResponseEntity<ByteArray> {
        if (userId == null || q.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }

        // Validate the token by attempting decryption (same as main instance)
        val decryptedParams = userAuthenticationsService.decrypt(userId, UserTokenType.CALENDAR_REST, q)
        if (decryptedParams == null) {
            log.warn { "ICS export: failed to decrypt params for user $userId" }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // Look up cached ICS data
        val icsData = icsCache.get(userId, q)
        if (icsData == null) {
            log.info { "ICS export: no cached data for user $userId (cache miss)" }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/calendar; charset=UTF-8")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=projectforge.ics")
        return ResponseEntity.ok().headers(headers).body(icsData.toByteArray(Charsets.UTF_8))
    }
}
