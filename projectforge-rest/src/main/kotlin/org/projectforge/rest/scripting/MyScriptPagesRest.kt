/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.jcr.AttachmentsDaoAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.jcr.FileSizeStandardChecker
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Script
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.projectforge.ui.UITableColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
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
  i18nKeyPrefix = "scripting.myScript"
) {
  @Autowired
  private lateinit var scriptPagesRest: ScriptPagesRest

  @PostConstruct
  private fun postConstruct() {
    val maxFileSize = attachmentsService.maxDefaultFileSize.toBytes()
    val maxFileSizeSpringProperty = AttachmentsService.MAX_DEFAULT_FILE_SIZE_SPRING_PROPERTY

    this.jcrPath =  scriptPagesRest.jcrPath
    this.attachmentsAccessChecker = AttachmentsDaoAccessChecker(
      baseDao, jcrPath, null, FileSizeStandardChecker(maxFileSize, maxFileSizeSpringProperty)
    )
  }

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
   * Don't show include scripts, because the user can't do anything with it.
   */
  override fun filterList(resultSet: MutableList<ScriptDO>, filter: MagicFilter): List<ScriptDO> {
    return resultSet.filter { !it.isDeleted && it.type != ScriptDO.ScriptType.INCLUDE }
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
    layout.add(UITable.createUIResultSetTable()
          .add(lc, "name", "description")
          .add(UITableColumn("parameterNames", title = "scripting.script.parameter", sortable = false))
          .add(UITableColumn("type", title = "scripting.script.type"))
          .add(lc, "lastUpdate")
      )
    layout.userAccess.insert = false
  }

  /**
   * @return the execution page.
   */
  override fun getStandardEditPage(): String {
    return "${PagesResolver.getDynamicPageUrl(MyScriptExecutePageRest::class.java)}:id"
  }
}
