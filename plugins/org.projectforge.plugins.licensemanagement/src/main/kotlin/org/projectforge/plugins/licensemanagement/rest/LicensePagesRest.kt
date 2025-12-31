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

package org.projectforge.plugins.licensemanagement.rest

import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.plugins.licensemanagement.LicenseDO
import org.projectforge.plugins.licensemanagement.LicenseDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/license")
class LicensePagesRest : AbstractDOPagesRest<LicenseDO, LicenseDao>(LicenseDao::class.java, "plugins.licensemanagement.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(lc, "organization", "product", "version", "numberOfLicenses", "ownerIds", "device", "key",
                                "comment"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: LicenseDO, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                            .add(lc, "organization", "product", "version", "updateFromVersion", "device",
                                    "numberOfLicenses")))
                .add(UILabel("TODO: Owner selection"))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "validSince"))
                        .add(UICol()
                                .add(lc, "validUntil")))
                .add(lc, "licenseHolder", "key")
                .add(UIRow()
                        .add(UICol()
                                .add(UILabel("TODO: File Selector")))
                        .add(UICol()
                                .add(UILabel("TODO: File Selector"))))
                .add(lc, "comment")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
