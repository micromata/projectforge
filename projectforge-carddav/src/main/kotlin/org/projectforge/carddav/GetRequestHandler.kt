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

import mu.KotlinLogging
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.ImageType
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

@Service
internal class GetRequestHandler {
    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    /**
     * Handles /carddav/users/admin/addressbooks/ProjectForge-129.vcf, method=GET
     * @param writerContext The writer context.
     */
    fun handleGetCall(writerContext: WriterContext) {
        val requestWrapper = writerContext.requestWrapper
        val response = writerContext.response
        val contactList = writerContext.contactList ?: emptyList()
        log.debug { "handleGetCall:  ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val requestedPath = requestWrapper.requestURI
        if (requestedPath == "/.well-known/carddav") {
            handleWellKnownGetCall(writerContext)
            return
        }
        val contactId = CardDavUtils.extractContactId(requestedPath)
        if (contactId != null && CardDavUtils.isImageUrl(requestedPath)) {
            handleImageGetCall(writerContext, contactId)
            return
        }
        val contact = contactList.find { it.id == contactId }
        val vcardData = contact?.vcardData
        if (vcardData == null) {
            ResponseUtils.setValues(response, status = HttpStatus.NOT_FOUND)
            return
        }
        val content = vcardData
        // For vCard data, use text/vcard (for version 3.0 or earlier) or text/vcard;charset=utf-8 for newer versions like 4.0.
        // A unique identifier for this version of the resource. This allows clients to detect changes efficiently.
        response.addHeader("ETag", contact.etag)
        response.addHeader("Last-Modified", contact.lastModifiedAsHttpDate)
        ResponseUtils.setValues(response, HttpStatus.OK, contentType = "text/vcard", content = content)
        CardDavServerDebugWriter.writeRequestResponseLogInTestMode(requestWrapper, response, content)
        log.debug {
            "handleGetCall: response=${ResponseUtils.asJson(response)}, content=[${
                CardDavServerDebugWriter.sanitizeContent(
                    content
                )
            }]"
        }
    }

    private fun handleWellKnownGetCall(writerContext: WriterContext) {
        log.debug { "handleWellKnownGetCall:  ${writerContext.requestWrapper.request.method}: '${writerContext.requestWrapper.requestURI}'" }
        // HTTP/1.1 301 Moved Permanently
        // Location: https://example.com/carddav/
        val response = writerContext.response
        ResponseUtils.setValues(response, status = HttpStatus.MOVED_PERMANENTLY)  // HTTP 301
        response.setHeader("Location", CardDavUtils.getBaseUrl())
        CardDavServerDebugWriter.writeRequestResponseLogInTestMode(
            writerContext.requestWrapper,
            response,
            "<empty-content> Location=${CardDavUtils.getBaseUrl()}"
        )
    }

    /**
     * Request:
     * ```
     * GET /carddav/photos/contact-123.jpg HTTP/1.1
     * Host: projectforge.acme.com
     * Accept: image/*, */*
     * User-Agent: [Client-specific, e.g. "iOS/17.2 (iPhone) CardDAVClient"]
     * Accept-Encoding: gzip, deflate, br
     * If-None-Match: "abc12345"
     * If-Modified-Since: Wed, 20 Dec 2024 10:00:00 GMT
     * Connection: keep-alive
     * ```
     * If-None-Match: "abc12345"
     * 	* Conditional fetch based on the image's ETag value. The client sends this header if it already has the image in the cache.
     * 	* If the image is unchanged, the server responds with 304 Not Modified without retransmitting the image content.
     * 	If-Modified-Since: Wed, 20 Dec 2024 10:00:00 GMT
     * 	* Conditional fetch based on the last modified date. The client sends this date if it has the image in the cache.
     * 	* The server responds with 304 Not Modified if the image has not been changed since that date.
     * 	@param writerContext The writer context.
     * 	@param contactId The contact ID.
     */
    private fun handleImageGetCall(writerContext: WriterContext, contactId: Long) {
        log.debug { "handleImageGetCall:  ${writerContext.requestWrapper.request.method}: '${writerContext.requestWrapper.requestURI}'" }
        val imageDO = addressImageDao.findImage(contactId, fetchImage = true)
        val image = imageDO?.image
        val imageType = imageDO?.imageType ?: ImageType.PNG
        if (image == null) {
            ResponseUtils.setValues(writerContext.response, status = HttpStatus.NOT_FOUND)
            return
        }
        val imageLastUpdate = imageDO.lastUpdate ?: Date(0L)
        val request = writerContext.requestWrapper.request
        val ifNoneMatch = request.getHeader("If-None-Match")
        val ifModifiedSinceAttr = request.getHeader("If-Modified-Since")
        val ifModifiedSince = if (ifModifiedSinceAttr != null) {
            PFDateTimeUtils.parseAndCreateDateTime(ifModifiedSinceAttr)?.utilDate
        } else {
            null
        }
        val eTag = CardDavUtils.getEtag(imageLastUpdate)
        if (ifNoneMatch == eTag || (ifModifiedSince != null && !imageLastUpdate.after(ifModifiedSince))) {
            log.debug { "handleImageGetCall: 304 Not Modified, because image's etag is same as given etag by client." }
            ResponseUtils.setValues(writerContext.response, status = HttpStatus.NOT_MODIFIED)
            return
        }
        ResponseUtils.setByteArrayContent(
            writerContext.response,
            status = HttpStatus.OK,
            contentType = imageType.mimeType,
            content = image
        )
        writerContext.response.let { response ->
            response.addHeader("Cache-Control", "public, max-age=86400") // 1 day
            response.addHeader("Last-Modified", PFDateTime.from(imageLastUpdate).formatAsHttpDate())
            response.addHeader("ETag", eTag)
        }
        val requestWrapper = writerContext.requestWrapper
        val response = writerContext.response
        CardDavServerDebugWriter.writeRequestResponseLogInTestMode(requestWrapper, response, "<imagedata>")
    }
}
