/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin.rest

import de.micromata.merlin.word.templating.VariableType
import org.jetbrains.kotlin.psi.postfixExpressionVisitor
import org.projectforge.framework.i18n.translate
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.merlin.MerlinTemplate
import org.projectforge.plugins.merlin.MerlinVariable
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

/**
 * Modal dialog showing details of an attachment with the functionality to download, modify and delete it.
 */
@RestController
@RequestMapping("${Rest.URL}/merlinvariables")
class MerlinVariablePageRest : AbstractDynamicPageRest() {
  /**
   * Returns the form for a single attachment, including file properties as well as editable properties such
   * as file name and description.
   * The form supports also the buttons: download, delete and update.
   * The react path of this should look like: 'react/attachment/dynamic/42?category=contract...'
   * @param id: Id of data object with attachments.
   */
  @GetMapping("dynamic")
  fun getForm(
    @RequestParam("id", required = true) id: Int,
    request: HttpServletRequest
  ): FormLayoutData {
    val dto = ExpiringSessionAttributes.getAttribute(request.session, "${this::class.java.name}:$id")
    if (dto == null || dto !is MerlinTemplate) {
      throw InternalError("Please try again.")
    }
    return FormLayoutData(dto, createEditLayout(dto), createServerData(request))
  }

  /**
   * For editing variables. Must be called first for storing current state of all variables in [ExpiringSessionAttributes]
   * before redirecting to dynamic page (where the values will get from the session).
   * Post to dynamic page isn't yet available, so this workaround is needed.
   */
  @PostMapping("edit/{variableId}")
  fun openEditModal(
    @PathVariable("variableId", required = true) variableId: Int,
    @Valid @RequestBody dto: MerlinTemplate, request: HttpServletRequest
  ): ResponseEntity<*> {
    dto.currentVariable =
      getCurrentVariable(dto, variableId) ?: throw InternalError("Variable with given id not found.")
    // 10 minutes (if user reloads page)
    ExpiringSessionAttributes.setAttribute(request.session, "${this::class.java.name}:${dto.id}", dto, 10)
    return ResponseEntity.ok()
      .body(
        ResponseAction(
          PagesResolver.getDynamicPageUrl(
            this::class.java,
            id = dto.id,
            absolute = true
          ), targetType = TargetType.MODAL
        )
      )
  }

  /**
   * User wants to save changes of variable (currentVariable of posted dto).
   */
  @PostMapping("update")
  fun update(
    @Valid @RequestBody data: PostData<MerlinTemplate>, request: HttpServletRequest
  ): ResponseEntity<*> {
    val dto = data.data
    val currentVariable = dto.currentVariable ?: throw IllegalArgumentException("No current variable found. Nothing to update.")
    val dest = getCurrentVariable(dto, dto.currentVariable?.id) ?: throw IllegalArgumentException("No current variable found. Nothing to update.")
    if (currentVariable.name != dest.name) {
      throw InternalError("Oups, name of current variable and variable to change in list differs!")
    }
    dest.copyFrom(currentVariable)
    dto.reorderVariables()
    val result = MerlinPagesRest.updateLayoutAndData(dto = dto, userAccess = UILayout.UserAccess(true, true, true, true))
    val ui = result.first
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(dto.variables, dto.dependentVariables, "Hurzel"))
          .addVariable("ui", ui)
      )
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<MerlinTemplate>): ResponseEntity<ResponseAction> {
    val dto = postData.data
    // write access is always true, otherwise watch field wasn't registered.
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable(
          "ui",
          createEditLayout(dto)
        )
        .addVariable("data", dto)
    )
  }

  private fun createEditLayout(dto: MerlinTemplate): UILayout {
    val variable = dto.currentVariable ?: throw InternalError("No current variable given to edit.")
    val lc = LayoutContext(MerlinVariable::class.java)

    val leftCol = UICol(UILength(md = 6))
      .add(UIReadOnlyField("currentVariable.name", lc))
    if (!variable.dependent) {
      leftCol.add(
        UISelect(
          "currentVariable.type",
          lc,
          values = VariableType.values()
            .map { UISelectValue(it, translate("plugins.merlin.variable.type.${it.name}")) })
      )
    }
    val rightCol = UICol(UILength(md = 6))
    if (variable.masterVariable != true) {
      val dependsOnCandidates = dto.variables.filter { it.id != variable.id && !it.allowedValues.isNullOrEmpty() }
        .map { UISelectValue(it.name, it.name) }
      rightCol.add(
        UISelect(
          "currentVariable.dependsOn",
          lc,
          values = dependsOnCandidates
        )
      )
    }

    val fieldset = UIFieldset(UILength(md = 12, lg = 12))
    fieldset.add(
      UIRow()
        .add(leftCol)
        .add(rightCol)
    )
    if (!variable.dependent) {
      rightCol.add(UICheckbox("currentVariable.required", lc))
      rightCol.add(UICheckbox("currentVariable.unique", lc))

      when (variable.type) {
        VariableType.STRING -> {
          fieldset.add(UICreatableSelect("currentVariable.allowedValues", lc))
        }
        VariableType.FLOAT, VariableType.INT -> {
          val dataType = if (variable.type == VariableType.FLOAT) UIDataType.DECIMAL else UIDataType.INT
          fieldset.add(
            UIRow().add(
              UICol(md = 6)
                .add(UIInput("minimumValue", lc, dataType = dataType))
            ).add(
              UICol(md = 6)
                .add(UIInput("maximumValue", lc, dataType = dataType))
            )
          )
        }
      }

      fieldset.add(
        UITextArea("currentVariable.description", lc)
      )
    } else {
      fieldset.add(UITextArea("currentVariable.mappingText", lc))
    }

    val layout = UILayout("plugins.merlin.variable.edit")
      .add(fieldset)
    layout.watchFields.clear()
    layout.watchFields.addAll(arrayOf("currentVariable.type", "currentVariable.dependsOn"))
    layout.addAction(
      UIButton(
        "cancel",
        translate("cancel"),
        UIColor.DANGER,
        responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL)
      )
    )
    layout.addAction(
      UIButton(
        "update",
        translate("update"),
        UIColor.SUCCESS,
        responseAction = ResponseAction(
          RestResolver.getRestUrl(this::class.java, "update"),
          targetType = TargetType.POST
        ),
        default = true
      )
    )
    LayoutUtils.process(layout)
    return layout
  }

  /**
   * @return Pair of path of variable (e. g. variables[1] or dependentVariables[12]) and found variable or null, if not found.
   */
  private fun getCurrentVariable(dto: MerlinTemplate, currentVariableId: Int?): MerlinVariable? {
    currentVariableId ?: return null
    dto.variables.find { it.id == currentVariableId }?.let {
      return it
    }
    dto.dependentVariables.find { it.id == currentVariableId }?.let {
      return it
    }
    return null
  }

  class ResponseData(val variables: List<MerlinVariable>, val dependentVariables: List<MerlinVariable>, val name: String)
}
