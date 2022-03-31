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
import org.projectforge.common.i18n.UserException
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
    val layout = getLayout(request, massUpdateData)
    LayoutUtils.process(layout)

    layout.postProcessPageMenu()

    return FormLayoutData(massUpdateData, layout, createServerData(request))
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
      return showValidationErrors(ValidationError(translateMsg(BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, BaseDao.MAX_MASS_UPDATE)))
    }
    val params = postData.data
    var nothingToDo = true
    val validationErrors = mutableListOf<ValidationError>()
    params.forEach { (key, param) ->
      if (param.isEmpty()) {
        if (param.delete == true) {
          nothingToDo = false
        }
      } else {
        if (param.delete == true) {
          validationErrors.add(ValidationError(translate("massUpdate.error.fieldToDeleteNotEmpty"), "$key.textValue"))
        } else {
          nothingToDo = false
        }
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

  open protected fun proceedMassUpdate(
    request: HttpServletRequest,
    params: Map<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>
  ): ResponseEntity<*> {
    return RestUtils.badRequest("not yet implemented.")
  }

  abstract fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>
  )

  protected fun getLayout(
    request: HttpServletRequest,
    massUpdateData: MutableMap<String, MassUpdateParameter>
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

    fillForm(request, layout, massUpdateData)

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
  protected fun createTextFieldRow(
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
    val param = MassUpdateParameter()
    param.delete = false
    massUpdateData[field] = param
    UIRow().let { row ->
      row.add(UICol(md = 8).add(el))
      if (elementInfo?.required != true) {
        row.add(
          UICol(md = 4).add(
            UICheckbox(
              "$field.delete",
              label = "massUpdate.field.checkbox4deletion",
              // Doesn't work: tooltip = "massUpdate.field.checkbox4deletion.info",
            )
          )
        )
      }
      return row
    }
  }

  /**
   * @param minLengthOfTextArea See [LayoutUtils.buildLabelInputElement]
   */
  protected fun createAndAddFields(
    lc: LayoutContext,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    container: IUIContainer,
    vararg fields: String,
    minLengthOfTextArea: Int = LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
  ) {
    fields.forEach { field ->
      container.add(createTextFieldRow(lc, field, massUpdateData, minLengthOfTextArea))
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
     * @param append If true, the given textValue of param will be appended to the oldValue (if textValue isn't already
     * contained in oldValue, otherwise null is returned).
     * @return The new Value to set or null, if no modification should done..
     */
    fun getNewTextValue(oldValue: String?, param: MassUpdateParameter?, append: Boolean = false): String? {
      param ?: return null
      if (param.delete == true) {
        return if (param.textValue.isNullOrBlank()) {
          ""
        } else {
          null // Parameter should be deleted, but a text value was given.
        }
      }
      param.textValue.let { newValue ->
        if (newValue.isNullOrBlank()) {
          return null // Nothing to do.
        }
        if (!append || oldValue.isNullOrBlank()) {
          return param.textValue // replace oldValue by this value.
        }
        return if (oldValue.contains(newValue.trim(), true) != true) {
          "$oldValue\n$newValue" // Append new value.
        } else {
          null // Leave it untouched, because the new value is already contained in old value.
        }
      }
    }

    fun processTextParameter(
      data: Any,
      property: String,
      params: Map<String, MassUpdateParameter>,
      append: Boolean = false
    ) {
      val param = params[property] ?: return
      val oldValue = BeanHelper.getProperty(data, property) as String?
      getNewTextValue(oldValue, param, append)?.let { newValue ->
        BeanHelper.setProperty(data, property, newValue)
      }
    }
  }
}
