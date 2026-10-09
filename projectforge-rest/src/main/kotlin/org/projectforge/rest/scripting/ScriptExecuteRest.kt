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
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.scripting.AbstractScriptDao
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.admin.LogViewerRest
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
 * The execution of the scripts by financial and controlling staff: a stored script or, without an id, ad-hoc
 * code typed in the editor (optionally starting with one of the [ExampleScripts]).
 */
@RestController
@RequestMapping("${Rest.URL}/scriptExecute")
class ScriptExecuteRest : AbstractScriptExecuteRest() {
    /**
     * The execution form: the script, the file of the user's last execution and the log viewer of the
     * scripting loggers (a subscription of the user's own).
     */
    class ExecuteForm(
        val script: Script,
        val download: DownloadFileSupport.Download?,
        val logViewerUrl: String,
    )

    @Autowired
    private lateinit var baseDao: ScriptDao

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    override lateinit var entityRest: ScriptEntityRest

    override val scriptDao: AbstractScriptDao
        get() = baseDao

    override val allowAdHoc = true

    /**
     * @param id The stored script to execute, or null for ad-hoc code.
     * @param example The index of an [ExampleScripts] to start the ad-hoc code with.
     */
    @AccessChecked("DAO: find of ScriptDao (FINANCE/CONTROLLING group); ad-hoc: getVariableNames only")
    @GetMapping("load")
    fun load(
        request: HttpServletRequest,
        @RequestParam("id", required = false) id: Long?,
        @RequestParam("example", required = false) example: Int?,
    ): ExecuteForm {
        val script = Script()
        if (id != null) {
            val scriptDO = baseDao.find(id) ?: throw IllegalArgumentException("Script not found.")
            script.copyFrom(scriptDO)
            prefillFromRecentCall(script, scriptDO)
            script.executableByEmails = getExecutableByEmails(script)
        } else {
            // Ad-hoc code: no stored script to check, so the access to the scripts in general.
            baseDao.checkLoggedInUserSelectAccess()
            // Kotlin, as the examples are: without a type the executor guesses by the code and takes
            // anything not starting with val/var/fun for Groovy (see ScriptExecutor.getScriptType).
            script.type = ScriptDO.ScriptType.KOTLIN
            script.availableVariables =
                scriptExecution.getVariableNames(script, script.parameters, baseDao, entityRest).joinToString()
            if (example != null && example in ExampleScripts.exampleFiles.indices) {
                script.script = ExampleScripts.loadScript(example)
            }
        }
        return ExecuteForm(script, getDownload(request), LogViewerRest.viewerUrl(ensureUserLogSubscription().id))
    }

    /**
     * The e-mail addresses of all users allowed to execute the script (given by group and/or as user).
     */
    private fun getExecutableByEmails(script: Script): String {
        val executableByMails = mutableSetOf<String>()
        script.executableByUsers?.forEach {
            UserGroupCache.getInstance().getUser(it.id)?.email?.let { email ->
                if (email.isNotBlank()) {
                    executableByMails.add(email)
                }
            }
        }
        script.executableByGroups?.mapNotNull { it.id }?.let {
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
        return executableByMails.joinToString()
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
