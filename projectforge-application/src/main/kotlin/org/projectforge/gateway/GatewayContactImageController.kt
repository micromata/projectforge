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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.ContactImageUrl
import org.projectforge.business.address.ImageType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * Delivers the contact image of an address under a signed url (see [ContactImageUrl]), e.g. as the photo of a digital
 * business card. The gateway knows nothing about business cards: it checks the signature and answers with the image
 * the address push brought here. Everything else (bad signature, unknown or deleted address, no image) is a plain 404
 * without a hint.
 */
@RestController
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewayContactImageController(
    private val contactImageUrl: ContactImageUrl,
    private val addressDao: AddressDao,
    private val addressImageDao: AddressImageDao,
) {
    @GetMapping("${ContactImageUrl.PATH}{uid}/{signature}")
    fun getImage(@PathVariable uid: String, @PathVariable signature: String): ResponseEntity<ByteArray> {
        val addressUid = ContactImageUrl.decodeUid(uid)
        if (addressUid == null || !contactImageUrl.verify(addressUid, signature)) {
            return ResponseEntity.notFound().build()
        }
        val address = addressDao.findByUid(addressUid)
        val addressId = address?.id
        if (address == null || addressId == null || address.deleted) {
            return ResponseEntity.notFound().build()
        }
        val imageDO = addressImageDao.findImage(addressId, fetchImage = true, checkAccess = false)
        val image = imageDO?.image
        if (image == null) {
            log.debug { "Contact image of address #$addressId requested, but there is none." }
            return ResponseEntity.notFound().build()
        }
        val imageType = imageDO.imageType ?: ImageType.PNG
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageType.mimeType))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(image)
    }
}
