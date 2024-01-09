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
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.Script
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/myScriptExecute")
class MyScriptExecutePageRest : AbstractScriptExecutePageRest() {
  @Autowired
  private lateinit var myScriptDao: MyScriptDao

  @Autowired
  override lateinit var pagesRest: MyScriptPagesRest

  override val accessCheckOnExecute: Boolean = false

  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("id") idString: String?,
  ): FormLayoutData {
    var scriptDO: ScriptDO? = null
    val origScript = Script()
    val id = idString?.toIntOrNull() ?: throw IllegalArgumentException("Script not found.")
    scriptDO = scriptDao.getById(id) ?: throw IllegalArgumentException("Script not found.")
    origScript.copyFrom(scriptDO) // Don't export all fields to the user
    val script = Script()
    script.id = origScript.id
    script.name = origScript.name
    script.description = origScript.description
    script.copyParametersFrom(origScript)
    script.type = origScript.type
    val variables = mutableMapOf<String, Any>()
    val layout = getLayout(request, script, variables, scriptDO)
    return FormLayoutData(script, layout, createServerData(request), variables)
  }

  @PostConstruct
  private fun postConstruct() {
    this.scriptDao = myScriptDao
  }
}
