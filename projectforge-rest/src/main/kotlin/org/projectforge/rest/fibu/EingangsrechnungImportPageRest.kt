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

package org.projectforge.rest.fibu

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.fibu.importer.EingangsrechnungImportJob
import org.projectforge.rest.fibu.importer.EingangsrechnungImportStorage
import org.projectforge.rest.fibu.importer.EingangsrechnungPosImportDTO
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportStorage
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/importIncomingInvoices")
class EingangsrechnungImportPageRest : AbstractImportPageRest<EingangsrechnungPosImportDTO>() {

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var jobHandler: JobHandler

    override val title: String = "fibu.eingangsrechnung.import.title"

    override fun callerPage(request: HttpServletRequest): String {
        return PagesResolver.getListPageUrl(EingangsrechnungPagesRest::class.java, absolute = true)
    }

    override fun clearImportStorage(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(
            request,
            getSessionAttributeName(EingangsrechnungImportPageRest::class.java),
        )
    }

    override fun getImportStorage(request: HttpServletRequest): EingangsrechnungImportStorage? {
        return ExpiringSessionAttributes.getAttribute(
            request,
            getSessionAttributeName(EingangsrechnungImportPageRest::class.java),
            EingangsrechnungImportStorage::class.java,
        )
    }

    override fun import(
        importStorage: ImportStorage<*>,
        selectedEntries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>
    ): Int? {
        return jobHandler.addJob(
            EingangsrechnungImportJob(
                eingangsrechnungDao,
                selectedEntries,
                importStorage = importStorage as EingangsrechnungImportStorage,
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
        val lc = LayoutContext(EingangsrechnungDO::class.java)
        addReadColumn(agGrid, lc, EingangsrechnungDO::datum)
        addReadColumn(agGrid, lc, EingangsrechnungDO::kreditor, wrapText = true)
        addReadColumn(agGrid, lc, EingangsrechnungDO::referenz, wrapText = true)
        addReadColumn(agGrid, lc, EingangsrechnungDO::betreff, wrapText = true)

        // Custom columns for amounts
        agGrid.add(UIAgGridColumnDef.createCol("netSum", headerName = translate("fibu.rechnung.netSum")))
        agGrid.add(UIAgGridColumnDef.createCol("vatAmountSum", headerName = translate("fibu.rechnung.vatAmountSum")))
        agGrid.add(UIAgGridColumnDef.createCol("grossSum", headerName = translate("fibu.rechnung.grossSum")))

        addReadColumn(agGrid, lc, EingangsrechnungDO::bezahlDatum)
        addReadColumn(agGrid, lc, EingangsrechnungDO::faelligkeit)
        addReadColumn(agGrid, lc, EingangsrechnungDO::iban)
        addReadColumn(agGrid, lc, EingangsrechnungDO::bic)
        addReadColumn(agGrid, lc, EingangsrechnungDO::receiver, wrapText = true)
        addReadColumn(agGrid, lc, EingangsrechnungDO::paymentType)
        addReadColumn(agGrid, lc, EingangsrechnungDO::customernr)
    }

    override fun createImportDropArea(layout: UILayout): Boolean {
        layout.add(UIAlert("fibu.eingangsrechnung.import.description", markdown = true, color = UIColor.INFO))

        // Drop Area for file upload
        layout.add(
            UIDropArea(
                "fibu.eingangsrechnung.import.dropArea",
                uploadUrl = RestResolver.getRestUrl(EingangsrechnungImportPageRest::class.java, "upload")
            )
        )

        // CSV Template download
        layout.add(
            UIAlert("fibu.eingangsrechnung.import.templateInfo", markdown = true, color = UIColor.SECONDARY)
        )

        layout.addAction(
            UIButton.createDownloadButton(
                title = "fibu.eingangsrechnung.import.downloadTemplate",
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(this.javaClass, "template"),
                    targetType = TargetType.DOWNLOAD
                )
            )
        )
        return true
    }
}
