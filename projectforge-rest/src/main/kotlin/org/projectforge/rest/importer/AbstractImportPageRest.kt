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

package org.projectforge.rest.importer

import org.projectforge.business.common.ListStatisticsSupport
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import kotlin.reflect.KProperty

abstract class AbstractImportPageRest<O : ImportEntry.Modified<O>> : AbstractDynamicPageRest() {
  /**
   * Contains the data, layout and filter settings served by [getInitialList].
   */
  class InitialListData(
    val ui: UILayout?,
    val data: ResultSet<*>,
  )

  @Autowired
  protected lateinit var agGridSupport: AGGridSupport

  protected fun createLayout(
    request: HttpServletRequest,
    title: String,
    importStorage: ImportStorage<*>?
  ): FormLayoutData {
    val layout = UILayout(title)
    val hasEntries = importStorage?.entries?.isNotEmpty() == true
    val data = importStorage?.info
    if (!hasEntries) {
      layout.add(UIAlert("import.error.nothingToImport", color = UIColor.DANGER))
    } else {
      if (data != null) {
        val statsSupport = ListStatisticsSupport()
        statsSupport.append("import.entries.total", NumberFormatter.format(data.totalNumber))
        addIfNotZero(statsSupport, "import.entries.new", data.numberOfNewEntries, ListStatisticsSupport.Color.GREEN)
        addIfNotZero(
          statsSupport,
          "import.entries.deleted",
          data.numberOfDeletedEntries,
          ListStatisticsSupport.Color.RED
        )
        addIfNotZero(
          statsSupport,
          "import.entries.modified",
          data.numberOfModifiedEntries,
          ListStatisticsSupport.Color.BLUE
        )
        addIfNotZero(statsSupport, "import.entries.unmodified", data.numberOfUnmodifiedEntries)
        val row = UIRow()
        layout.add(row)
        row.add(
          UICol(md = 6)
            .add(UIAlert("'${statsSupport.asMarkdown}", color = UIColor.LIGHT, markdown = true))
        )
        if (data.totalNumber > 0) {
          val col = UICol(md = 6)
          row.add(col)
          val checkboxGroup = UIRow()
          col.add(checkboxGroup)
          checkboxGroup.add(UILabel("import.display.options"))
          addCheckBoxIfNotZero(layout, checkboxGroup, "new", data.numberOfNewEntries)
          addCheckBoxIfNotZero(layout, checkboxGroup, "deleted", data.numberOfDeletedEntries)
          addCheckBoxIfNotZero(layout, checkboxGroup, "modified", data.numberOfModifiedEntries)
          addCheckBoxIfNotZero(layout, checkboxGroup, "unmodified", data.numberOfUnmodifiedEntries)
        }
      }
      val agGrid = UIAgGrid("entries")
      layout.add(agGrid)
      agGridSupport.prepareUIGrid4MultiSelectionListPage(
        request,
        layout,
        agGrid,
        this,
        pageAfterMultiSelect = this::class.java,
      )
      // agGrid.height = "window.screen.height - 400"
      createListLayout(request, layout, agGrid)
      agGrid.withMultiRowSelection()
    }

    val settingsInfo = StringBuilder()
    settingsInfo
      .appendLine("### ${translate("import.info.detectedColumns")}")
      .appendLine()
      .appendLine("| ${translate("import.field.name")} | ${translate("import.field.mapping")} |")
      .appendLine("| --- | --- |")
    if (importStorage != null) {
      importStorage.detectedColumns.entries.forEach {
        settingsInfo.appendLine("| ${it.value.label} | ${it.key} |")
      }
      settingsInfo
        .appendLine()
        .appendLine("### ${translate("import.info.unknownColumns")}")
        .appendLine()
        .appendLine(importStorage.unknownColumns.joinToString())
      layout.add(UIAlert("'$settingsInfo", color = UIColor.INFO, markdown = true))
      layout.add(createSettingsHelp(importStorage.importSettings))
    }
    LayoutUtils.process(layout)
    val formLayoutData = FormLayoutData(data, layout, createServerData(request))
    importStorage?.entries?.let { entries ->
      // Put result list to variables instead of data, otherwise any post data of the client will contain the whole list.
      formLayoutData.variables = mapOf("entries" to (entries ?: emptyList<Any>()))
    }
    return formLayoutData

  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(
    @Valid @RequestBody postData: PostData<ImportStorageInfo>
  ): ResponseEntity<ResponseAction> {
    val data = postData.data
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("data", data)
    )
  }

  protected abstract fun createListLayout(request: HttpServletRequest, layout: UILayout, agGrid: UIAgGrid)

  protected fun addReadColumn(agGrid: UIAgGrid, lc: LayoutContext, property: KProperty<*>) {
    val field = property.name
    agGrid.add(lc, "readEntry.$field", lcField = field)
  }

  protected fun addStoredColumn(agGrid: UIAgGrid, lc: LayoutContext, property: KProperty<*>) {
    val field = property.name
    agGrid.add(lc, "storedEntry.$field", lcField = field)
  }

  protected fun addIfNotZero(
    statsSupport: ListStatisticsSupport,
    i18nKey: String,
    number: Int,
    color: ListStatisticsSupport.Color? = null,
  ) {
    if (number > 0) {
      statsSupport.append(i18nKey, NumberFormatter.format(number), color)
    }
  }

  protected fun addCheckBoxIfNotZero(
    layout: UILayout,
    row: IUIContainer,
    id: String,
    number: Int,
  ) {
    if (number > 0) {
      val fieldId = "displayOptions.$id"
      row.add(UICheckbox(fieldId, label = "import.display.$id"))
      layout.watchFields.add(fieldId)
    }
  }

  companion object {
    fun getSessionAttributeName(importPageRest: Class<*>): String {
      return "${importPageRest.name}.importStorage"
    }

    fun createSettingsHelp(importSettings: ImportSettings?): UIAlert {
      val sb = StringBuilder()
      sb.appendLine(translate("import.help.settings.info"))
      sb.appendLine("```")
      if (importSettings?.charSet == null) {
        sb.appendLine("# Uncomment the following line for other coding than UTF-8")
        sb.appendLine("#encoding=iso-8859-15")
      }
      if (importSettings != null) {
        sb.appendLine("encoding=${importSettings.charSet.name()}")
        importSettings.allFieldNames.forEach { fieldName ->
          if (!arrayOf("id", "created", "lastUpdate", "deleted", "importSettings").contains(fieldName)) {
            val fieldSettings = importSettings.getFieldSettings(fieldName)
            sb.append("$fieldName=")
            if (fieldSettings != null) {
              sb.appendLine(fieldSettings.getSettingsAsString(true))
            } else {
              sb.appendLine()
            }
          }
        }
      }
      sb.appendLine("# Example of field 'birthday', found by header column:")
      sb.appendLine("#columnfield=[header column(s)]|:[format]")
      sb.appendLine("#birthday=birthday*|*born*|:dd.MM.yyyy|:dd.MM.yy")
      sb.appendLine("# header column(s) are mapped case-insensitive, wildchars '*' for text and '?' for single char are supported.")
      sb.appendLine("```")
      return UIAlert(sb.toString(), title = "import.help.settings.title", markdown = true, color = UIColor.LIGHT)
    }
  }
}
