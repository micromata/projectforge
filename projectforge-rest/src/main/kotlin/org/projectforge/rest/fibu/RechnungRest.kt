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

import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/invoice")
class RechnungRest() : AbstractDORest<RechnungDO, RechnungDao>(RechnungDao::class.java, "fibu.rechnung.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        // TODO: Status refers to positionen
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "nummer", "kunde", "projekt", "account", "betreff", "datum", "faelligkeit",
                                "bezahlDatum", "periodOfPerformanceBegin", "periodOfPerformanceEnd")
                        .add(UITableColumn("netSum", title = translate("fibu.common.netto"), dataType = UIDataType.DECIMAL))
                        .add(UITableColumn("grossSum", title = translate("fibu.rechnung.bruttoBetrag"), dataType = UIDataType.DECIMAL))
                        .add(lc, "orders", "bemerkung", "status"))
        layout.getTableColumnById("kunde").formatter = Formatter.CUSTOMER
        layout.getTableColumnById("projekt").formatter = Formatter.PROJECT
        layout.getTableColumnById("datum").formatter = Formatter.DATE
        layout.getTableColumnById("faelligkeit").formatter = Formatter.DATE
        layout.getTableColumnById("bezahlDatum").formatter = Formatter.DATE
        layout.getTableColumnById("periodOfPerformanceBegin").formatter = Formatter.DATE
        layout.getTableColumnById("periodOfPerformanceEnd").formatter = Formatter.DATE
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: RechnungDO): UILayout {
        val layout = super.createEditLayout(dto)
                .add(lc, "betreff")
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "typ"))
                        .add(UICol()
                                .add(lc, "status", "account"))
                        .add(UICol()
                                .add(lc, "datum", "vatAmountSum", "bezahlDatum", "faelligkeit"))
                        .add(UICol()
                                .add(lc, "netSum", "grossSum")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "projekt", "kunde", "customerAddress", "customerref1", "attachment")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "bemerkung"))
                        .add(UICol()
                                .add(lc, "besonderheiten")))
                .add(UILabel("TODO: Customized element for Pos"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
