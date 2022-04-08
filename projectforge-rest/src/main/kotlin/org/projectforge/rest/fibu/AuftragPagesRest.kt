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

import org.projectforge.business.fibu.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Auftrag
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/order")
open class AuftragPagesRest : // open needed by Wicket's SpringBean for proxying.
  AbstractDTOPagesRest<AuftragDO, Auftrag, AuftragDao>(AuftragDao::class.java, "fibu.auftrag.title") {
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

  @PostConstruct
  private fun postConstruct() {
    /**
     * Enable attachments for this entity.
     */
    enableJcr()
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
    val layout = super.createListLayout(request, magicFilter)
      .add(
        UITable.createUIResultSetTable()
          .add(lc, "nummer")
          .add(UITableColumn("kunde.displayName", title = "fibu.kunde"))
          .add(UITableColumn("projekt.displayName", title = "fibu.projekt"))
          .add(lc, "titel")
          .add(UITableColumn("pos", title = "label.position.short"))
          .add(UITableColumn("attachmentsSizeFormatted", titleIcon = UIIconType.PAPER_CLIP))
          .add(
            UITableColumn(
              "personDays", title = "projectmanagement.personDays",
              dataType = UIDataType.DECIMAL
            )
          )
          .add(lc, "referenz")
          .add(
            UITableColumn(
              "assignedPersons", title = "fibu.common.assignedPersons",
              dataType = UIDataType.STRING
            )
          )
          .add(lc, "erfassungsDatum", "entscheidungsDatum")
          .add(
            UITableColumn(
              "formattedNettoSumme", title = "fibu.auftrag.nettoSumme",
              dataType = UIDataType.DECIMAL
            )
          )
          .add(
            UITableColumn(
              "formattedBeauftragtNettoSumme", title = "fibu.auftrag.commissioned",
              dataType = UIDataType.DECIMAL
            )
          )
          .add(UITableColumn("formattedFakturiertSum", title = "fibu.fakturiert"))
          .add(UITableColumn("formattedZuFakturierenSum", title = "fibu.tobeinvoiced"))
          .add(lc, "periodOfPerformanceBegin", "periodOfPerformanceEnd", "probabilityOfOccurrence", "auftragsStatus")
      )
    layout.getTableColumnById("erfassungsDatum").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("entscheidungsDatum").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("periodOfPerformanceBegin").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("periodOfPerformanceEnd").formatter = UITableColumn.Formatter.DATE
    return LayoutUtils.processListPage(layout, this)
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    elements.add(
      UIFilterListElement("positionsArt", label = translate("fibu.auftrag.position.art"), defaultFilter = true)
        .buildValues(AuftragsPositionsArt::class.java)
    )
    elements.add(
      UIFilterListElement("positionsStatus", label = translate("fibu.auftrag.positions"), defaultFilter = true)
        .buildValues(AuftragsPositionsStatus::class.java)
    )
    elements.add(
      UIFilterListElement(
        "positionsPaymentType",
        label = translate("fibu.auftrag.position.paymenttype"),
        defaultFilter = true
      )
        .buildValues(AuftragsPositionsPaymentType::class.java)
    )
    elements.add(
      UIFilterListElement("fakturiert", label = translate("fibu.auftrag.status.fakturiert"), defaultFilter = true)
        .buildValues(AuftragFakturiertFilterStatus::class.java)
    )
    val statusFilter = elements.find { it is UIFilterElement && it.id == "auftragsStatus" } as UIFilterElement
    statusFilter.defaultFilter = true
  }

  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<AuftragDO>>? {
    val filters = mutableListOf<CustomResultFilter<AuftragDO>>()

    val positionTypeFilter = source.entries.find { it.field == "positionsArt" }
    positionTypeFilter?.synthetic = true // Don't process this filter by data base.
    positionTypeFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragsPositionsArtFilter.create(it))
      }
    }

    val positionsStatusFilter = source.entries.find { it.field == "positionsStatus" }
    positionsStatusFilter?.synthetic = true // Don't process this filter by data base.
    positionsStatusFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragsPositionsStatusFilter.create(it))
      }
    }

    val paymentTypeFilter = source.entries.find { it.field == "positionsPaymentType" }
    paymentTypeFilter?.synthetic = true // Don't process this filter by data base.
    paymentTypeFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragsPositionsPaymentTypeFilter.create(it))
      }
    }

    val fakturiertFilter = source.entries.find { it.field == "fakturiert" }
    fakturiertFilter?.synthetic = true // Don't process this filter by data base.
    fakturiertFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragFakturiertFilter.create(it))
      }
    }
    return filters
  }

  /**
   * LAYOUT Edit page: Only usable for attachments
   */
  override fun createEditLayout(dto: Auftrag, userAccess: UILayout.UserAccess): UILayout {
    userAccess.delete = false
    userAccess.update = false
    userAccess.cancel = false
    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIRow()
          .add(
            UICol()
              .add(UIReadOnlyField("nummer", lc))
              .add(UIReadOnlyField("customer.displayName", lc, label = "fibu.kunde"))
          )
          .add(
            UICol()
              .add(UIReadOnlyField("formattedNettoSumme", lc, label = "fibu.auftrag.nettoSumme"))
              .add(UIReadOnlyField("project.displayName", lc, label = "fibu.projekt"))
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(UIReadOnlyField("titel", lc))
          )
      )
      .add(
        UIFieldset(title = "attachment.list")
          .add(UIAttachmentList(category, dto.id))
      )
    //layout.enableHistoryBackButton()
    return LayoutUtils.processEditPage(layout, dto, this)
  }

  /**
   * LAYOUT Edit page (new version under construction)
   */
  @Suppress("FunctionName", "unused")
  private fun _createEditLayoutUnderConstruction(dto: Auftrag, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "nummer")
          )
          .add(
            UICol()
              .add(lc, "nettoSumme")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "title")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "referenz")
          )
          .add(
            UICol()
              .add(lc, "auftragsStatus")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "projekt", "projekt.status")
          )
          .add(
            UICol()
              .add(lc, "kunde", "kundeText")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "projectmanager")
          )
          .add(
            UICol()
              .add(lc, "headOfBusinessManager")
          )
          .add(
            UICol()
              .add(lc, "salesManager")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "erfassungsDatum")
          )
          .add(
            UICol()
              .add(lc, "angebotsDatum")
          )
          .add(
            UICol()
              .add(lc, "entscheidungsDatum")
          )
          .add(
            UICol()
              .add(lc, "bindungsFrist")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "contactPerson")
          )
          .add(
            UICol()
              .add(lc, "beauftragungsDatum")
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(lc, "periodOfPerformanceBegin", "periodOfPerformanceEnd", "probabilityOfOccurrence")
          )
          .add(
            UICol()
              .add(
                UIList(lc, "paymentSchedules", "paymentSchedule")
                  .add(
                    UICol()
                      .add(lc, "paymentSchedule.scheduleDate")
                  )
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
              .add(lc, "statusBeschreibung")
          )
      )
      // Positionen
      .add(
        UIList(lc, "positionen", "position")
          .add(
            UIRow()
              .add(
                UICol()
                  .add(lc, "position.titel")
              )
          )
          .add(
            UIRow()
              .add(
                UICol()
                  .add(lc, "position.art", "position.paymentType")
              )
              .add(
                UICol()
                  .add(lc, "position.personDays")
              )
              .add(
                UICol()
                  .add(lc, "position.nettoSumme")
              )
          )
          .add(
            UIRow()
              .add(
                UICol()
                  .add(lc, "position.art", "position.paymentType")
              )
              .add(
                UICol()
                  .add(lc, "position.fakturiertSum")
              )
              .add(
                UICol()
                  .add(lc, "position.status")
              )
          )
          .add(
            UIRow()
              .add(
                UICol()
                  .add(lc, "position.task", "position.periodOfPerformanceType", "position.bemerkung")
              )
          )
      )

    layout.getLabelledElementById("position.periodOfPerformanceType").label = "fibu.periodOfPerformance"

    return LayoutUtils.processEditPage(layout, dto, this)
  }
}
