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

import jakarta.servlet.http.HttpServletResponse
import org.projectforge.carddav.model.Contact
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.*

internal object CardDavUtils {
    fun getVcfFileName(contact: Contact): String {
        // If you change this, don't forget to change the regex in CardDavXmlUtils.extractAddressIds.
        return "ProjectForge-${contact.id}.vcf"
    }

    fun getETag(contact: Contact): String {
        val lastUpdated = contact.lastUpdated ?: Date()
        return lastUpdated.time.toString()
    }

    /**
     * Returns the display name of the user's addressbook.
     * This is the name that is shown in the CardDAV client.
     * @param user The user.
     */
    fun getUsersAddressbookDisplayName(user: PFUserDO): String {
        return translateMsg("address.cardDAV.addressbook.displayName", user.firstname)
    }

    /**
     * Handles the PROPFIND request for the current user principal.
     * If no properties were found, a bad request response is set and null returned.
     * @param requestWrapper The request wrapper.
     * @param response The response.
     * @param user The user.
     * @return The list of properties or null if no properties were found.
     */
    fun handleProps(requestWrapper: RequestWrapper, response: HttpServletResponse): List<Prop>? {
        val props = Prop.extractProps(requestWrapper.body)
        if (props.isEmpty()) {
            ResponseUtils.setValues(
                response, HttpStatus.BAD_REQUEST, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "No properties found in ${requestWrapper.method} request."
            )
            return null
        }
        return props
    }

    fun setMultiStatusResponse(response: HttpServletResponse, content: String) {
        ResponseUtils.setValues(
            response,
            HttpStatus.MULTI_STATUS,
            contentType = MediaType.APPLICATION_XML_VALUE,
            content = content,
        )
    }

    fun normalizedUri(requestUri: String): String {
        return requestUri.removePrefix("/carddav").removePrefix("/").removeSuffix("/")
    }
}
