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
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.business.scripting.*
import org.projectforge.common.DateFormatType
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.RestConfiguration
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Script
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*

private val log = KotlinLogging.logger {}

abstract class AbstractScriptExecutePageRest : AbstractDynamicPageRest() {
    class LogEntry(timestamp: Date, val level: LogLevel, val message: String) {
        val id = timestamp.time
        val timestamp: String = PFDateTime.from(timestamp).format(DateFormatType.DATE_TIME_SECONDS)
        val levelAsString = translate(level.i18nKey)
    }

    protected lateinit var scriptDao: AbstractScriptDao

    @Autowired
    private lateinit var restConfiguration: RestConfiguration

    @Autowired
    protected lateinit var scriptExecution: ScriptExecution

    protected abstract val pagesRest: AbstractPagesRest<*, *, *>

    protected open val accessCheckOnExecute = true

    protected fun getLayout(
        request: HttpServletRequest,
        script: Script,
        variables: MutableMap<String, Any>,
        scriptDO: ScriptDO?,
        executionResult: ScriptExecutionResult? = null
    ): UILayout {
        val layout = UILayout("scripting.script.execute")
        if (scriptDO != null) {
            // DB-Script execution
            scriptExecution.updateFromRecentCall(script)?.let { task ->
                // Task must be added to variables for displaying it:
                variables["task"] = task
            }
            // Update param names
            script.parameter1?.name = scriptDO.parameter1Name
            script.parameter2?.name = scriptDO.parameter2Name
            script.parameter3?.name = scriptDO.parameter3Name
            script.parameter4?.name = scriptDO.parameter4Name
            script.parameter5?.name = scriptDO.parameter5Name
            script.parameter6?.name = scriptDO.parameter6Name
            script.parameter1?.description = scriptDO.parameter1Description
            script.parameter2?.description = scriptDO.parameter2Description
            script.parameter3?.description = scriptDO.parameter3Description
            script.parameter4?.description = scriptDO.parameter4Description
            script.parameter5?.description = scriptDO.parameter5Description
            script.parameter6?.description = scriptDO.parameter6Description
            layout.add(UIReadOnlyField("name", label = "scripting.script.name"))
            layout.add(UIAlert(id = "description", title = "description", markdown = true, color = UIColor.LIGHT))
            UIFieldset(title = "scripting.script.parameter").let { fieldset ->
                layout.add(fieldset)
                addParameterInput(fieldset, script.parameter1, 1)
                addParameterInput(fieldset, script.parameter2, 2)
                addParameterInput(fieldset, script.parameter3, 3)
                addParameterInput(fieldset, script.parameter4, 4)
                addParameterInput(fieldset, script.parameter5, 5)
                addParameterInput(fieldset, script.parameter6, 6)
            }
        } else {
            // Editing and executing ad-hoc script
            layout.add(UIEditor("script", type = ScriptExecutor.getScriptType(script.script, script.type)))
                .add(UIReadOnlyField("availableVariables", label = "scripting.script.availableVariables"))
        }

        layout.add(
            UIButton.createBackButton(
                ResponseAction(
                    PagesResolver.getListPageUrl(pagesRest::class.java, absolute = true),
                    targetType = TargetType.REDIRECT
                ),
            )
        )

        layout.add(
            UIButton.createDefaultButton(
                "execute",
                title = "execute",
                responseAction = ResponseAction(
                    url = "${getRestPath()}/execute",
                    targetType = TargetType.POST
                )
            )
        )

        if (executionResult?.resultAsUserFriendlyString?.isNotBlank() == true) {
            layout.add(
                UIAlert(
                    executionResult.resultAsUserFriendlyString,
                    title = "scripting.script.result",
                    markdown = true,
                    color = if (executionResult.scriptLogger.hasErrors) {
                        UIColor.DANGER
                    } else {
                        UIColor.INFO
                    }
                )
            )
        }

        scriptExecution.getDownloadFile(request)?.let { downloadFile ->
            val download = DownloadFileSupport.Download(downloadFile)
            script.download = download
            layout.add(
                UIRow().add(
                    scriptExecution.downloadFileSupport.createDownloadFieldset(
                        "scripting.download.filename.info",
                        "${getRestPath()}/download",
                        download,
                    )
                )
            )
        }

        val method = if (restConfiguration.sseEnabled == true) UITable.RefreshMethod.SSE else UITable.RefreshMethod.GET
        val refreshUrl = if (method == UITable.RefreshMethod.GET) {
            "refresh/${script.id}"
        } else {
            "logs/${script.id}"
        }

        layout.add(
            UIRow().add(
                UITable(
                    "logging",
                    refreshUrl = RestResolver.getRestUrl(this::class.java, refreshUrl),
                    refreshIntervalSeconds = 2,
                    refreshMethod = method,
                ).also {
                    it.add(UITableColumn("timestamp", title = "time", sortable = false))
                    it.add(UITableColumn("levelAsString", title = "log.level", sortable = false))
                    it.add(UITableColumn("message", title = "message.title", sortable = false))
                }
            )
        )

        onAfterLayout(layout, script, scriptDO)
        LayoutUtils.process(layout)
        return layout
    }

    protected open fun onAfterLayout(layout: UILayout, script: Script, scriptDO: ScriptDO?) {
    }

