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
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.plugins.merlin.*
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/merlinexecution")
class MerlinExecutionPageRest : AbstractDynamicPageRest() {

  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  @Autowired
  private lateinit var merlinRunner: MerlinRunner

  @Autowired
  private lateinit var merlinPagesRest: MerlinPagesRest

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = NumberHelper.parseInteger(idString) ?: throw IllegalAccessException("Parameter id not an int.")
    val stats = merlinRunner.getStatistics(id)
    val dbObj = merlinTemplateDao.getById(id)
    val dto = merlinPagesRest.transformFromDB(dbObj)
    val variablesFieldset = UIFieldset(title = "variables")
    stats.variables.filter { it.input }.sortedBy { it.number ?: Integer.MAX_VALUE }.forEach {
      variablesFieldset.add(createInputElement(it))
    }
    val layout = UILayout("plugins.merlin.templateExecutor.heading")
      .add(UIReadOnlyField("todo", label = "validation of min/max value"))
      .add(variablesFieldset)
    layout.add(
      UIButton(
        "back",
        translate("back"),
        UIColor.SUCCESS,
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(
            MerlinPagesRest::class.java,
            absolute = true
          ), targetType = TargetType.REDIRECT
        ),
        default = true
      )
    )
    if (hasEditAccess(dto, dbObj)) {
      layout.add(
        MenuItem(
          "EDIT",
          i18nKey = "plugins.merlin.title.edit",
          url = PagesResolver.getEditPageUrl(MerlinPagesRest::class.java, dto.id),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(dto, layout, createServerData(request))
  }

  private fun createInputElement(variable: MerlinVariable): UIElement {
    val definition = variable.definition!!
    val dataType = when (definition.type) {
      VariableType.DATE -> UIDataType.DATE
      VariableType.FLOAT -> UIDataType.DECIMAL
      VariableType.INT -> UIDataType.INT
      else -> UIDataType.STRING
    }
    val allowedValuesList = definition.allowedValuesList
    if (allowedValuesList.isNullOrEmpty()) {
      return UIInput(variable.name, label = "'${variable.name}", dataType = dataType, required = definition.isRequired)
    }
    val values = allowedValuesList.map { UISelectValue(it, "$it") }
    return UISelect("excelDateFormat", label = "'${variable.name}", required = definition.isRequired, values = values)
  }

  /**
   * @return true, if the area isn't a personal box and the user has write access.
   */
  private fun hasEditAccess(dto: MerlinTemplate, dbObj: MerlinTemplateDO): Boolean {
    return merlinTemplateDao.hasLoggedInUserUpdateAccess(dbObj, dbObj, false)
  }
}
