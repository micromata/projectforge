/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin

import de.micromata.merlin.excel.ExcelWorkbook
import de.micromata.merlin.word.templating.*
import org.apache.commons.io.FilenameUtils
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream


// private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class MerlinTemplateDefinitionHandler {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var merlinHandler: MerlinHandler

  /**
   * @return Pair of filename and byte array representing the Excel file.
   */
  fun writeTemplateDefinitionWorkbook(dto: MerlinTemplate): Pair<String, ByteArray> {
    val writer = TemplateDefinitionExcelWriter()
    val filename = "${FilenameUtils.getBaseName(dto.wordTemplateFileName ?: "untitled.docx")}.xlsx"
    MerlinRunner.initTemplateRunContext(writer.templateRunContext)
    val templateDefinition = readOrCreateTemplateDefinitionFrom(dto)
    templateDefinition.filenamePattern = dto.fileNamePattern
    templateDefinition.id = filename
    templateDefinition.isStronglyRestrictedFilenames = dto.stronglyRestrictedFilenames == true
    templateDefinition.description = dto.description
    val workbook = writer.writeToWorkbook(templateDefinition)
    workbook.filename = filename
    val outStream = ByteArrayOutputStream()
    outStream.use {
      workbook.write(it)
      return Pair(filename, outStream.toByteArray())
    }
  }

  /**
   * Creates Template definition by using defined variables or by loading the latest xlsx file, if the file is newer
   * than the last modification time of the template.
   * If the template definition is read from Excel file, the variables of the given dto will be updated.
   */
  fun readOrCreateTemplateDefinitionFrom(id: Int): TemplateDefinition {
    return readOrCreateTemplateDefinitionFrom(merlinHandler.getDto(id))
  }

  /**
   * Creates Template definition by using defined variables or by loading the latest xlsx file, if the file is newer
   * than the last modification time of the template.
   * If the template definition is read from Excel file, the variables of the given dto will be updated.
   * @param updateDto If true, the variables of the given dto will be updated, if an newer template definition Excel file was found.
   */
  fun readOrCreateTemplateDefinitionFrom(dto: MerlinTemplate, updateDto: Boolean = false): TemplateDefinition {
    val id = dto.id!!
    // No variables defined, so try to read from uploaded Excel:
    val list = merlinHandler.getAttachments(id) ?: return createTemplateDefinitionFromDto(dto)
    val excelAttachment = list.find { it.fileExtension == "xlsx" }
    excelAttachment?.created?.let { excelUploadDate ->
      // Created date is the upload date, lastUpdate is also renewed, if the user edit the description or filename.
      if (dto.lastVariableUpdate == null || excelUploadDate > dto.lastVariableUpdate) {
        readTemplateDefinition(id, excelAttachment)?.let { templateDefinition ->
          dto.excelTemplateDefinitionFileName = excelAttachment.name
          dto.updateFromTemplateDefinition(templateDefinition)
          return templateDefinition
        }
      }
    }
    dto.excelTemplateDefinitionFileName = null // No Excel file is used.
    return createTemplateDefinitionFromDto(dto)
  }

  private fun readTemplateDefinition(id: Int, attachment: Attachment?): TemplateDefinition? {
    attachment ?: return null
    val attPair = attachmentsService.getAttachmentInputStream(
      merlinHandler.jcrPath,
      id,
      attachment.fileId!!,
      accessChecker = merlinHandler.attachmentsAccessChecker,
    ) ?: return null

    val istream = attPair.second
    val fileObject = attPair.first
    istream.use {
      val reader = TemplateDefinitionExcelReader()
      ExcelWorkbook(istream, fileObject.fileName ?: "undefined", ThreadLocalUserContext.getLocale()).use { workBook ->
        val def = reader.readFromWorkbook(workBook, false)
        // log.info("Template definition: ${ToStringUtil.toJsonString(def)}")
        return def
      }
    }
  }

  /**
   * Creates templateDefinition from given dto.
   */
  fun createTemplateDefinitionFromDto(dto: MerlinTemplate?): TemplateDefinition {
    val templateDefinition = TemplateDefinition()
    dto ?: return templateDefinition
    templateDefinition.filenamePattern = dto.fileNamePattern
    templateDefinition.description = dto.description
    templateDefinition.isStronglyRestrictedFilenames = (dto.stronglyRestrictedFilenames == true)
    val allVariables = mutableListOf<MerlinVariable>()
    // First get all variables from templateDefinition if any:
    templateDefinition.variableDefinitions?.forEach {
      allVariables.add(MerlinVariable.from(it))
    }
    templateDefinition.dependentVariableDefinitions?.forEach {
      allVariables.add(MerlinVariable.from(it))
    }
    // Upsert all variables from dto:
    (dto.variables + dto.dependentVariables).forEach { dtoVariable ->
      val variable = allVariables.find { it.name == dtoVariable.name }
      if (variable == null) {
        // Add dto variable to template definition.
        allVariables.add(dtoVariable)
      } else {
        variable.copyFrom(dtoVariable)
      }
    }
    // Now, re-create variables in templateDefinition:
    templateDefinition.variableDefinitions = MerlinTemplate.extractInputVariables(allVariables).map { dtoVariable ->
      val definition = VariableDefinition()
      dtoVariable.copyTo(definition)
      definition
    }
    // Now, re-create dependent variables in templateDefinition:
    templateDefinition.dependentVariableDefinitions =
      MerlinTemplate.extractDependentVariables(allVariables).map { dtoVariable ->
        val definition = DependentVariableDefinition()
        dtoVariable.copyTo(definition)
        definition
      }
    return templateDefinition
  }
}
