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

package org.projectforge.rest.scripting

import mu.KotlinLogging
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
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

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/scriptExecute")
class ScriptExecutePageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var scriptDao: ScriptDao

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = NumberHelper.parseInteger(idString) ?: throw IllegalArgumentException("id not given.")
    val scriptDO = scriptDao.getById(id) ?: throw IllegalArgumentException("Script not found.")
    val layout = UILayout("scripting.script.execute")

    layout.add(
      UIButton(
        "back",
        translate("back"),
        UIColor.SUCCESS,
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(ScriptPagesRest::class.java, absolute = true),
          targetType = TargetType.REDIRECT
        ),
      )
    )

    layout.add(
      UIButton(
        "execute",
        translate("execute"),
        UIColor.DANGER,
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(ScriptPagesRest::class.java, absolute = true),
          targetType = TargetType.REDIRECT
        ),
        default = true
      )
    )

    layout.add(
      MenuItem(
        "EDIT",
        i18nKey = "scripting.title.edit",
        url = PagesResolver.getEditPageUrl(ScriptPagesRest::class.java, id),
        type = MenuItemTargetType.REDIRECT
      )
    )
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(scriptDO, layout, createServerData(request))
  }
}
