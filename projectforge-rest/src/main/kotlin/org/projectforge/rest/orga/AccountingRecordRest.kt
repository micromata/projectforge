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

package org.projectforge.rest.orga

import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/accountingRecord")
class AccountingRecordRest: AbstractDORest<BuchungssatzDO, BuchungssatzDao>(baseDaoClazz = BuchungssatzDao::class.java, i18nKeyPrefix = "fibu.buchungssatz.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "formattedSatzNummer", "betrag", "beleg", "kost1", "kost2", "konto", "gegenKonto",
                                "sh", "text", "comment"))
        layout.getTableColumnById("kost1").formatter = Formatter.COST1
        layout.getTableColumnById("kost2").formatter = Formatter.COST2
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
        layout.getTableColumnById("gegenKonto").formatter = Formatter.KONTO
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: BuchungssatzDO): UILayout {
        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "datum", "year", "month", "satznr", "betrag", "sh", "beleg"))
                        .add(UICol()
                                .add(lc, "kost1", "kost2", "konto", "gegenKonto", "text", "menge")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "comment")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
