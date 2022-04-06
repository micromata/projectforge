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

package org.projectforge.rest.multiselect

import de.micromata.merlin.excel.ExcelCell
import de.micromata.merlin.utils.ReplaceUtils
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.CellValue
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.DownloadFileSupport
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Base class of mass updates after multi selection.
 */
abstract class AbstractMultiSelectedPage<T : IdObject<out Serializable>> : AbstractDynamicPageRest() {
  class MultiSelection {
    var selectedIds: Collection<Serializable>? = null
  }

  /**
   * If not a standard react page (e. g. Wicket-Page), modify this variable. The standard list and multi-selection-page
   * is auto-detected by [PagesResolver] with parameter [pageRestClass].
   */
  protected open val listPageUrl: String
    get() = PagesResolver.getListPageUrl(pagesRestClass, absolute = true)

  abstract fun getTitleKey(): String

  protected abstract val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>

  protected abstract fun ensureUserLogSubscription(): LogSubscription

  private val downloadFileSupport =
    DownloadFileSupport(expiringSessionAttribute = "${this::class.java.name}.downloadFile", downloadExpiryMinutes = 5)

  /**
   * Should be set for i18n translations of Excel-Export.
   */
  open val layoutContext: LayoutContext? = null

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val massUpdateData = mutableMapOf<String, MassUpdateParameter>()
    val variables = mutableMapOf<String, Any>()
    val layout = getLayout(request, massUpdateData, variables)
    return FormLayoutData(massUpdateData, layout, createServerData(request), variables)
  }

  @PostMapping("massUpdate")
  fun massUpdate(
    request: HttpServletRequest,
    @RequestBody postData: PostData<Map<String, MassUpdateParameter>>
  ): ResponseEntity<*> {
    val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRestClass)

    val massUpdateContext = MassUpdateContext<T>(postData.data.toMutableMap())
    handleClientMassUpdateCall(request, massUpdateContext)
    massUpdate(selectedIds, massUpdateContext)?.let { return it }
    val excel = MultiSelectionExcelExport.export(massUpdateContext, this)
    val filename =
      ReplaceUtils.encodeFilename("${translate(getTitleKey())}_${PFDateTime.now().format4Filenames()}.xlsx", true)
    downloadFileSupport.storeDownloadFile(request, filename, excel)
    val variables = mutableMapOf<String, Any>()

    val massUpdateData = postData.data.toMutableMap()
    val layout = getLayout(request, massUpdateData, variables, massUpdateContext)
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", layout)
        .addVariable("data", massUpdateData)
    )
  }

  @GetMapping("download")
  fun download(request: HttpServletRequest): ResponseEntity<*> {
    val downloadFile = downloadFileSupport.getDownloadFile(request)
      ?: return RestUtils.badRequest(translate("download.expired"))
    log.info("Downloading '${downloadFile.filename}' of size ${downloadFile.sizeHumanReadable}.")
    return RestUtils.downloadFile(downloadFile.filename, downloadFile.bytes)
  }

  /**
   * Used by TimesheetMultiSelectedPage for fixing kost2 issues. Does nothing at default.
   */
  protected open fun handleClientMassUpdateCall(request: HttpServletRequest, massUpdateContext: MassUpdateContext<T>) {
  }

  /**
   * First excel columns for identification. Default is "Element|30", means db id of column width 11 and
   * identifier of length 30. Must match [getExcelIdentifierCells].
   */
  open fun customizeExcelIdentifierHeadCells(): Array<String> {
    return arrayOf("Element|30")
  }

  /**
   * First excel columns for identification. Default is id and identifier. Must match [customizeExcelIdentifierHeadCells].
   */
  open fun getExcelIdentifierCells(massUpdateObject: MassUpdateObject<T>): List<Any?> {
    return mutableListOf(massUpdateObject.identifier)
  }

  open fun handleValue(
    cell: ExcelCell,
    field: String,
    value: Any?,
  ): Boolean {
    return false
  }

  /**
   * @param cellValue may differ from value (e. g. this is the displayValue).
   * @return true, if the cell style was set or false, if nothing was done and the cell style could be set by [MultiSelectionExcelExport].
   */
  open fun handleCellStyle(cell: ExcelCell, field: String, value: Any?, cellValue: Any?): Boolean {
    return false
  }

  /**
   * Field translation is used by Excel export. Returns translation of field from LayoutContext, if availabel in this
   * class, or capitalized field name itself at default.
   * You may use [getFieldTranslation] with param [LayoutContext] for auto translation of known fields in your derived fun.
   */
  open fun getFieldTranslation(field: String): String {
    ElementsRegistry.getElementInfo(layoutContext, field)?.i18nKey?.let {
      return translate(it)
    }
    return field.replaceFirstChar { it.lowercase() }
  }

  fun massUpdate(selectedIds: Collection<Serializable>?, massUpdateContext: MassUpdateContext<T>): ResponseEntity<*>? {
    if (selectedIds.isNullOrEmpty()) {
      return showNoEntriesValidationError()
    }
    if (selectedIds.size > BaseDao.MAX_MASS_UPDATE) {
      return showValidationErrors(
        ValidationError(
          translateMsg(
            BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N,
            BaseDao.MAX_MASS_UPDATE
          )
        )
      )
    }
    val massUpdateData = massUpdateContext.massUpdateData
    var nothingToDo = true
    val validationErrors = mutableListOf<ValidationError>()
    massUpdateData.forEach { (field, param) ->
      if (checkParamHasAction(massUpdateData, param, field, validationErrors)) {
        nothingToDo = false
      }
    }
    if (!validationErrors.isEmpty()) {
      return showValidationErrors(*validationErrors.toTypedArray())
    }
    if (nothingToDo) {
      return showNothingToDoValidationError()
    }

    proceedMassUpdate(selectedIds, massUpdateContext)?.let { responseEntity ->
      return responseEntity
    }
    if (massUpdateContext.nothingDone) {
      return showNoEntriesValidationError()
    }
    return null
  }

  /**
   * @params Supply all params for complexer checks (e. g. taskAndKost2 has to look at parameter task and kost2).
   */
  protected open fun checkParamHasAction(
    params: Map<String, MassUpdateParameter>,
    param: MassUpdateParameter,
    field: String,
    validationErrors: MutableList<ValidationError>
  ): Boolean {
    param.error?.let { message ->
      validationErrors.add(ValidationError(translate(message), "$field.textValue"))
      validationErrors.add(ValidationError("${translate(message)}: $field"))
      return false
    }
    return param.hasAction
  }

  /**
   * @return null to handle ResponseEntity result by this class. If ResponseEntity is returned, it will be used.
   */
  protected abstract fun proceedMassUpdate(
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<T>,
  ): ResponseEntity<*>?

  abstract fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  )

  protected fun getLayout(
    request: HttpServletRequest,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    variables: MutableMap<String, Any>,
    massUpdateContext: MassUpdateContext<T>? = null,
  ): UILayout {
    val layout = UILayout(getTitleKey())

    val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRestClass)
    val formattedSize = NumberFormatter.format(selectedIds?.size)
    if (selectedIds.isNullOrEmpty()) {
      layout.add(UIAlert("massUpdate.error.noEntriesSelected", color = UIColor.DANGER))
    } else {
      layout.add(
        UIAlert(
          "'${translateMsg("massUpdate.entriesFound", formattedSize)}",
          color = UIColor.SUCCESS
        )
      )
    }

    fillForm(request, layout, massUpdateData, selectedIds, variables)

    layout.add(UIAlert(message = "massUpdate.info", color = UIColor.INFO))
    layout.add(
      UIButton.createCancelButton(
        ResponseAction(
          listPageUrl,
          targetType = TargetType.REDIRECT
        )
      )
    )
    if (!MultiSelectionSupport.getRegisteredEntityIds(request, pagesRestClass).isNullOrEmpty()) {
      layout.add(
        UIButton.createBackButton(
          ResponseAction(
            PagesResolver.getMultiSelectionPageUrl(pagesRestClass, absolute = true),
            targetType = TargetType.REDIRECT
          ),
          title = "massUpdate.changeSelection",
        )
      )
    }
    if (!selectedIds.isNullOrEmpty()) {
      layout.add(
        UIButton.createDefaultButton(
          id = "execute",
          title = "execute",
          responseAction = ResponseAction(
            url = "${getRestPath()}/massUpdate",
            targetType = TargetType.POST
          ),
          confirmMessage = translateMsg("massUpdate.confirmQuestion", formattedSize),
        )
      )
    }
    massUpdateContext?.let { stats ->
      if (stats.errorCounter > 0) {
        val sb = StringBuilder()
        sb.appendLine("'*${stats.resultMessage}*")
        sb.appendLine()
        sb.appendLine("| # | ${translate("massUpdate.error.table.element")} | ${translate("massUpdate.error.table.message")}    |")
          .appendLine("| --: | :-- | :-- |")
        stats.errorMessages.forEachIndexed { index, error ->
          sb.appendLine("| ${index + 1} | ${error.identifier} | ${error.message} |")
        }
        layout.add(
          UIAlert(
            sb.toString(),
            title = "massUpdate.error.table.title",
            color = UIColor.DANGER,
            markdown = true
          )
        )
      } else if (stats.total > 0) {
        layout.add(UIAlert(message = "'${stats.resultMessage}"))
      } else {
        // Do nothing.
      }
    }
    downloadFileSupport.getDownloadFile(request)?.let { download ->
      val download = DownloadFileSupport.Download(download)
      variables["download"] = download
      layout.add(
        UIRow().add(
          downloadFileSupport.createDownloadFieldset(
            "massUpdate.excel.download",
            "${getRestPath()}/download",
            download,
            useDataObject = false,
          )
        )
      )
    }
    layout.add(
      MenuItem(
        "logViewer",
        i18nKey = "plugins.merlin.viewLogs",
        url = PagesResolver.getDynamicPageUrl(
          LogViewerPageRest::class.java,
          id = ensureUserLogSubscription().id
        ),
        type = MenuItemTargetType.REDIRECT,
      )
    )
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return layout
  }

  @PostMapping(URL_PATH_SELECTED)
  fun selected(
    request: HttpServletRequest,
    @RequestBody selectedIds: MultiSelection?
  ): ResponseEntity<*> {
    MultiSelectionSupport.registerSelectedEntityIds(request, pagesRestClass, selectedIds?.selectedIds)
    return ResponseEntity.ok(
      ResponseAction(
        targetType = TargetType.REDIRECT,
        url = PagesResolver.getDynamicPageUrl(this::class.java, absolute = true)
      )
    )
  }

  /**
   * @param minLengthOfTextArea See [LayoutUtils.buildLabelInputElement]
   */
  protected fun createInputFieldRow(
    lc: LayoutContext,
    field: String,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    minLengthOfTextArea: Int = LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
  ): UIRow {
    val el = LayoutUtils.buildLabelInputElement(lc, field, minLengthOfTextArea)
    if (el is UIInput) {
      el.id = when (el.dataType) {
        UIDataType.DATE -> "$field.localDateValue"
        UIDataType.AMOUNT, UIDataType.DECIMAL -> "$field.decimalValue"
        UIDataType.INT -> "$field.intValue"
        UIDataType.KONTO, UIDataType.USER, UIDataType.TASK, UIDataType.GROUP, UIDataType.EMPLOYEE -> "$field.intValue"
        UIDataType.BOOLEAN -> "$field.booleanValue"
        UIDataType.TIMESTAMP -> "$field.timestampValue"
        UIDataType.TIME -> "$field.timeValue"
        else -> "$field.textValue"
      }
      el.required = false //
    } else if (el is IUIId) {
      el.id = "$field.textValue"
    }
    val elementInfo = ElementsRegistry.getElementInfo(lc, field)
    return createInputFieldRow(field, el, massUpdateData, showDeleteOption = elementInfo?.required != true)
  }

  protected fun createInputFieldRow(
    field: String,
    el: UIElement,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    showDeleteOption: Boolean = false,
    myOptions: List<UIElement>? = null,
  ): UIRow {
    val param = massUpdateData[field] ?: MassUpdateParameter()
    param.delete = false
    massUpdateData[field] = param
    UIRow().let { row ->
      row.add(UICol(md = 7).add(el))
      val optionsRow = UIRow()
      row.add(UICol(md = 5).add(optionsRow))
      val options = mutableListOf<UIElement>()
      if (showDeleteOption) {
        options.add(
          UICheckbox(
            "$field.delete",
            label = "massUpdate.field.checkbox4deletion",
            tooltip = "massUpdate.field.checkbox4deletion.info",
          )
        )
      }
      if (el is UITextArea || (el is UIInput && el.dataType == UIDataType.STRING)) {
        options.add(
          UIInput(
            "$field.replaceText",
            label = "massUpdate.field.replace",
            tooltip = "massUpdate.field.replace.info"
          )
        )
      }
      if (el is UITextArea) {
        options.add(
          UICheckbox(
            "$field.append",
            label = "massUpdate.field.checkbox4appending",
            tooltip = "massUpdate.field.checkbox4appending.info"
          )
        )
      }
      myOptions?.let { options.addAll(it) }
      options.forEachIndexed { index, uiElement ->
        if (index > 0) {
          // Ugly: Add space:
          optionsRow.add(UISpacer())
        }
        optionsRow.add(uiElement)
      }
      return row
    }
  }

  /**
   * @param minLengthOfTextArea See [LayoutUtils.buildLabelInputElement]
   * @param append If true, the append checkbox will be preset (without function for non-text-area-fields)
   */
  protected fun createAndAddFields(
    lc: LayoutContext,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    container: IUIContainer,
    vararg fields: String,
    minLengthOfTextArea: Int = LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
    append: Boolean? = null,
  ) {
    fields.forEach { field ->
      container.add(createInputFieldRow(lc, field, massUpdateData, minLengthOfTextArea))
      if (append == true) {
        ensureMassUpdateParam(massUpdateData, field).append = true
      }
    }
  }

  protected fun showNoEntriesValidationError(): ResponseEntity<ResponseAction> {
    return showValidationErrors(ValidationError(translate("massUpdate.error.noEntriesSelected")))
  }

  protected fun showNothingToDoValidationError(): ResponseEntity<ResponseAction> {
    return showValidationErrors(ValidationError(translate("massUpdate.error.nothingToDo")))
  }

  companion object {
    const val URL_PATH_SELECTED = "selected"
    const val URL_SUFFIX_SELECTED = "Selected"

    fun ensureMassUpdateParam(
      massUpdateData: MutableMap<String, MassUpdateParameter>,
      name: String
    ): MassUpdateParameter {
      massUpdateData[name]?.let { return it }
      MassUpdateParameter().let {
        massUpdateData[name] = it
        return it
      }
    }

    fun processTextParameter(
      data: Any,
      property: String,
      params: Map<String, MassUpdateParameter>,
    ) {
      TextFieldModification.processTextParameter(data, property, params)
    }
  }
}
