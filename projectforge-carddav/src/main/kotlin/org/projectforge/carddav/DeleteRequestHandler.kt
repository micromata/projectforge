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

import mu.KotlinLogging
import org.projectforge.carddav.service.AddressService
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
internal class DeleteRequestHandler {
    @Autowired
    private lateinit var addressService: AddressService

    /**
     * Handles uri=/carddav/users/admin/addressbooks/ProjectForge-128.vcf, method=DELETE
     * @param writerContext The writer context.
     */
    fun handleDeleteCall(writerContext: WriterContext) {
        val requestWrapper = writerContext.requestWrapper
        val response = writerContext.response
        val contactList = writerContext.contactList ?: emptyList()
        log.debug { "handleDeleteCall:  ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val requestedPath = requestWrapper.requestURI
        val contactId = CardDavUtils.extractContactId(requestedPath)
        if (contactId == null) {
            log.info { "Contact with id=$contactId not found in personal contact list. Can't delete it." }
            ResponseUtils.setValues(response, content = "The resource does not exist.", status = HttpStatus.NOT_FOUND)
            return
        }
        if (addressService.deleteContact(contactId)) {
            ResponseUtils.setValues(response, HttpStatus.NO_CONTENT)
        } else {
            ResponseUtils.setValues(response, content = "The resource does not exist.", status = HttpStatus.NOT_FOUND)
        }
        log.debug { "handleGetCall: response.content=[]" }
    }
}
