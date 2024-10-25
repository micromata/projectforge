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
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.Script
import org.projectforge.ui.UILayout
import org.projectforge.ui.UIReadOnlyField
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/scriptExecute")
class ScriptExecutePageRest : AbstractScriptExecutePageRest() {
  @Autowired
  private lateinit var baseDao: ScriptDao

  @Autowired
  private lateinit var groupService: GroupService

  @PostConstruct
  private fun postConstruct() {
    this.scriptDao = baseDao
  }

  @Autowired
  override lateinit var pagesRest: ScriptPagesRest

  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("id") idString: String?,
    @RequestParam("example") example: Int?
  ): FormLayoutData {
    var scriptDO: ScriptDO? = null
    val script = Script()
    var id: Long? = null
    var exampleIdx: Int? = null // fix, because idString is like ?example=#
    if (example != null) {
      exampleIdx = example
    } else if (idString?.startsWith("?example=") == true) {
      exampleIdx = idString.removePrefix("?example=").toIntOrNull()
    } else {
      id = NumberHelper.parseLong(idString)
    }
    if (id != null) {
      scriptDO = scriptDao.find(id) ?: throw IllegalArgumentException("Script not found.")
      script.copyFrom(scriptDO)
    } else {
      script.availableVariables =
        scriptExecution.getVariableNames(script, script.parameters, scriptDao, pagesRest).joinToString()
      if (exampleIdx != null) {
        script.script = ExampleScripts.loadScript(exampleIdx)
      }
    }
    val variables = mutableMapOf<String, Any>()
    val layout = getLayout(request, script, variables, scriptDO)
    return FormLayoutData(script, layout, createServerData(request), variables)
  }

  override fun onAfterLayout(layout: UILayout, script: Script, scriptDO: ScriptDO?) {
    if (scriptDO != null) {
      layout.add(
        MenuItem(
          "EDIT",
          i18nKey = "scripting.title.edit",
          url = PagesResolver.getEditPageUrl(
            ScriptPagesRest::
            class.java, scriptDO.id
          ),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }
    val executableByMails = mutableSetOf<String>()
    script.executableByUsers?.forEach { it ->
      val user = UserGroupCache.getInstance().getUser(it.id)
      user?.email?.let { email ->
        if (email.isNotBlank()) {
          executableByMails.add(email)
        }
      }
    }
    script.executableByGroups?.let { groups ->
      groups.mapNotNull { it.id }.let {
        if (it.isNotEmpty()) {
          groupService.getGroupUsers(it.toLongArray())?.forEach { user ->
            user.email?.let { email ->
              if (email.isNotBlank()) {
                executableByMails.add(email)
              }
            }
          }
        }
      }
    }
    script.executableByEmails = executableByMails.joinToString()
    if (executableByMails.isNotEmpty()) {
      layout.add(UIReadOnlyField("executableByEmails", label = "scripting.script.executableByUsers"))
    }
    val examplesMenu = MenuItem("examples", translate("scripting.script.examples"))
    layout.add(examplesMenu)
    ExampleScripts.exampleFiles.forEachIndexed { index, exampleScript ->
      examplesMenu.add(
        MenuItem(
          id = "example$index",
          title = exampleScript.title,
          url = PagesResolver.getDynamicPageUrl(
            ScriptExecutePageRest::class.java,
            params = mapOf("example" to index),
          ),
        )
      )
    }
    layout.add(
      MenuItem(
        "logViewer",
        i18nKey = "system.admin.logViewer.title",
        url = PagesResolver.getDynamicPageUrl(
          LogViewerPageRest::
          class.java, id = ensureUserLogSubscription().id
        ),
        type = MenuItemTargetType.REDIRECT,
      )
    )
  }

  private fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
    return LogSubscription.ensureSubscription(
      title = "Scripting",
      user = username,
      create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher("org.projectforge.rest.scripting", "org.projectforge.business.scripting"),
          maxSize = 10000,
        )
      })
  }
}
