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

package org.projectforge.ui

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.AbstractEntityRest.CloneSupport
import org.projectforge.rest.dto.Address
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * A server laid out page of the addresses for [UILayoutTest] only: no address page of projectforge-rest serves a
 * `UILayout` any more, but the layout framework is still in use (plugin merlin). The address fits,
 * because its DAO supports force deletion and the page declares [CloneSupport], so every action button appears.
 */
@RestController
@RequestMapping("${Rest.URL}/uiLayoutTestAddress")
class UILayoutTestAddressPagesRest : AbstractDTOPagesRest<AddressDO, Address, AddressDao>(
    AddressDao::class.java,
    i18nKeyPrefix = "address.title",
    cloneSupport = CloneSupport.CLONE,
) {
    override fun transformForDB(dto: Address): AddressDO {
        val addressDO = AddressDO()
        dto.copyTo(addressDO)
        return addressDO
    }

    override fun transformFromDB(obj: AddressDO, editMode: Boolean): Address {
        val address = Address()
        address.copyFrom(obj)
        return address
    }

    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess,
    ) {
        agGridSupport.prepareUIGrid4ListPage(request, layout, magicFilter, this, userAccess = userAccess)
            .add(lc, "name", "firstName", "organization", "email")
    }

    override fun createEditLayout(dto: Address, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(lc, "addressStatus", "form", "name", "firstName")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
