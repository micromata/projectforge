/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Auftrag
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/order")
class AuftragPagesRest: AbstractDTOPagesRest<AuftragDO, Auftrag, AuftragDao>(AuftragDao::class.java, "fibu.auftrag.title") {
    override fun transformForDB(dto: Auftrag): AuftragDO {
        val auftragDO = AuftragDO()
        dto.copyTo(auftragDO)
        return auftragDO
    }

    override fun transformFromDB(obj: AuftragDO, editMode: Boolean): Auftrag {
        val auftrag = Auftrag()
        auftrag.copyFrom(obj)
        return auftrag
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(lc, "nummer")
                        .add(UITableColumn("kunde.displayName", title = "fibu.kunde"))
                        .add(UITableColumn("projekt.displayName", title = "fibu.projekt"))
                        .add(lc, "titel")
                        .add(UITableColumn("pos", title = "label.position.short"))
                        .add(UITableColumn("personDays", title = "projectmanagement.personDays",
                                dataType = UIDataType.DECIMAL))
                        .add(lc, "referenz")
                        .add(UITableColumn("assignedPersons", title = "fibu.common.assignedPersons",
                                dataType = UIDataType.STRING))
                        .add(lc, "erfassungsDatum", "entscheidungsDatum")
                        .add(UITableColumn("formattedNettoSumme", title = "fibu.auftrag.nettoSumme",
                                dataType = UIDataType.DECIMAL))
                        .add(UITableColumn("formattedBeauftragtNettoSumme", title = "fibu.auftrag.commissioned",
                                dataType = UIDataType.DECIMAL))
                        .add(UITableColumn("formattedFakturiertSum", title = "fibu.fakturiert"))
                        .add(UITableColumn("formattedZuFakturierenSum", title = "fibu.tobeinvoiced"))
                        .add(lc, "periodOfPerformanceBegin", "periodOfPerformanceEnd", "probabilityOfOccurrence", "auftragsStatus"))
        layout.getTableColumnById("erfassungsDatum").formatter = Formatter.DATE
        layout.getTableColumnById("entscheidungsDatum").formatter = Formatter.DATE
        layout.getTableColumnById("periodOfPerformanceBegin").formatter = Formatter.DATE
        layout.getTableColumnById("periodOfPerformanceEnd").formatter = Formatter.DATE
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Auftrag, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer"))
                        .add(UICol()
                                .add(lc, "nettoSumme")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "title")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "referenz"))
                        .add(UICol()
                                .add(lc, "auftragsStatus")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "projekt", "projekt.status"))
                        .add(UICol()
                                .add(lc, "kunde", "kundeText")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "projectmanager"))
                        .add(UICol()
                                .add(lc, "headOfBusinessManager"))
                        .add(UICol()
                                .add(lc, "salesManager")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "erfassungsDatum"))
                        .add(UICol()
                                .add(lc, "angebotsDatum"))
                        .add(UICol()
                                .add(lc, "entscheidungsDatum"))
                        .add(UICol()
                                .add(lc, "bindungsFrist")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "contactPerson"))
                        .add(UICol()
                                .add(lc, "beauftragungsDatum")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "periodOfPerformanceBegin", "periodOfPerformanceEnd", "probabilityOfOccurrence"))
                        .add(UICol()
                                .add(UIList(lc, "paymentSchedules", "paymentSchedule")
                                        .add(UICol()
                                                .add(lc, "paymentSchedule.scheduleDate")))))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "bemerkung"))
                        .add(UICol()
                                .add(lc, "statusBeschreibung")))
                // Positionen
                .add(UIList(lc, "positionen", "position")
                        .add(UIRow()
                                .add(UICol()
                                        .add(lc, "position.titel")))
                        .add(UIRow()
                                .add(UICol()
                                        .add(lc, "position.art", "position.paymentType"))
                                .add(UICol()
                                        .add(lc, "position.personDays"))
                                .add(UICol()
                                        .add(lc, "position.nettoSumme")))
                        .add(UIRow()
                                .add(UICol()
                                        .add(lc, "position.art", "position.paymentType"))
                                .add(UICol()
                                        .add(lc, "position.fakturiertSum"))
                                .add(UICol()
                                        .add(lc, "position.status")))
                        .add(UIRow()
                                .add(UICol()
                                        .add(lc, "position.task", "position.periodOfPerformanceType", "position.bemerkung"))))

        layout.getLabelledElementById("position.periodOfPerformanceType").label = "fibu.periodOfPerformance"

        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
