/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.scripting.AbstractScriptDao
import org.projectforge.business.scripting.MyScriptDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.DownloadFileSupport
import org.projectforge.rest.dto.Script
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The execution of the scripts by all users the script configuration allows to (see [MyScriptDao]). Stored
 * scripts only: the user sees only their name, description and parameters, never their code.
 */
@RestController
@RequestMapping("${Rest.URL}/myScriptExecute")
class MyScriptExecuteRest : AbstractScriptExecuteRest() {
    /** The execution form: the script and the file of the user's last execution. */
    class ExecuteForm(
        val script: Script,
        val download: DownloadFileSupport.Download?,
    )

    @Autowired
    private lateinit var myScriptDao: MyScriptDao

    @Autowired
    override lateinit var entityRest: MyScriptEntityRest

    override val scriptDao: AbstractScriptDao
        get() = myScriptDao

    @AccessChecked("DAO: find of MyScriptDao (executableByUserIds/executableByGroupIds)")
    @GetMapping("load")
    fun load(request: HttpServletRequest, @RequestParam("id") id: Long): ExecuteForm {
        val scriptDO = myScriptDao.find(id) ?: throw IllegalArgumentException("Script not found.")
        val script = createUserView(scriptDO)
        prefillFromRecentCall(script, scriptDO)
        return ExecuteForm(script, getDownload(request))
    }
}
