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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.scripting.AbstractScriptDao
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptLogger
import org.projectforge.common.DateFormatType
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.DownloadFileSupport
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.dto.Script
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * The layout-free base of the script execution, serving the hand-built projectforge-next execution page
 * (see components/features/script): the form is loaded by the `load` endpoint of the subclasses, executed
 * by [execute], its log polled by [refresh] while it runs and its result file fetched by [download].
 *
 * A stored script is always executed as it is stored: of the posted script only the id and the values of
 * its parameters are used (see [prepareScript]). Only [allowAdHoc] executes code posted by the client.
 */
abstract class AbstractScriptExecuteRest {
    class LogEntry(timestamp: Date, val level: LogLevel, val message: String) {
        val timestamp: String = PFDateTime.from(timestamp).format(DateFormatType.DATE_TIME_SECONDS)
        val levelAsString = translate(level.i18nKey)
    }

    /**
     * The outcome of an execution: [result] is Markdown (the script's own result, its exception or a
     * standard message), [download] the file the script produced, if any.
     */
    class ExecutionResult(
        val result: String,
        val hasErrors: Boolean,
        val download: DownloadFileSupport.Download? = null,
    )

    @Autowired
    protected lateinit var scriptExecution: ScriptExecution

    protected abstract val scriptDao: AbstractScriptDao

    protected abstract val entityRest: AbstractEntityRest<*, *, *>

    /** Whether code posted by the client may be executed (the ad-hoc editor of the script administration). */
    protected open val allowAdHoc = false

    /**
     * Fills the parameters of a stored script with the values of the user's recent call of it, keeping the
     * names and descriptions of the script itself.
     */
    protected fun prefillFromRecentCall(script: Script, scriptDO: ScriptDO) {
        scriptExecution.updateFromRecentCall(script)
        val params = listOf(script.parameter1, script.parameter2, script.parameter3, script.parameter4, script.parameter5, script.parameter6)
        val names = listOf(scriptDO.parameter1Name, scriptDO.parameter2Name, scriptDO.parameter3Name, scriptDO.parameter4Name, scriptDO.parameter5Name, scriptDO.parameter6Name)
        val descriptions = listOf(
            scriptDO.parameter1Description, scriptDO.parameter2Description, scriptDO.parameter3Description,
            scriptDO.parameter4Description, scriptDO.parameter5Description, scriptDO.parameter6Description,
        )
        params.forEachIndexed { index, param ->
            param?.name = names[index]
            param?.description = descriptions[index]
        }
    }

    /**
     * The outcome of the user's last execution of the script (null: ad-hoc code), kept in the session as
     * long as its log and its file, so the page shows them again when the user comes back. Without the file:
     * that is [getDownload]'s, the user's last of all scripts.
     */
    protected fun getLastExecution(request: HttpServletRequest, scriptId: Long?): ExecutionResult? {
        return ExpiringSessionAttributes.getAttribute(
            request.getSession(false),
            getResultSessionAttr(scriptId),
            ExecutionResult::class.java,
        )
    }

    /** The file of the user's last execution, still available for download. */
    protected fun getDownload(request: HttpServletRequest): DownloadFileSupport.Download? {
        return scriptExecution.getDownloadFile(request)?.let { DownloadFileSupport.Download(it) }
    }

    @AccessChecked("DAO: find/execute of the script DAO (ScriptDao: FINANCE/CONTROLLING, MyScriptDao: executableBy*); ad-hoc code only with allowAdHoc")
    @PostMapping("execute")
    fun execute(request: HttpServletRequest, @RequestBody posted: Script): ExecutionResult {
        val script = prepareScript(posted)
        val parameters = script.parameters
        val scriptLogger = ScriptLogger()
        val session = request.getSession(false)
        // Store the scriptLogger in user's session to show the log entries in the UI.
        ExpiringSessionAttributes.setAttribute(session, getSessionAttr(script.id), scriptLogger, SESSION_EXPIRY_MINUTES)
        val result = scriptExecution.execute(request, script, parameters, scriptDao, entityRest, scriptLogger)
        val output = StringBuilder()
        if (result.exception == null && result.result is Exception) {
            result.exception = result.result as? Exception
        }
        result.exception?.let { ex ->
            output.appendLine("${ex::class.java.name}:")
            output.appendLine("```") // Code
            output.appendLine(ex.message)
            output.appendLine("```") // Code
        }
        result.downloadAvailable?.let {
            output.appendLine(it)
        }
        result.result?.let {
            if (it is String) {
                output.appendLine(it)
            }
        }
        if (output.isEmpty()) {
            if (result.scriptLogger.hasErrors) {
                output.appendLine(translate("scripting.script.execution.log.terminatedWithErrors"))
            } else {
                output.appendLine(translate("scripting.script.execution.log.successfullyCompleted"))
            }
        }
        val hasErrors = result.exception != null || result.scriptLogger.hasErrors
        // Kept as long as the file (counted from now), the log renewed for the same time:
        ExpiringSessionAttributes.setAttribute(session, getSessionAttr(script.id), scriptLogger, SESSION_EXPIRY_MINUTES)
        ExpiringSessionAttributes.setAttribute(
            session,
            getResultSessionAttr(script.id),
            ExecutionResult(output.toString(), hasErrors),
            SESSION_EXPIRY_MINUTES,
        )
        return ExecutionResult(
            result = output.toString(),
            hasErrors = hasErrors,
            // Only the file of this execution, not one left over from an earlier one:
            download = if (result.downloadAvailable != null) getDownload(request) else null,
        )
    }

