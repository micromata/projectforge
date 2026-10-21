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

package org.projectforge.plugins.merlin.rest

import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.word.templating.VariableType
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.merlin.*
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

/**
 * The execution of a Word® template (single document or serial execution by an Excel file), layout free for
 * projectforge-next. Every user with select access to the template (admins and users/groups with access) may
 * execute it.
 */
@RestController
@RequestMapping("${Rest.URL}/merlinexecution")
class MerlinExecuteRest {
    /**
     * An input variable of the template, as the execution form needs it.
     */
    class InputVariable(
        val name: String,
        val type: VariableType,
        val required: Boolean,
        val allowedValues: List<String>?,
        val description: String?,
        val minimumValue: Any?,
        val maximumValue: Any?,
        val scale: Int?,
    )

    /**
     * Everything the execution page shows: the template, its input variables and the values the user entered last
     * time.
     * @param serialFills The kinds of the Excel templates for the serial execution: `base` and, for admins and HR,
     * `employees` and `users` (prefilled with them).
     * @param editAccess True, if the user may edit the template (admins of it).
     */
    class ExecutionData(
        val id: Long,
        val name: String?,
        val description: String?,
        val variables: List<InputVariable>,
        val inputVariables: Map<String, Any>?,
        val pdfFormat: Boolean,
        val wordTemplateFileName: String?,
        val serialFills: List<String>,
        val editAccess: Boolean,
        val logViewerUrl: String,
    )

    @Autowired
    private lateinit var merlinTemplateDao: MerlinTemplateDao

    @Autowired
    private lateinit var merlinHandler: MerlinHandler

    @Autowired
    private lateinit var merlinRunner: MerlinRunner

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var employeeService: EmployeeService

    @AccessChecked("DAO: find of MerlinTemplateDao (admins and users/groups with access)")
    @GetMapping("load/{id}")
    fun load(@PathVariable("id", required = true) id: Long): ExecutionData {
        val dbObj = getTemplate(id)
        val analysis = merlinHandler.analyze(id)
        val inputVariables = analysis.statistics.variables.filter { it.input }.map {
            InputVariable(
                name = it.name,
                type = it.type,
                required = it.required,
                allowedValues = it.allowedValues,
                description = it.description,
                minimumValue = it.minimumValue,
                maximumValue = it.maximumValue,
                scale = it.scale,
            )
        }
        val serialFills = mutableListOf("base")
        if (isAdminOrHR()) {
            serialFills.add("employees")
            serialFills.add("users")
        }
        val userPref = getUserPref(id)
        return ExecutionData(
            id = id,
            name = dbObj.name,
            description = dbObj.description,
            variables = inputVariables,
            inputVariables = normalizeDates(userPref.inputVariables, inputVariables),
            pdfFormat = userPref.pdfFormat,
            wordTemplateFileName = analysis.wordTemplateFilename,
            serialFills = serialFills,
            editAccess = merlinTemplateDao.hasLoggedInUserUpdateAccess(dbObj, dbObj, false),
            logViewerUrl = MerlinPlugin.logViewerUrl(),
        )
    }

    /**
     * Creates the document of the template filled with the given variables (Word® or PDF).
     */
    @AccessChecked("DAO: find of MerlinTemplateDao (admins and users/groups with access)")
    @PostMapping("execute")
    fun execute(@Valid @RequestBody postData: PostData<MerlinExecutionData>): ResponseEntity<*> {
        MerlinPlugin.ensureUserLogSubscription()
        val executionData = postData.data
        getTemplate(executionData.id)
        log.info { "User wants to execute '${executionData.name}'..." }
        // Save input values as user preference:
        val userPref = getUserPref(executionData.id)
        userPref.inputVariables = executionData.inputVariables
        userPref.pdfFormat = executionData.pdfFormat
        val errors = validate(executionData)
        if (!errors.isNullOrEmpty()) {
            return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
        }
        val result = merlinRunner.executeTemplate(executionData.id, executionData.inputVariables)
        var filename = result.first
        val wordBytes = result.second
        var download = wordBytes
        if (executionData.pdfFormat) {
            try {
                val pdfResult = merlinRunner.convertToPdf(wordBytes, filename)
                filename = pdfResult.filename
                download = pdfResult.content
            } catch (ex: Throwable) {
                // Stackoverflow may occur.
                log.error(ex) { "Error while converting to pdf (falling back to docx): ${ex.message}" }
            }
        }
        return RestUtils.downloadFile(filename, download)
    }

