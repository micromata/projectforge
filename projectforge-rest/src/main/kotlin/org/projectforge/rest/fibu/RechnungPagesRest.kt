/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Rechnung
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/outgoingInvoice")
class RechnungPagesRest :
  AbstractDTOPagesRest<RechnungDO, Rechnung, RechnungDao>(RechnungDao::class.java, "fibu.rechnung.title") {

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
    val multiSelectionMode = MultiSelectionSupport.isMultiSelection(request, magicFilter)
    val layout = super.createListLayout(request, magicFilter)
    MultiSelectionSupport.prepareUIGrid4ListPage(
      request,
      layout,
      RechnungMultiSelectedPageRest::class.java,
      magicFilter,
    )
      .add(lc, "nummer")
      .add(
        UIAgGridColumnDef(
          "kunde",
          headerName = "fibu.kunde",
          valueGetter = "data.customer.displayName",
          sortable = true,
        )
      )
      .add(
        UIAgGridColumnDef(
          "projekt",
          headerName = "fibu.projekt",
          valueGetter = "data.project.displayName",
          sortable = true,
        )
      )
      .add(
        UIAgGridColumnDef(
          "konto",
          headerName = "fibu.konto",
          valueGetter = "data.konto.displayName",
          sortable = true,
        )
      )
      .add(
        lc, "betreff", "datum", "faelligkeit", "status",
        "bezahlDatum", "periodOfPerformanceBegin", "periodOfPerformanceEnd"
      )
      .add(UIAgGridColumnDef("formattedNetSum", headerName = "fibu.common.netto"))
      .add(UIAgGridColumnDef("formattedGrossSum", headerName = "fibu.rechnung.bruttoBetrag"))
      .add(UIAgGridColumnDef("orders", headerName = "fibu.auftrag.auftraege", dataType = UIDataType.INT))
      .add(lc, "bemerkung")
      .withMultiRowSelection(request, this::class.java, multiSelectionMode)
    /*layout.getTableColumnById("kunde").formatter = Formatter.CUSTOMER
    layout.getTableColumnById("konto").formatter = Formatter.KONTO
    layout.getTableColumnById("projekt").formatter = Formatter.PROJECT
    layout.getTableColumnById("datum").formatter = Formatter.DATE
    layout.getTableColumnById("faelligkeit").formatter = Formatter.DATE
    layout.getTableColumnById("bezahlDatum").formatter = Formatter.DATE
    layout.getTableColumnById("periodOfPerformanceBegin").formatter = Formatter.DATE
    layout.getTableColumnById("periodOfPerformanceEnd").formatter = Formatter.DATE*/
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: Rechnung, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(lc, "betreff")
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "nummer", "typ")
          )
          .add(
            UICol()
              .add(lc, "status", "konto")
          )
          .add(
            UICol()
              .add(lc, "datum", "vatAmountSum", "bezahlDatum", "faelligkeit")
          )
          .add(
            UICol()
              .add(lc, "netSum", "grossSum", "zahlBetrag", "discountMaturity", "discountPercent")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(UISelect.createProjectSelect(lc, "projekt", false))
              .add(UISelect.createCustomerSelect(lc, "kunde", false))
              .add(
                lc,
                "kundeText",
                "customerAddress",
                "customerref1",
                "attachment",
                "periodOfPerformanceBegin",
                "periodOfPerformanceEnd"
              )
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "bemerkung")
          )
          .add(
            UICol()
              .add(lc, "besonderheiten")
          )
      )
      // Positionen
      .add(UICustomized("invoice.outgoingPosition"))
    return LayoutUtils.processEditPage(layout, dto, this)
  }


  override fun transformForDB(dto: Rechnung): RechnungDO {
    val rechnungDO = RechnungDO()
    dto.copyTo(rechnungDO)
    return rechnungDO
  }

  override fun transformFromDB(obj: RechnungDO, editMode: Boolean): Rechnung {
    val rechnung = Rechnung()
    rechnung.copyFrom(obj)
    if (editMode) {
      rechnung.copyPositionenFrom(obj)
    }
    return rechnung
  }
}
