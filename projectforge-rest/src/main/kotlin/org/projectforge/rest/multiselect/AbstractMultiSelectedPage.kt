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

import mu.KotlinLogging
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.NumberFormatter
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

private val log = KotlinLogging.logger {}

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
    return RestUtils.badRequest("tbd")
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
    if (selectedIds.isNullOrEmpty()) {
      layout.add(UIAlert("massUpdate.error.noEntriesSelected", color = UIColor.DANGER))
    } else {
      layout.add(
        UIAlert(
          "'${translateMsg("massUpdate.entriesFound", NumberFormatter.format(selectedIds.size))}",
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
        )
      )
    }
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

  protected fun createTextFieldRow(
    lc: LayoutContext,
    field: String,
    massUpdateData: MutableMap<String, MassUpdateParameter>
  ): UIRow {
    val el = LayoutUtils.buildLabelInputElement(lc, field)
    if (el is UIInput) {
      el.id = when (el.dataType) {
        UIDataType.DATE -> "$field.dateValue"
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
    param.checked = false
    massUpdateData[field] = param
    UIRow().let { row ->
      row.add(UICol(md = 8).add(el))
      if (elementInfo?.required != true) {
        row.add(
          UICol(md = 4).add(
            UICheckbox(
              "$field.checked",
              label = "massUpdate.field.checkbox4deletion",
              // Doesn't work: tooltip = "massUpdate.field.checkbox4deletion.info",
            )
          )
        )
      }
      return row
    }
  }

  protected fun createAndAddFields(
    lc: LayoutContext,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    container: IUIContainer,
    vararg fields: String
  ) {
    fields.forEach { field ->
      container.add(createTextFieldRow(lc, field, massUpdateData))
    }
  }

  companion object {
    const val URL_PATH_SELECTED = "selected"
    const val URL_SUFFIX_SELECTED = "Selected"
  }

}
