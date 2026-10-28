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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Addressbook
import org.projectforge.rest.dto.User
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the address books (menu "Addressbooks"), serving the hand-built list and edit
 * page of projectforge-next (see components/features/addressBook). Replaces the React UILayout `AddressBookPagesRest`.
 *
 * The autocompletion of the address books (`addressBook/ac`, used by the React address form) is served by
 * [org.projectforge.rest.core.AbstractEntityRest] through [autoCompleteSearchFields].
 */
@RestController
@RequestMapping("${Rest.URL}/addressBook")
class AddressbookEntityRest :
    AbstractDTOEntityRest<AddressbookDO, Addressbook, AddressbookDao>(AddressbookDao::class.java, "addressbook.title") {

    override fun transformFromDB(obj: AddressbookDO, editMode: Boolean): Addressbook {
        return Addressbook.transformFromDB(obj)
    }

    override fun transformForDB(dto: Addressbook): AddressbookDO {
        val addressbookDO = AddressbookDO()
        dto.copyTo(addressbookDO)
        return addressbookDO
    }

    override fun newBaseDTO(request: HttpServletRequest?): Addressbook {
        val addressbook = Addressbook()
        addressbook.owner = User.getUser(ThreadLocalUserContext.loggedInUserId)
        return addressbook
    }

    override val autoCompleteSearchFields = arrayOf("title", "description")

    /** Name hits first, see [autoCompletePrimarySearchFields]. */
    override val autoCompletePrimarySearchFields = arrayOf("title")
}
