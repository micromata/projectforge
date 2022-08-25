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

import mu.KotlinLogging
import org.projectforge.business.common.ListStatisticsSupport
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.jobs.JobInfoPageRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

abstract class AbstractImportPageRest<O : ImportPairEntry.Modified<O>> : AbstractDynamicPageRest() {
  @Autowired
  protected lateinit var agGridSupport: AGGridSupport

  protected abstract fun callerPage(request: HttpServletRequest): String

  protected abstract fun getImportStorage(request: HttpServletRequest): ImportStorage<O>?
  protected abstract fun clearImportStorage(request: HttpServletRequest)

  /**
   * Don't forget to fill the [ImportStorage.importResult].
   * @return Job id (null, if no job was created).
   */
  protected abstract fun import(importStorage: ImportStorage<*>, selectedEntries: List<ImportPairEntry<O>>): Int?

  protected fun createLayout(
    request: HttpServletRequest,
    title: String,
    importStorage: ImportStorage<*>?
  ): FormLayoutData {
    val layout = UILayout(title)
    val hasEntries = importStorage?.pairEntries?.isNotEmpty() == true
    val data = importStorage?.info
    val fieldset = UIFieldset(title = importStorage?.title ?: translate("import.title"))
    layout.add(fieldset)
    if (!hasEntries) {
      fieldset.add(UIAlert("import.error.nothingToImport", color = UIColor.DANGER))
      fieldset.add(
        UIButton.createCancelButton(
          ResponseAction(
            RestResolver.getRestUrl(this::class.java, "cancel"),
            targetType = TargetType.GET,
          )
        )
      )
    } else {
      if (data != null) {
        val row = UIRow()
        fieldset.add(row)
        row.add(
          UICol(md = 6)
            .add(UIAlert("'${createStatsMarkdown(data)}", color = UIColor.LIGHT, markdown = true))
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
          addCheckBoxIfNotZero(layout, checkboxGroup, "faulty", data.numberOfFaultyEntries)
          addCheckBoxIfNotZero(layout, checkboxGroup, "unknown", data.numberOfUnknownEntries)
        }
      }

      importStorage?.importResultAsAlert?.let {
        layout.add(it)
      }

      val agGrid = UIAgGrid("entries", listPageTable = true)
      layout.add(agGrid)
      agGridSupport.prepareUIGrid4MultiSelectionListPage(
        request,
        layout,
        agGrid,
        this,
        pageAfterMultiSelect = this::class.java,
      )
      agGrid.handleCancelUrl = RestResolver.getRestUrl(this::class.java, "cancelAndGetUrl")
      agGrid.urlAfterMultiSelect = RestResolver.getRestUrl(this::class.java, "import")
      // agGrid.height = "window.screen.height - 400"
      agGrid.add("statusAsString", headerName = "status", width = 120)
      createListLayout(request, layout, agGrid)
      agGrid.withMultiRowSelection()
      agGrid.multiSelectButtonTitle = translate("import")
      agGrid.multiSelectButtonConfirmMessage = translate("import.confirmMessage")
      agGrid.withGetRowClass(
        """if (params.node.data.status === 'NEW') {
             return 'ag-row-green';
           } else if (['DELETED', 'FAULTY', 'UNKNOWN', 'UNKNOWN_MODIFICATION'].includes(params.node.data.status)) {
             return 'ag-row-red';
           } else if (params.node.data.status === 'MODIFIED') {
             return 'ag-row-blue';
        }""".trimMargin()
      )
    }

    addSettingsInfo(layout, importStorage)

    LayoutUtils.process(layout)
    val formLayoutData = FormLayoutData(data, layout, createServerData(request))
    importStorage?.let { storage ->
      val entries = createListEntries(storage, data?.displayOptions ?: ImportStorage.DisplayOptions())
      formLayoutData.variables = mapOf("entries" to (entries))
    }
    return formLayoutData

  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(
    request: HttpServletRequest,
    @Valid @RequestBody postData: PostData<ImportStorageInfo>
  ): ResponseEntity<ResponseAction> {
    val data = postData.data
    val importStorage = getImportStorage(request)
      ?: return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE, merge = true)
        .addVariable("variables", mapOf("entries" to createListEntries(importStorage, data.displayOptions)))
    )
  }

  protected open fun createListEntries(
    importStorage: ImportStorage<*>,
    displayOptions: ImportStorage.DisplayOptions,
  ): List<ImportEntry<*>> {
    return importStorage.createEntries(displayOptions)
  }

  /**
   * Will be called, if the user wants to import the selected entries.
   */
  @PostMapping("import")
  fun import(
    request: HttpServletRequest,
    @RequestBody multiSelection: AbstractMultiSelectedPage.MultiSelection?
  ): ResponseEntity<ResponseAction> {
    val importStorage = getImportStorage(request)
    if (importStorage == null) {
      log.error("No import storage given (expired?). Can't proceed any import here.")
      return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }
    val selectedIds = multiSelection?.selectedIds
    val selectedEntries = mutableListOf<ImportPairEntry<O>>()
    if (selectedIds != null) {
      importStorage.pairEntries.forEach { entry ->
        if (selectedIds.contains(entry.id)) {
          selectedEntries.add(entry)
        }
      }
    }
    if (selectedEntries.isEmpty()) {
      importStorage.importResult = ImportStorage.ImportResult(translate("import.error.noEntrySelected"))
      // TODO: merge UPDATE
      return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }
    log.info { "User wants to import #${selectedEntries.size} entries..." }
    val jobId = import(importStorage, selectedEntries)
    if (jobId == null) {
      importStorage.importResult.let { result ->
        if (result == null) {
          throw IllegalArgumentException("Import without result object! Please contact the developer.")
        }
        result.errorMessages?.let { errors ->
          return showValidationErrors(*errors.map { ValidationError(it) }.toTypedArray())
        }
      }
    }
    return ResponseEntity.ok(
      ResponseAction(
        url = PagesResolver.getDynamicPageUrl(JobInfoPageRest::class.java, id = jobId, absolute = true),
        targetType = TargetType.REDIRECT,
      )
    )
  }

  /**
   * Called from UIButton cancel above.
   */
  @GetMapping("cancel")
  fun cancel(request: HttpServletRequest): ResponseAction {
    val callerPage = callerPage(request) // must be called before clearImportStorage!
    clearImportStorage(request)
    return ResponseAction(callerPage)
  }

  /**
   * Called by DynamicListPageAgGrid
   */
  @GetMapping("cancelAndGetUrl")
  fun cancelAndGetUrl(request: HttpServletRequest): String {
    val callerPage = callerPage(request) // must be called before clearImportStorage!
    clearImportStorage(request)
    return callerPage
  }

  protected abstract fun createListLayout(request: HttpServletRequest, layout: UILayout, agGrid: UIAgGrid)

  protected fun addReadColumn(agGrid: UIAgGrid, lc: LayoutContext, property: KProperty<*>) {
    val field = property.name
    agGrid.add(lc, "read.$field", lcField = field)
  }

  protected fun addStoredColumn(agGrid: UIAgGrid, lc: LayoutContext, property: KProperty<*>) {
    val field = property.name
    agGrid.add(lc, "stored.$field", lcField = field)
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
      row.add(UICheckbox(fieldId, label = "import.entry.status.$id"))
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

  private fun addSettingsInfo(container: IUIContainer, importStorage: ImportStorage<*>?) {
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
      container.add(UIAlert("'$settingsInfo", color = UIColor.WARNING, markdown = true))
      container.add(createSettingsHelp(importStorage.importSettings))
    }
  }

  private fun createStatsMarkdown(data: ImportStorageInfo): String {
    val statsSupport = ListStatisticsSupport()
    statsSupport.append("import.stats.total", NumberFormatter.format(data.totalNumber))
    addIfNotZero(statsSupport, "import.stats.new", data.numberOfNewEntries, ListStatisticsSupport.Color.GREEN)
    addIfNotZero(
      statsSupport,
      "import.stats.deleted",
      data.numberOfDeletedEntries,
      ListStatisticsSupport.Color.RED,
    )
    addIfNotZero(
      statsSupport,
      "import.stats.modified",
      data.numberOfModifiedEntries,
      ListStatisticsSupport.Color.BLUE,
    )
    addIfNotZero(statsSupport, "import.stats.unmodified", data.numberOfUnmodifiedEntries)
    addIfNotZero(
      statsSupport,
      "import.stats.faulty",
      data.numberOfFaultyEntries,
      ListStatisticsSupport.Color.RED,
    )
    addIfNotZero(
      statsSupport,
      "import.stats.unknown",
      data.numberOfUnknownEntries,
      ListStatisticsSupport.Color.RED,
    )
    return statsSupport.asMarkdown
  }
}
