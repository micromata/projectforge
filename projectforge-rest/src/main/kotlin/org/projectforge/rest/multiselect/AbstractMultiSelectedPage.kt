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

import org.projectforge.common.BeanHelper
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
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

/**
 * Base class of mass updates after multi selection.
 */
abstract class AbstractMultiSelectedPage : AbstractDynamicPageRest() {
  class MultiSelection {
    var selectedIds: Collection<Serializable>? = null
  }

  /**
   * If not a standard react page (e. g. Wicket-Page), modify this variable. The standard list and multi-selection-page
   * is auto-detected by [PagesResolver] with parameter [pageRestClass].
   */
  protected open val listPageUrl: String
    get() = PagesResolver.getListPageUrl(pagesRestClass, absolute = true)

  protected abstract fun getTitleKey(): String

  protected abstract val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>

  protected abstract fun ensureUserLogSubscription(): LogSubscription

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val massUpdateData = mutableMapOf<String, MassUpdateParameter>()
    val variables = mutableMapOf<String, Any>()
    val layout = getLayout(request, massUpdateData, variables)
    LayoutUtils.process(layout)

    layout.postProcessPageMenu()

    return FormLayoutData(massUpdateData, layout, createServerData(request), variables)
  }

  @PostMapping("massUpdate")
  fun massUpdate(
    request: HttpServletRequest,
    @RequestBody postData: PostData<Map<String, MassUpdateParameter>>
  ): ResponseEntity<*> {
    val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRestClass)
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
    val params = postData.data
    var nothingToDo = true
    val validationErrors = mutableListOf<ValidationError>()
    params.forEach { (field, param) ->
      if (checkParamHasAction(params, param, field, validationErrors)) {
        nothingToDo = false
      }
    }
    if (!validationErrors.isEmpty()) {
      return showValidationErrors(*validationErrors.toTypedArray())
    }
    if (nothingToDo) {
      return showNothingToDoValidationError()
    }
    return proceedMassUpdate(request, params, selectedIds)
  }

  /**
   * @params Supply all params for complexer checks (e. g. taskAndKost2 has to look at parameter task and kost2).
   */
  protected open fun checkParamHasAction(params: Map<String, MassUpdateParameter>, param: MassUpdateParameter, field: String, validationErrors: MutableList<ValidationError>): Boolean {
    if (param.isEmpty()) {
      if (param.delete == true) {
        return true // Delete action.
      }
    } else {
      if (param.delete == true) {
        validationErrors.add(ValidationError(translate("massUpdate.error.fieldToDeleteNotEmpty"), "$field.textValue"))
      } else {
        return true // Modification action.
      }
    }
    return false
  }

  protected open fun proceedMassUpdate(
    request: HttpServletRequest,
    params: Map<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>
  ): ResponseEntity<*> {
    return RestUtils.badRequest("not yet implemented.")
  }

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
    variables: MutableMap<String, Any>
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
    val param = MassUpdateParameter()
    param.delete = false
    massUpdateData[field] = param
    UIRow().let { row ->
      row.add(UICol(md = 8).add(el))
      val optionsRow = UIRow()
      row.add(UICol(md = 4).add(optionsRow))
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

  protected fun showSuccessToast(numberOfEntries: Int): ResponseEntity<ResponseAction> {
    return UIToast.createToastResponseEntity(
      translateMsg(
        "massUpdate.success",
        NumberFormatter.format(numberOfEntries)
      )
    )
  }

  /**
   * Show toast after executing mass update (if no validation error was found).
   */
  protected fun showToast(numberOfUpdatedEntries: Int): ResponseEntity<ResponseAction> {
    return UIToast.createToastResponseEntity(
      translateMsg(
        "massUpdate.success",
        NumberFormatter.format(numberOfUpdatedEntries)
      )
    )
  }

  companion object {
    const val URL_PATH_SELECTED = "selected"
    const val URL_SUFFIX_SELECTED = "Selected"

    /**
     * @param param If append value is true, the given textValue of param will be appended to the oldValue (if textValue isn't already
     * contained in oldValue, otherwise null is returned).
     * @return The new Value to set or null, if no modification should done..
     */
    fun getNewTextValue(oldValue: String?, param: MassUpdateParameter?): String? {
      param ?: return null
      if (param.delete == true && param.append == true) {
        // Can't append AND delete text.
        return oldValue
      }
      if (param.delete == true) {
        return if (param.textValue.isNullOrBlank()) {
          // Whole field should be empty:
          ""
        } else {
          // Only occurence of textValue should be deleted:
          // Delete full line:
          oldValue?.replace("""\n\s*${param.textValue}\s*\n""".toRegex(RegexOption.IGNORE_CASE), "\n")
            // Delete occurrence including leading spaces:
            ?.replace("""\s*${param.textValue}""".toRegex(RegexOption.IGNORE_CASE), "")
        }
      }
      param.textValue.let { newValue ->
        if (newValue.isNullOrBlank()) {
          return null // Nothing to do.
        }
        if (param.append != true || oldValue.isNullOrBlank()) {
          return param.textValue // replace oldValue by this value.
        }
        return if (!oldValue.contains(newValue.trim(), true)) {
          "$oldValue\n$newValue" // Append new value.
        } else {
          null // Leave it untouched, because the new value is already contained in old value.
        }
      }
    }

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
      val param = params[property] ?: return
      if (param.delete == true && param.append == true) {
        // Can't append AND delete text.
        return
      }
      val oldValue = BeanHelper.getProperty(data, property) as String?
      getNewTextValue(oldValue, param)?.let { newValue ->
        BeanHelper.setProperty(data, property, newValue)
      }
    }
  }
}