    /**
     * The log entries of the user's running or last execution of the script.
     * @param scriptId The script ID, or null if only code is executed and the script isn't persisted.
     *                 scriptId is used to find the log entries in the user's session.
     */
    @AccessChecked("Own session only: the log of the user's own execution")
    @GetMapping("refresh")
    fun refresh(request: HttpServletRequest, @RequestParam("id", required = false) scriptId: Long?): List<LogEntry> {
        val scriptLogger = ExpiringSessionAttributes.getAttribute(
            request.getSession(false),
            getSessionAttr(scriptId),
            ScriptLogger::class.java,
        ) ?: return emptyList()
        return scriptLogger.messages.map {
            LogEntry(it.timestamp, it.level, it.message ?: "")
        }
    }

    @AccessChecked("Own session only: the file of the user's own execution")
    @GetMapping("download")
    fun download(request: HttpServletRequest): ResponseEntity<*> {
        val downloadFile = scriptExecution.getDownloadFile(request)
            ?: return RestUtils.badRequest(translate("download.expired"))
        log.info { "Downloading '${downloadFile.filename}' of size ${downloadFile.sizeHumanReadable}." }
        return RestUtils.downloadFile(downloadFile.filename, downloadFile.bytes)
    }

    /**
     * The script to execute: a stored one as stored (select access checked by the DAO), filled with the
     * posted parameter values; the posted one only for an ad-hoc execution.
     */
    private fun prepareScript(posted: Script): Script {
        val id = posted.id
        if (id == null) {
            if (!allowAdHoc) {
                throw IllegalArgumentException("Script id required.")
            }
            // Ad-hoc code is Kotlin unless the client says otherwise (see ScriptExecuteRest.load).
            posted.type = posted.type ?: ScriptDO.ScriptType.KOTLIN
            return posted
        }
        val scriptDO = scriptDao.find(id) ?: throw IllegalArgumentException("Script not found.")
        val script = createUserView(scriptDO)
        mergeValues(script.parameter1, posted.parameter1)
        mergeValues(script.parameter2, posted.parameter2)
        mergeValues(script.parameter3, posted.parameter3)
        mergeValues(script.parameter4, posted.parameter4)
        mergeValues(script.parameter5, posted.parameter5)
        mergeValues(script.parameter6, posted.parameter6)
        // Only a page the script is configured for (see ScriptDO.pageTargets):
        script.pageTarget = posted.pageTarget?.takeIf { scriptDO.pageTargetList.contains(it) }
        return script
    }

    private fun mergeValues(dest: Script.Param?, src: Script.Param?) {
        dest ?: return
        src ?: return
        dest.stringValue = src.stringValue
        dest.intValue = src.intValue
        dest.decimalValue = src.decimalValue
        dest.booleanValue = src.booleanValue
        dest.dateValue = src.dateValue
        dest.toDateValue = src.toDateValue
        dest.userValue = src.userValue
        dest.taskValue = src.taskValue
    }

    companion object {
        /**
         * What an executing user gets to see of a script: its name, description, type and parameters — not
         * its code, its access configuration or its attachments.
         */
        fun createUserView(obj: ScriptDO): Script {
            val full = Script()
            full.copyFrom(obj)
            val script = Script(name = obj.name, type = obj.type, description = obj.description)
            script.id = obj.id
            script.deleted = obj.deleted
            script.created = obj.created
            script.lastUpdate = obj.lastUpdate
            script.copyParametersFrom(full)
            return script
        }

        /** How long the log and the result of an execution are kept in the session, as its file is. */
        private const val SESSION_EXPIRY_MINUTES = 5

        private fun getSessionAttr(scriptId: Long?): String {
            return "${AbstractScriptExecuteRest::class.simpleName}:$scriptId"
        }

        private fun getResultSessionAttr(scriptId: Long?): String {
            return "${getSessionAttr(scriptId)}:result"
        }
    }
}
