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

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.MultiSelectionSupport
import org.projectforge.rest.dto.Eingangsrechnung
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/incomingInvoice")
class EingangsrechnungPagesRest : AbstractDTOPagesRest<EingangsrechnungDO, Eingangsrechnung, EingangsrechnungDao>(
  EingangsrechnungDao::class.java,
  "fibu.eingangsrechnung.title"
) {

  override fun transformForDB(dto: Eingangsrechnung): EingangsrechnungDO {
    val eingangsrechnungDO = EingangsrechnungDO()
    dto.copyTo(eingangsrechnungDO)
    return eingangsrechnungDO
  }

  override fun transformFromDB(obj: EingangsrechnungDO, editMode: Boolean): Eingangsrechnung {
    val eingangsrechnung = Eingangsrechnung()
    eingangsrechnung.copyFrom(obj)
    return eingangsrechnung
  }

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest): UILayout {
    val multiSelectionMode = MultiSelectionSupport.isMassSelection(request)
    val layout = super.createListLayout(request)
      .add(
        UIAgGrid.createUIResultSetTable()
          .add(lc, "kreditor")
          .add(UIAgGridColumnDef("konto", headerName="fibu.konto", valueGetter = "data.konto.nummer + ' ' + data.konto.bezeichnung"))
          .add(lc, "referenz", "betreff", "datum", "faelligkeit", "bezahlDatum")
          .add(UIAgGridColumnDef("formattedNetSum", headerName = "fibu.common.netto").withType(UIAgGridColumnDef.TYPE.NUMERIC_COLUMN))
          .add(UIAgGridColumnDef("formattedGrossSum", headerName = "fibu.rechnung.bruttoBetrag").withType(UIAgGridColumnDef.TYPE.NUMERIC_COLUMN))
          .add(lc, "bemerkung")
          .withMultiRowSelection(multiSelectionMode)
      )
    //layout.getTableColumnById("konto").formatter = Formatter.KONTO
    //layout.getTableColumnById("faelligkeit").formatter = Formatter.DATE
    //layout.getTableColumnById("bezahlDatum").formatter = Formatter.DATE
    return LayoutUtils.processListPage(layout, this)
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
      this.eingangsrechnungDao,
      eingangsrechnung,
      postData,
      this,
      this.validate(eingangsrechnung)
    )
  }
}
