/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/incomingInvoice")
class EingangsrechnungRest : AbstractDORest<EingangsrechnungDO, EingangsrechnungDao>(EingangsrechnungDao::class.java, "fibu.eingangsrechnung.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        // TODO:
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "kreditor", "konto", "referenz", "betreff", "datum", "faelligkeit",
                                "isBezahlt")
                        .add(UITableColumn("netSum", title = translate("fibu.common.netto"), dataType = UIDataType.DECIMAL))
                        .add(UITableColumn("grossSum", title = translate("fibu.rechnung.bruttoBetrag"), dataType = UIDataType.DECIMAL))
                        .add(lc, "bemerkung"))
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
        layout.getTableColumnById("faelligkeit").formatter = Formatter.DATE
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: EingangsrechnungDO, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(lc, "betreff")
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "kreditor", "customernr", "referenz", "konto"))
                        .add(UICol()
                                .add(lc, "datum", "vatAmountSum", "bezahlDatum", "faelligkeit"))
                        .add(UICol()
                                .add(lc, "netSum", "grossSum", "zahlBetrag", "discountPercent")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "paymentType", "receiver", "iban", "bic"))
                        .add(UICol()
                                .add(lc, "bemerkung")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "besonderheiten")))
                // Positionen
                .add(UIList(lc, "positionen", "position")
                        .add(UIRow()
                                .add(UICol()
                                        .add(lc, "position.menge"))
                                .add(UICol()
                                        .add(lc, "position.einzelNetto"))
                                .add(UICol()
                                        .add(lc, "position.vat"))
                                .add(UICol()
                                        .add(lc, "position.netSum"))
                                .add(UICol()
                                        .add(lc, "position.vatAmount"))
                                .add(UICol()
                                        .add(lc, "position.bruttoSum")))
                        .add(UIRow()
                                .add(UICol()
                                        .add(lc, "text")))
                        .add(UILabel("TODO: kost1, kost2, netto, prozent")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
