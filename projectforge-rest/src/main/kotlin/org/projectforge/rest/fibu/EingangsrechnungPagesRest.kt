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
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Eingangsrechnung
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/incomingInvoice")
class EingangsrechnungPagesRest : AbstractDTOPagesRest<EingangsrechnungDO, Eingangsrechnung, EingangsrechnungDao>(
    EingangsrechnungDao::class.java,
    "fibu.eingangsrechnung.title"
) {

    /**
     * ########################################
     * # Force usage only for selection mode: #
     * #                                      #
     * # If used in normal mode, please note: #
     * # SEPA-export of NON-EUR amounts!!!!!  #
     * ########################################
     */
    override fun getInitialList(request: HttpServletRequest): InitialListData {
        MultiSelectionSupport.ensureMultiSelectionOnly(request, this, "/wa/incomingInvoiceList")
        return super.getInitialList(request)
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        val infoLC = LayoutContext(RechnungInfo::class.java)
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            EingangsrechnungMultiSelectedPageRest::class.java,
            userAccess,
        )
            .add(lc, "kreditor", "referenz", "betreff", "konto", "datum")
            .add(
                lc,
                "faelligkeitOrDiscountMaturity",
                headerName = translate("fibu.rechnung.faelligkeit"),
            )
            .add(lc, "bezahlDatum")
            .add(lc, "ibanFormatted", lcField = "iban")
            .add(infoLC, "netSum", "grossSumWithDiscount")
            .add(lc, "paymentTypeAsString", lcField = "paymentType", width = 100)
            .add(lc, "bemerkung")
            .add(field = "kost1List", headerName = translate("fibu.kost1"), tooltipField = "kost1Info")
            .add(field = "kost2List", headerName = translate("fibu.kost2"), tooltipField = "kost2Info")
            .withPinnedLeft(2)
            .withMultiRowSelection(request, magicFilter)
            .withGetRowClass(
                """if (params.node.data.ueberfaellig) {
            return 'ag-row-red';
        } else if (!params.node.data.bezahlDatum) {
            return 'ag-row-blue';
        }"""
            )
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Eingangsrechnung, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(lc, "betreff")
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "kreditor", "customernr", "referenz", "konto")
                    )
                    .add(
                        UICol()
                            .add(lc, "datum", "vatAmountSum", "bezahlDatum", "faelligkeit")
                    )
                    .add(
                        UICol()
                            .add(lc, "netSum", "grossSum", "zahlBetrag", "discountPercent")
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "paymentType", "receiver", "iban", "bic")
                    )
                    .add(
                        UICol()
                            .add(lc, "bemerkung")
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "besonderheiten")
                    )
            )
            // Positionen
            .add(UICustomized("invoice.incomingPosition"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    @PostMapping("addPosition")
    fun addPosition(
        request: HttpServletRequest,
        @RequestBody postData: PostData<Eingangsrechnung>
    ): ResponseEntity<ResponseAction> {
        val eingangsrechnung = EingangsrechnungDO()
        var newPosition = EingangsrechnungsPositionDO()
        postData.data.copyTo(eingangsrechnung)
        eingangsrechnung.addPosition(newPosition)
        postData.data.copyFrom(eingangsrechnung)
        return org.projectforge.rest.core.saveOrUpdate(
            request,
            this.baseDao,
            eingangsrechnung,
            postData,
            this,
            this.validate(eingangsrechnung)
        )
    }

    override fun transformForDB(dto: Eingangsrechnung): EingangsrechnungDO {
        val eingangsrechnungDO = EingangsrechnungDO()
        dto.copyTo(eingangsrechnungDO)
        return eingangsrechnungDO
    }

    override fun transformFromDB(obj: EingangsrechnungDO, editMode: Boolean): Eingangsrechnung {
        val eingangsrechnung = Eingangsrechnung()
        eingangsrechnung.copyFrom(obj)
        if (editMode) {
            eingangsrechnung.copyPositionenFrom(obj)
        }
        val kost1Sorted = obj.info.sortedKost1
        eingangsrechnung.kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        eingangsrechnung.kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = obj.info.sortedKost2
        eingangsrechnung.kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        eingangsrechnung.kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
        return eingangsrechnung
    }
}
