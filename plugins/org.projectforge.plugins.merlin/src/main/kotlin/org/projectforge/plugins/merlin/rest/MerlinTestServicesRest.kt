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

import de.micromata.merlin.word.templating.VariableType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.merlin.MerlinHandler
import org.projectforge.plugins.merlin.MerlinTemplate
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.plugins.merlin.MerlinVariable
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.User
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/merlin")
class MerlinTestServicesRest {
  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var merlinHandler: MerlinHandler

  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  @Autowired
  private lateinit var merlinPagesRest: MerlinPagesRest

  @GetMapping("contract")
  fun contract(): ResponseEntity<*> {
    return createTemplate(
      titleKey = "plugins.merlin.menu.examples.contract",
      filename = "EmploymentContractTemplate.docx",
      fileNamePattern = "\${Date}-Employment-Contract_\${Employee}",
      variables = listOf(
        createVariable("Gender", true, "Gender of the employee.", listOf("male", "female")),
        createVariable(
          "Employee", true, "Name of employee."
        ),
        createVariable(
          "Date", true, "Date of contract.", type = VariableType.DATE
        ),
        createVariable("BeginDate", true, "Begin of the contract.", type = VariableType.DATE),
        createVariable(
          "WeeklyHours",
          true,
          "The weekly working hours.",
          type = VariableType.INT,
          minimumValue = 1,
          maximumValue = 40
        ),
        createVariable(
          "NumberOfLeaveDays",
          true,
          "The number of leave days per year.",
          type = VariableType.INT,
          minimumValue = 20,
          maximumValue = 30
        )
      ),
      dependentVariables = listOf(
        createDependentVariable("Mr_Mrs", "Gender", "Mr., Mrs."),
        createDependentVariable("him_her", "Gender", "him, her"),
        createDependentVariable("He_She", "Gender", "He, She"),
        createDependentVariable("he_she", "Gender", "he, she"),
        createDependentVariable("his_her", "Gender", "his, her"),
        createDependentVariable("His_Her", "Gender", "His, Her"),
      )
    )
  }

  @GetMapping("letter")
  fun letter(): ResponseEntity<*> {
    return createTemplate(
      titleKey = "plugins.merlin.menu.examples.letter",
      filename = "LetterTemplate.docx",
      fileNamePattern = "\${Date}-letter_\${Receiver}",
      variables = listOf(
        createVariable("Gender", true, "Gender of the employee.", listOf("male", "female")),
        createVariable("Date", true, "Date of letter.", type = VariableType.DATE),
        createVariable("Receiver", true, "Name of receiver."),
        createVariable("Receiver_Address", true),
        createVariable("Receiver_City", true),
      ),
      dependentVariables = listOf(
        createDependentVariable("Mr_Mrs", "Gender", "Mr., Mrs."),
      )
    )
  }

  private fun createTemplate(
    titleKey: String,
    filename: String,
    fileNamePattern: String,
    variables: List<MerlinVariable>,
    dependentVariables: List<MerlinVariable>
  ): ResponseEntity<*> {
    val dto = MerlinTemplate()
    dto.name = translate(titleKey)
    dto.admins = listOf(User(ThreadLocalUserContext.user!!))
    dto.fileNamePattern = fileNamePattern
    dto.stronglyRestrictedFilenames = true
    if (merlinHandler.dataTransferPluginAvailable()) {
      dto.dataTransferUsage = true
    }
    dto.variables.addAll(variables)
    dto.dependentVariables.addAll(dependentVariables)
    dto.dependentVariables.forEach { variable ->
      variable.dependsOnName?.let { dependsOnName ->
        // Restore dependsOn variable:
        variable.dependsOn = dto.variables.find { it.name == dependsOnName }
      }
    }
    val dbo = merlinPagesRest.transformForDB(dto)
    merlinTemplateDao.save(dbo)
    dto.id = dbo.id
    applicationContext.getResource("classpath:examples/$filename").inputStream.use { istream ->
      attachmentsService.addAttachment(
        merlinHandler.jcrPath,
        FileInfo(filename),
        istream,
        merlinTemplateDao,
        dbo,
        accessChecker = merlinHandler.attachmentsAccessChecker,
      )
    }
    return ResponseEntity(
      ResponseAction(
        PagesResolver.getDynamicPageUrl(MerlinExecutionPageRest::class.java, id = dto.id, absolute = true),
        targetType = TargetType.REDIRECT
      ), HttpStatus.OK
    )
  }

  private fun createVariable(
    name: String,
    required: Boolean,
    description: String? = null,
    allowedValues: List<String>? = null,
    type: VariableType = VariableType.STRING,
    minimumValue: Int? = null,
    maximumValue: Int? = null,
  ): MerlinVariable {
    val variable = MerlinVariable()
    variable.name = name
    variable.required = required
    variable.description = description
    variable.allowedValues = allowedValues
    variable.type = type
    variable.minimumValue = minimumValue
    variable.maximumValue = maximumValue
    return variable
  }

  private fun createDependentVariable(
    name: String,
    dependsOn: String,
    mappingValues: String,
  ): MerlinVariable {
    val variable = MerlinVariable()
    variable.name = name
    variable.mappingValues = mappingValues
    variable.dependsOnName = dependsOn
    return variable
  }
}
