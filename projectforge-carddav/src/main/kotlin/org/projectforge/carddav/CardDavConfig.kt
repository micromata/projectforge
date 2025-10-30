/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import mu.KotlinLogging
import org.projectforge.business.address.vcard.VCardVersion
import org.projectforge.business.configuration.DomainService
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.CardDAVInfoPageRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

/**
 * Photo mode for VCard generation.
 */
enum class PhotoMode {
    /**
     * Use URL references for photos (e.g., PHOTO;VALUE=uri:/carddav/photos/contact-123.jpg).
     * Advantages: Smaller VCards, faster sync
     * Disadvantages: May have line-folding issues with long URLs in VCard 3.0
     */
    URL,

    /**
     * Use embedded Base64-encoded photos (e.g., PHOTO;ENCODING=b;TYPE=jpeg:...base64...).
     * Advantages: No line-folding issues, works offline
     * Disadvantages: Larger VCards, slower sync
     */
    EMBEDDED;

    companion object {
        fun from(value: String?): PhotoMode {
            return when (value?.lowercase()) {
                "embedded" -> EMBEDDED
                "url" -> URL
                else -> {
                    if (value != null && value.isNotBlank()) {
                        log.warn { "Unknown photo mode '$value', defaulting to URL mode" }
                    }
                    URL
                }
            }
        }
    }
}

@Configuration
open class CardDavConfig {
    @Autowired
    private lateinit var domainService: DomainService

    @Value("\${projectforge.carddav.server.enable:true}")
    var enable: Boolean = true
        private set

    /**
     * If set, the user with this name is used for debugging by writing requests and responses to log files.
     * @see CardDavServerDebugWriter
     */
    @Value("\${projectforge.carddav.server.debugUser:}")
    internal var debugUser: String = ""
        private set

    /**
     * Supported: "3.0" or "4.0".
     */
    @Value("\${projectforge.carddav.server.vcardVersion:3.0}")
    private var vcardVersionNumber: String = "3.0"

    /**
     * Photo mode for VCard generation: "url" or "embedded".
     * - url: Use URL references (smaller VCards, may have line-folding issues)
     * - embedded: Use Base64-encoded images (larger VCards, no line-folding issues)
     */
    @Value("\${projectforge.carddav.server.photoMode:embedded}")
    private var photoModeString: String = "embedded"

    @PostConstruct
    private fun postConstruct() {
        CardDavServerDebugWriter.debugUser = debugUser
        AddressPagesRest.carddavServerEnabled = enable
        CardDAVInfoPageRest.standardUrl = "${domainService.domain}${CardDavInit.CARD_DAV_BASE_PATH}"
        CardDAVInfoPageRest.appleUrl = domainService.plainDomain
        CardDAVInfoPageRest.applePath = "${CardDavInit.CARD_DAV_BASE_PATH}/"
        CardDAVInfoPageRest.iOSUrl = "${domainService.plainDomain}${CardDavInit.CARD_DAV_BASE_PATH}"
        log.info { "CardDAV server configured: enable=$enable, vcardVersion=$vcardVersionNumber, photoMode=${photoMode.name}" }
    }

    val vcardVersion: VCardVersion
        get() = VCardVersion.from(vcardVersionNumber)

    val photoMode: PhotoMode
        get() = PhotoMode.from(photoModeString)
}
