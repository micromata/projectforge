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

package org.projectforge.rest.address.importer

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDO
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportStorage
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIAgGrid
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/importAddress")
class AddressImportPageRest : AbstractImportPageRest<AddressImportDTO>() {

    @Autowired
    private lateinit var jobHandler: JobHandler

    override val title: String = "address.import.title"

    override fun callerPage(request: HttpServletRequest): String {
        return PagesResolver.getListPageUrl(AddressPagesRest::class.java, absolute = true)
    }

    override fun clearImportStorage(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(
            request,
            getSessionAttributeName(AddressImportPageRest::class.java),
        )
    }

    override fun getImportStorage(request: HttpServletRequest): AddressImportStorage? {
        return ExpiringSessionAttributes.getAttribute(
            request,
            getSessionAttributeName(AddressImportPageRest::class.java),
            AddressImportStorage::class.java,
        )
    }

    override fun import(
        importStorage: ImportStorage<*>,
        selectedEntries: List<ImportPairEntry<AddressImportDTO>>
    ): Int {
        return jobHandler.addJob(
            AddressImportJob(
                selectedEntries,
                importStorage = importStorage as AddressImportStorage,
            )
        ).id
    }

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val importStorage = getImportStorage(request)
        return createFormLayoutData(request, importStorage)
    }

    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        agGrid: UIAgGrid,
    ) {
        val lc = LayoutContext(AddressDO::class.java)

        // Name
        addReadColumn(agGrid, lc, AddressDO::name, width = 150)

        // First Name
        addReadColumn(agGrid, lc, AddressDO::firstName, width = 150)

        // Organization
        addReadColumn(agGrid, lc, AddressDO::organization, width = 200, wrapText = true)

        // Business Email
        addReadColumn(agGrid, lc, AddressDO::email, width = 200)

        // Business Phone
        addReadColumn(agGrid, lc, AddressDO::businessPhone, width = 150)

        // Mobile Phone
        addReadColumn(agGrid, lc, AddressDO::mobilePhone, width = 150)

        // City (Business)
        addReadColumn(agGrid, lc, AddressDO::city, width = 150)

        // Private Email
        addReadColumn(agGrid, lc, AddressDO::privateEmail, width = 200)

        // Private Phone
        addReadColumn(agGrid, lc, AddressDO::privatePhone, width = 150)

        // Private City
        addReadColumn(agGrid, lc, AddressDO::privateCity, width = 150)

        // Website
        addReadColumn(agGrid, lc, AddressDO::website, width = 200)

        // Comment
        addReadColumn(agGrid, lc, AddressDO::comment, width = 200, wrapText = true)
    }
}
