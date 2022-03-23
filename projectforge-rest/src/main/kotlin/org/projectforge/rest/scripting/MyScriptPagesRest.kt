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
import org.projectforge.business.scripting.MyScriptDao
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Script
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.projectforge.ui.UITableColumn
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * These script pages are usable by all users. They have only access to some script list and their execution, if the
 * are allowed by the script configuration.
 * @see ScriptDO.executableByUserIds
 * @see ScriptDO.executableByGroupIds
 */
@RestController
@RequestMapping("${Rest.URL}/myscript")
class MyScriptPagesRest : AbstractDTOPagesRest<ScriptDO, Script, MyScriptDao>(
  baseDaoClazz = MyScriptDao::class.java,
  i18nKeyPrefix = "scripting.myscript.title"
) {
  override fun newBaseDO(request: HttpServletRequest?): ScriptDO {
    val script = ScriptDO()
    script.type = ScriptDO.ScriptType.KOTLIN
    return script
  }

  override fun transformForDB(dto: Script): ScriptDO {
    return ScriptDO() // Not needed, no modifications will be done in the data base.
  }

  override fun transformFromDB(obj: ScriptDO, editMode: Boolean): Script {
    val script = Script()
    script.filename = obj.filename
    script.copyFrom(obj)
    return script
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(): UILayout {
    val layout = super.createListLayout()
      .add(
        UITable.createUIResultSetTable()
          .add(lc, "name", "description")
          .add(UITableColumn("type", title = "scripting.script.type"))
          .add(lc, "lastUpdate")
      )
    layout.userAccess.insert = false
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * @return the execution page.
   */
  override fun getStandardEditPage(): String {
    return "${PagesResolver.getDynamicPageUrl(ScriptExecutePageRest::class.java)}:id"
  }
}
