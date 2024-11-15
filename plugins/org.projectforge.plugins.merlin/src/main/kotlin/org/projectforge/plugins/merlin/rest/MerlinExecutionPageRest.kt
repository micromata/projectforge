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

package org.projectforge.plugins.merlin.rest

import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.word.templating.VariableType
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.plugins.merlin.*
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/merlinexecution")
class MerlinExecutionPageRest : AbstractDynamicPageRest() {

  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  @Autowired
  private lateinit var merlinHandler: MerlinHandler

  @Autowired
  private lateinit var merlinRunner: MerlinRunner

  @Autowired
  private lateinit var merlinPagesRest: MerlinPagesRest

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var employeeService: EmployeeService

  /**
   * Will be called, if the user wants to change his/her observeStatus.
   */
  @PostMapping("execute")
  fun execute(@Valid @RequestBody postData: PostData<MerlinExecutionData>): ResponseEntity<*> {
    MerlinPlugin.ensureUserLogSubscription()
    val executionData = postData.data
    log.info("User wants to execute '${executionData.name}'...")
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
        log.error("Error while converting to pdf (falling back to docx): ${ex.message}", ex)
      }
    }
    return RestUtils.downloadFile(filename, download)
  }

  @PostMapping("serialExecution/{id}")
  fun serialExecution(
    @PathVariable("id", required = true) id: Long,
    @RequestParam("file") file: MultipartFile
  ): ResponseEntity<*> {
    MerlinPlugin.ensureUserLogSubscription()
    val filename = file.originalFilename
    log.info {
      "User tries to upload serial execution file: id='$id', filename='$filename', size=${
        FormatterUtils.formatBytes(
          file.size
        )
      }."
    }
    val result = merlinRunner.serialExecuteTemplate(id, filename ?: "untitled.xlsx", file.inputStream)
      ?: throw IllegalArgumentException("Can't execute serial Excel file.")
    val zipFilename = result.first
    val zipByteArray = result.second
    return RestUtils.downloadFile(zipFilename, zipByteArray)
  }

  private fun validate(data: MerlinExecutionData): List<ValidationError>? {
    val validationErrors = mutableListOf<ValidationError>()
    val stats = merlinHandler.analyze(data.id).statistics
    val inputVariables = stats.variables.filter { it.input }
    val inputData = data.inputVariables
    if (inputData != null) {
      inputVariables.forEach { variable ->
        variable.validate(inputData[variable.name])
          ?.let { validationErrors.add(ValidationError(it, getFieldId(variable.name))) }
      }
    }
    return if (validationErrors.isEmpty()) null else validationErrors
  }

  @GetMapping("downloadSerialExecutionTemplate/{id}")
  fun downloadSerialExecutionTemplate(
    @PathVariable("id", required = true) id: Long,
    @RequestParam("fill") fill: String? = null
  )
      : ResponseEntity<*> {
    MerlinPlugin.ensureUserLogSubscription()
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

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val logViewerMenuItem = MerlinPlugin.createUserLogSubscriptionMenuItem()
    val id = NumberHelper.parseLong(idString) ?: throw IllegalAccessException("Parameter id not an int.")
    val dbObj = merlinTemplateDao.find(id)!!
    val dto = merlinPagesRest.transformFromDB(dbObj)
    val stats = merlinHandler.analyze(dto).statistics
    val col1 = UICol(md = 6)
    val col2 = UICol(md = 6)
    val inputVariables = stats.variables.filter { it.input }
    val size = inputVariables.size
    var counter = 0
    // Place input variables in two columns
    inputVariables.forEach {
      if (counter < size) {
        col1.add(createInputElement(it))
      } else {
        col2.add(createInputElement(it))
      }
      counter += 2
    }
    val layout = UILayout("plugins.merlin.templateExecutor.heading")
      .add(
        UIDropArea(
          "plugins.merlin.upload.serialExecution",
          tooltip = "plugins.merlin.upload.serialExecution.info",
          uploadUrl = RestResolver.getRestUrl(this::class.java, "serialExecution/$id"),
        )
      )
    val variablesFieldset = UIFieldset(title = "'${dto.name}")
    if (!dbObj.description.isNullOrBlank()) {
      variablesFieldset.add(UIAlert(message = "'${dbObj.description}", color = UIColor.LIGHT))
    }
    variablesFieldset.add(
      UIRow()
        .add(col1)
        .add(col2)
    )
      .add(
        UIRow()
          .add(
            UICol(md = 6)
              .add(
                UICheckbox(
                  "pdfFormat",
                  label = "plugins.merlin.format.pdf",
                  tooltip = "plugins.merlin.format.pdf.info"
                )
              )
          )
      )
    layout.add(variablesFieldset)
    layout.add(
      UIButton.createBackButton(
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(
            MerlinPagesRest::class.java,
            absolute = true
          ), targetType = TargetType.REDIRECT
        ),
      )
    ).add(
      UIButton.createDefaultButton(
        "execute",
        title = "plugins.merlin.templateExecutor.execute",
        responseAction = ResponseAction(
          RestResolver.getRestUrl(
            this::class.java,
            subPath = "execute"
          ), targetType = TargetType.POST
        ),
      )
    )

    layout.add(logViewerMenuItem)
    val serialExceutionMenu = MenuItem(
      "serialExceutionMenu",
      title = translate("plugins.merlin.serial.template.download"),
    )
    serialExceutionMenu.add(createSerialExcelDownloadMenu(id, "base"))
    if (UserGroupCache.getInstance().isUserMemberOfAdminGroup(ThreadLocalUserContext.loggedInUserId)
      || UserGroupCache.getInstance().isUserMemberOfHRGroup(ThreadLocalUserContext.loggedInUserId)
    ) {
      serialExceutionMenu.add(createSerialExcelDownloadMenu(id, "employees"))
      serialExceutionMenu.add(createSerialExcelDownloadMenu(id, "users"))
    }
    layout.add(serialExceutionMenu)
    MenuItem(
      "logViewer",
      i18nKey = "plugins.merlin.viewLogs",
      url = PagesResolver.getDynamicPageUrl(
        LogViewerPageRest::class.java,
        id = MerlinPlugin.ensureUserLogSubscription().id
      ),
      type = MenuItemTargetType.REDIRECT,
    )

    if (hasEditAccess(dbObj)) {
      layout.add(
        MenuItem(
          "EDIT",
          i18nKey = "plugins.merlin.title.edit",
          url = PagesResolver.getEditPageUrl(MerlinPagesRest::class.java, dto.id),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }
    LayoutUtils.process(layout)

    val executionData = MerlinExecutionData(dto.id!!, dto.name ?: "???")
    val userPref = getUserPref(id)
    executionData.inputVariables = userPref.inputVariables
    executionData.pdfFormat = userPref.pdfFormat
    return FormLayoutData(executionData, layout, createServerData(request))
  }

  private fun createSerialExcelDownloadMenu(id: Long, type: String): MenuItem {
    val fill = if (type == "base") {
      ""
    } else {
      "?fill=$type"
    }
    return MenuItem(
      "serialExecution",
      i18nKey = "plugins.merlin.serial.template.download.$type",
      tooltip = "plugins.merlin.serial.template.download.info",
      url = RestResolver.getRestUrl(
        this.javaClass,
        "downloadSerialExecutionTemplate/$id$fill"
      ),
      type = MenuItemTargetType.DOWNLOAD,
    )
  }

  private fun createInputElement(variable: MerlinVariable): UIElement {
    val dataType = when (variable.type) {
      VariableType.DATE -> UIDataType.DATE
      VariableType.FLOAT -> UIDataType.DECIMAL
      VariableType.INT -> UIDataType.INT
      else -> UIDataType.STRING
    }
    val allowedValues = variable.allowedValues
    val name = variable.name
    if (allowedValues.isNullOrEmpty()) {
      return UIInput(
        getFieldId(name),
        label = "'$name",
        dataType = dataType,
        required = variable.required,
        tooltip = "'${variable.description}",
      )
    }
    val values = allowedValues.map { UISelectValue(it, it) }
    return UISelect(getFieldId(name), label = "'$name", required = variable.required, values = values)
  }

  /**
   * @return true, if the area isn't a personal box and the user has write access.
   */
  private fun hasEditAccess(dbObj: MerlinTemplateDO): Boolean {
    return merlinTemplateDao.hasLoggedInUserUpdateAccess(dbObj, dbObj, false)
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
        employee.user?.let { user ->
          if (user.nickname.isNullOrBlank()) {
            user.nickname = user.firstname
          }
          row.autoFillFromObject(employee.user, "staffNumber", "street", "zipCode", "city", "country")
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