    /**
     * Creates the documents of all rows of the uploaded Excel file, as zip archive.
     */
    @AccessChecked("DAO: find of MerlinTemplateDao (admins and users/groups with access)")
    @PostMapping("serialExecution/{id}")
    fun serialExecution(
        @PathVariable("id", required = true) id: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        MerlinPlugin.ensureUserLogSubscription()
        getTemplate(id)
        val filename = file.originalFilename
        log.info {
            "User tries to upload serial execution file: id='$id', filename='$filename', size=${
                FormatterUtils.formatBytes(
                    file.size
                )
            }."
        }
        val result = file.inputStream.use { inputStream ->
            merlinRunner.serialExecuteTemplate(id, filename ?: "untitled.xlsx", inputStream)
        } ?: return ResponseEntity.badRequest().body(translate("plugins.merlin.serial.unsupportedFile"))
        val zipFilename = result.first
        val zipByteArray = result.second
        return RestUtils.downloadFile(zipFilename, zipByteArray)
    }

    /**
     * The date values of the user pref as the date field of the form expects them ("yyyy-MM-dd"): the React form
     * stored timestamps.
     */
    private fun normalizeDates(values: Map<String, Any>?, variables: List<InputVariable>): Map<String, Any>? {
        values ?: return null
        return values.mapValues { (name, value) ->
            if (value is String && variables.any { it.name == name && it.type == VariableType.DATE }) {
                MerlinRunner.parseDate(value)?.toString() ?: value
            } else {
                value
            }
        }
    }

    private fun validate(data: MerlinExecutionData): List<ValidationError>? {
        val validationErrors = mutableListOf<ValidationError>()
        val stats = merlinHandler.analyze(data.id).statistics
        val inputVariables = stats.variables.filter { it.input }
        val inputData = data.inputVariables ?: emptyMap()
        inputVariables.forEach { variable ->
            variable.validate(inputData[variable.name])
                ?.let { validationErrors.add(ValidationError(it, getFieldId(variable.name))) }
        }
        return if (validationErrors.isEmpty()) null else validationErrors
    }

    /**
     * The Excel file for a serial execution, `fill=users` or `fill=employees` prefills it with them (admins and HR
     * only).
     */
    @AccessChecked("DAO: find of MerlinTemplateDao (admins and users/groups with access); users/employees for admins/HR only")
    @GetMapping("downloadSerialExecutionTemplate/{id}")
    fun downloadSerialExecutionTemplate(
        @PathVariable("id", required = true) id: Long,
        @RequestParam("fill") fill: String? = null
    ): ResponseEntity<*> {
        MerlinPlugin.ensureUserLogSubscription()
        getTemplate(id)
        val result = merlinRunner.createSerialExcelTemplate(id) { sheet: ExcelSheet ->
            if (fill == "users") {
                addUsers(sheet)
            } else if (fill == "employees") {
                addUsers(sheet, employees = true)
            }
        }
        val filename = result.first
        val excel = result.second
        return RestUtils.downloadFile(filename, excel)
    }

    /**
     * The template, if the logged-in user may read (and execute) it, otherwise an access exception is thrown.
     */
    private fun getTemplate(id: Long): MerlinTemplateDO {
        return merlinTemplateDao.find(id) ?: throw IllegalArgumentException("Template #$id not found.")
    }

    private fun isAdminOrHR(): Boolean {
        val userId = ThreadLocalUserContext.loggedInUserId
        return UserGroupCache.getInstance().isUserMemberOfAdminGroup(userId)
                || UserGroupCache.getInstance().isUserMemberOfHRGroup(userId)
    }

    private fun getUserPref(id: Long): MerlinExecutionData {
        return userPrefService.ensureEntry("merlin-template", "$id", MerlinExecutionData(id, ""))
    }

    private fun getFieldId(variableName: String): String {
        return "inputVariables.$variableName"
    }

    private fun addUsers(sheet: ExcelSheet, employees: Boolean = false) {
        val access = UserGroupCache.getInstance().isUserMemberOfAdminGroup(ThreadLocalUserContext.loggedInUserId)
                || UserGroupCache.getInstance().isUserMemberOfHRGroup(ThreadLocalUserContext.loggedInUserId)
        if (!access) {
            return
        }
        val hrAccess = UserGroupCache.getInstance().isUserMemberOfHRGroup(ThreadLocalUserContext.loggedInUserId)
        if (employees && hrAccess) {
            registerColumn(sheet, "staffNumber", "fibu.employee.staffNumber")
        }
        registerColumn(sheet, "username", "username")
        registerColumn(sheet, "gender", "gender")
        registerColumn(sheet, "nickname", "nickname")
        registerColumn(sheet, "firstname", "firstName")
        registerColumn(sheet, "lastname", "name")
        registerColumn(sheet, "email", "email")
        registerColumn(sheet, "organization", "organization")
        if (employees && hrAccess) {
            registerColumn(sheet, "street", "fibu.employee.street")
            registerColumn(sheet, "zipCode", "fibu.employee.zipCode")
            registerColumn(sheet, "city", "fibu.employee.city")
            registerColumn(sheet, "country", "fibu.employee.country")
        }
        sheet.reset()
        if (employees) {
            // Add employees
            employeeService.selectAllActive(false).filter { it.user?.hasSystemAccess() == true }.forEach { employee ->
                val row = sheet.createRow()
                sheet.reset()
                employee.user?.id?.let { userId ->
                    val user = userService.getUser(userId)
                    if (user.nickname.isNullOrBlank()) {
                        user.nickname = user.firstname
                    }
                    row.autoFillFromObject(user, "staffNumber", "street", "zipCode", "city", "country")
                }
                if (hrAccess) {
                    row.getCell("staffNumber")?.setCellValue(employee.staffNumber)
                }
            }
        } else {
            // Add users
            userService.allActiveUsers.forEach { user ->
                if (user.nickname.isNullOrBlank()) {
                    user.nickname = user.firstname
                }
                sheet.createRow().autoFillFromObject(user)
            }
        }
    }

    private fun registerColumn(sheet: ExcelSheet, columnHead: String, i18nKey: String, vararg aliases: String) {
        val headRow = sheet.headRow!!
        val translation = translate(i18nKey)
        val colDef = sheet.getColumnDef(columnHead) ?: sheet.getColumnDef(translation)
        if (colDef == null) {
            sheet.registerColumn(columnHead, translation, *aliases)
            headRow.createCell().setCellValue(translation)
        } else
            if (!colDef.found()) {
                // Column not found, but already registered with another name.
                val setOfAliases = colDef.columnAliases.toMutableSet()
                setOfAliases.add(colDef.columnHeadname ?: "<unknown>") // Add the old name.
                setOfAliases.add(translation)
                colDef.columnAliases = setOfAliases.toTypedArray()
                colDef.columnHeadname = columnHead // The new name.
                headRow.createCell().setCellValue(translation)
            } else {
                colDef.columnAliases = arrayOf(columnHead, translation)
            }
    }
}