    @PostMapping("execute")
    fun execute(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<Script>
    ): ResponseEntity<ResponseAction> {
        validateCsrfToken(request, postData)?.let { return it }
        val variables = mutableMapOf<String, Any>()
        val script = postData.data

        val parameters = script.parameters
        parameters.forEach { scriptParameter ->
            if (scriptParameter.type == ScriptParameterType.TASK) {
                scriptParameter.intValue?.let { taskId ->
                    TaskServicesRest.createTask(taskId)?.let { task ->
                        // Task must be added to variables for displaying it:
                        variables["task"] = task
                    }
                }
            }
        }
        if (!accessCheckOnExecute) {
            // If no accessCheckOnExecute, then at least check the select access of the actual script:
            scriptDao.find(script.id) // Throws exception if user is not financial or controlling staff member.
        }
        val scriptLogger = ScriptLogger()
        val session = request.getSession(false)
        // Store the scriptLogger in user's session to show the log entries in the UI.
        ExpiringSessionAttributes.setAttribute(session, getSessionAttr(script.id), scriptLogger, 5)
        val result = scriptExecution.execute(request, script, parameters, scriptDao, pagesRest, scriptLogger)
        val output = StringBuilder()
        output.append("'") // ProjectForge shouldn't try to find i18n-key.
        if (result.exception == null && result.result is Exception) {
            result.exception = result.result as? Exception
        }
        result.exception?.let { ex ->
            output.appendLine("---") // Horizontal rule
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
        if (output.length == 1) {
            if (result.scriptLogger.hasErrors) {
                output.appendLine(translate("scripting.script.execution.log.terminatedWithErrors"))
            } else {
                output.appendLine(translate("scripting.script.execution.log.successfullyCompleted"))
            }
        }
        result.resultAsUserFriendlyString = output.toString()
        val scriptDO = scriptDao.find(script.id) // null, if script.id is null.
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE, merge = true)
                .addVariable("data", script)
                .addVariable("ui", getLayout(request, script, variables, scriptDO, executionResult = result))
                .addVariable("variables", variables)
        )
    }

    /**
     * Refreshes the log viewer.
     * @param scriptId The script ID, or null if only code is executed and the script isn't persisted.
     *                 scriptId is used to find the log entries in the user's session.
     */
    @GetMapping("refresh/{scriptId}")
    fun refresh(request: HttpServletRequest, @PathVariable("scriptId") scriptId: Long)
            : List<LogEntry> {
        val scriptLogger = ExpiringSessionAttributes.getAttribute(
            request.getSession(false),
            getSessionAttr(scriptId),
            ScriptLogger::class.java,
        ) ?: return emptyList()
        return scriptLogger.messages.map {
            LogEntry(it.timestamp, it.level, it.message ?: "")
        }
    }

    @GetMapping("logs/{scriptId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamLogs(
        request: HttpServletRequest,
        @PathVariable("scriptId") scriptIdString: String?
    ): SseEmitter {
        val scriptId = scriptIdString?.toLongOrNull()
        object : SseEmitterTool() {
            var scriptLogger: ScriptLogger? = ExpiringSessionAttributes.getAttribute(
                request.getSession(false),
                getSessionAttr(scriptId),
                ScriptLogger::class.java,
            )

            override val lastModified: Date?
                get() = scriptLogger?.lastModified

            override fun getData(): Any? {
                return scriptLogger?.messages?.map {
                    LogEntry(it.timestamp, it.level, it.message ?: "")
                }
            }
        }.also {
            it.launch()
            return it.emitter
        }
    }

    @GetMapping("download")
    fun download(request: HttpServletRequest): ResponseEntity<*> {
        val downloadFile = scriptExecution.getDownloadFile(request)
            ?: return RestUtils.badRequest(translate("download.expired"))
        log.info("Downloading '${downloadFile.filename}' of size ${downloadFile.sizeHumanReadable}.")
        return RestUtils.downloadFile(downloadFile.filename, downloadFile.bytes)
    }

    private fun addParameterInput(parent: IUIContainer, parameter: Script.Param?, index: Int) {
        parameter?.type ?: return
        parent.add(UIRow().also { row ->
            val label = "'${parameter.name}"
            row.add(
                UICol().add(
                    when (parameter.type!!) {
                        ScriptParameterType.STRING -> UIInput("parameter$index.stringValue", label = label)
                        ScriptParameterType.INTEGER -> UIInput(
                            "parameter$index.intValue",
                            label = label, dataType = UIDataType.INT
                        )

                        ScriptParameterType.DECIMAL -> UIInput(
                            "parameter$index.decimalValue",
                            label = label, dataType = UIDataType.DECIMAL
                        )

                        ScriptParameterType.BOOLEAN -> UICheckbox("parameter$index.booleanValue", label = label)
                        ScriptParameterType.TASK -> UIInput(
                            "parameter$index.taskValue",
                            label = label, dataType = UIDataType.TASK
                        )

                        ScriptParameterType.USER -> UISelect.createUserSelect(
                            id = "parameter$index.userValue",
                            label = label
                        )

                        ScriptParameterType.TIME_PERIOD -> UIRow()
                            .add(
                                UICol(4).add(
                                    UIInput(
                                        "parameter$index.dateValue",
                                        label = label,
                                        additionalLabel = "date.begin",
                                        dataType = UIDataType.DATE
                                    )
                                )
                            ).add(
                                UICol(4).add(
                                    UIInput(
                                        "parameter$index.toDateValue",
                                        label = label,
                                        additionalLabel = "date.end",
                                        dataType = UIDataType.DATE
                                    )
                                )
                            )

                        ScriptParameterType.DATE -> UIInput(
                            "parameter$index.dateValue",
                            label = label,
                            dataType = UIDataType.DATE
                        )
                    }
                )
            )
            row.add(UICol().also { col ->
                parameter.description?.let {
                    col.add(UIAlert(message = it, color = UIColor.LIGHT, markdown = true))
                }
            })
        })
    }

    companion object {
        private fun getSessionAttr(scriptId: Long?): String {
            return "${AbstractScriptExecutePageRest::class.simpleName}:$scriptId"
        }
    }
}
