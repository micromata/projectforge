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
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.ContactImageUrl
import org.projectforge.business.address.ImageType
import org.projectforge.business.login.LoginProtection
import org.projectforge.security.SecurityLogging
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

/**
 * Delivers the contact image of an address under its public url (see [ContactImageUrl]), e.g. as the photo of a digital
 * business card. No login: the random token in the url is the authorization (it belongs to the image, nothing in it can
 * be derived). The same controller runs on the main instance and on the gateway.
 *
 * Everything else (unknown token, deleted address, no image) is a plain 404 without a hint. Guessing is throttled per
 * client ip: failed lookups are penalized like invalid tokens at the credentials endpoint.
 */
@RestController
open class ContactImagePublicRest {
    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @GetMapping("${ContactImageUrl.PATH}{token}")
    fun getImage(request: HttpServletRequest, @PathVariable token: String): ResponseEntity<ByteArray> {
        val clientIp = WebUtils.getClientIp(request)
        val loginProtection = LoginProtection.instance()
        // The ip address is passed as the user key on purpose, see AuthenticationPublicServicesRest.
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(clientIp, null, AUTHENTICATION_TYPE)
        if (offset > 0) {
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "CONTACT IMAGE DENIED",
                "Access denied for ${offset / 1000} seconds due to failed attempts with an invalid token."
            )
            return ResponseEntity.notFound().build()
        }
        val image = addressImageDao.findByPublicToken(token)
        val bytes = image?.image
        if (image == null || bytes == null) {
            loginProtection.incrementFailedLoginTimeOffset(clientIp, null, AUTHENTICATION_TYPE)
            return ResponseEntity.notFound().build()
        }
        loginProtection.clearLoginTimeOffset(clientIp, null, null, AUTHENTICATION_TYPE)
        val imageType = image.imageType ?: ImageType.PNG
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageType.mimeType))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(bytes)
    }

    companion object {
        private const val AUTHENTICATION_TYPE = "CONTACT_IMAGE"
    }
}
