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
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AGGridSupport
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Rechnung
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
    val layout = super.createListLayout(request, magicFilter)
    AGGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      magicFilter,
      RechnungMultiSelectedPageRest::class.java,
    )
      .add(lc, "nummer", width = 120)
      .add(lc, "customer", lcField = "kunde")
      .add(lc, "project", lcField = "projekt")
      .add(lc, "betreff", "datum", "faelligkeit", "bezahlDatum")
      .add(lc, "statusAsString", headerName = "fibu.rechnung.status", width = 100)
      .add(lc, "formattedNetSum", headerName = "fibu.common.netto", pfStyle = UIAgGridColumnDef.PF_STYLE.CURRENCY)
      .add(
        lc,
        "formattedGrossSum",
        headerName = "fibu.rechnung.bruttoBetrag",
        pfStyle = UIAgGridColumnDef.PF_STYLE.CURRENCY
      )
      .add(lc, "konto", "bemerkung", "periodOfPerformanceBegin", "periodOfPerformanceEnd")
      .withMultiRowSelection(request, magicFilter)
      .withPinnedLeft(3)
      .withGetRowClass("""if (params.node.data.ueberfaellig) {
            return 'ag-row-red';
        } else if (params.node.data.status !== 'BEZAHLT') {
            return 'ag-row-blue';
        }"""
      )

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
    } else {
      rechnung.project?.displayName = obj.projekt?.name
    }
    return rechnung
  }
}
