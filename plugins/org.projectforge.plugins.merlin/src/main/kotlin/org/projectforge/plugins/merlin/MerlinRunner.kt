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
import de.micromata.merlin.persistency.FileDescriptor
import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.*
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.projectforge.common.DateFormatType
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class MerlinRunner {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  internal lateinit var attachmentsAccessChecker: AttachmentsAccessChecker // Set by MerlinPagesRest

  internal lateinit var jcrPath: String // Set by MerlinPagesRest

  /**
   * @param id Id of the MerlinTemplateDO
   */
  fun getStatistics(id: Int): MerlinStatistics {
    val list = getAttachments(id) ?: return MerlinStatistics()
    val wordAttachment = getWordTemplate(list)
    val excelAttachment = list.find { it.fileExtension == "xlsx" }

    var templateDefinition: TemplateDefinition? = null
    val stats = MerlinStatistics()
    excelAttachment?.let {
      attachmentsService.getAttachmentInputStream(
        jcrPath,
        id,
        it.fileId!!,
        accessChecker = attachmentsAccessChecker,
      )?.let {
        val istream = it.second
        val fileObject = it.first
        stats.excelTemplateDefinitionFilename = fileObject.fileName
        istream.use {
          val reader = TemplateDefinitionExcelReader()
          val workBook = ExcelWorkbook(istream, fileObject.fileName ?: "undefined", ThreadLocalUserContext.getLocale())
          val def = reader.readFromWorkbook(workBook, false)
          // log.info("Template definition: ${ToStringUtil.toJsonString(def)}")
          def?.fileDescriptor = fakeFileDescriptor(fileObject.fileName ?: "untitled.xlsx")
          templateDefinition = def
        }
      }
    }
    wordAttachment?.let { word ->
      attachmentsService.getAttachmentInputStream(
        jcrPath,
        id,
        word.fileId!!,
        accessChecker = attachmentsAccessChecker,
      )?.let {
        val istream = it.second
        val fileObject = it.first
        istream.use {
          analyzeTemplate(istream, fileObject.fileName ?: "untitled.docx", templateDefinition, stats)
          stats.excelTemplateDefinitionFilename = excelAttachment?.name
          word.name?.let {
            stats.wordTemplateFilename = word.name
          }
          // log.info("Statistics: $stats")
        }
      }
    }
    return stats
  }

  /**
   * @return Pair of filename and byte array representing the Excel file.
   */
  fun writeTemplateDefinitionWorkbook(dto: MerlinTemplate): Pair<String, ByteArray> {
    val stats = getStatistics(dto.id!!)
    val writer = TemplateDefinitionExcelWriter()
    var filename = stats.excelTemplateDefinitionFilename
    if (filename == null) {
      filename = "${FilenameUtils.getBaseName(stats.wordTemplateFilename ?: "untitled")}.xlsx"
    }
    initTemplateRunContext(writer.templateRunContext)
    stats.template?.let { template ->
      template.fileDescriptor = FileDescriptor()
      template.fileDescriptor.filename = filename
    }
    val templateDefinition =
      stats.templateDefinition ?: stats.template?.createAutoTemplateDefinition() ?: TemplateDefinition()
    templateDefinition.filenamePattern = dto.fileNamePattern
    templateDefinition.id = "${dto.id}"
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
   * @return Pair of filename and byte array representing the Word file.
   */
  fun executeTemplate(dbo: MerlinTemplateDO, inputVariables: Map<String, Any?>?): Pair<String, ByteArray> {
    val id = dbo.id!!
    val stats = getStatistics(id)
    val templateDefinition = stats.templateDefinition
    val list = getAttachments(id) ?: throw IllegalArgumentException("No attachments given. Can't run template.")
    val wordAttachment =
      getWordTemplate(list) ?: throw IllegalArgumentException("No Word® template given. Can't run template.")
    attachmentsService.getAttachmentInputStream(
      jcrPath,
      id,
      wordAttachment.fileId!!,
      accessChecker = attachmentsAccessChecker,
    )?.let {
      val istream = it.second
      val fileObject = it.first
      val templateFilename = fileObject.fileName ?: "untitled.docx"
      istream.use {
        var doc: WordDocument? = null
        try {
          doc = WordDocument(istream, templateFilename)
          val runner = WordTemplateRunner(stats.templateDefinition, doc)
          val context = TemplateRunContext()
          initTemplateRunContext(context)
          val variables = convertVariables(inputVariables, templateDefinition, context)
          val result = runner.run(variables)
          val filename = runner.createFilename(dbo.fileNamePattern, variables)
          val byteArray = result.asByteArrayOutputStream.toByteArray()
          return Pair(filename, byteArray)
        } finally {
          doc?.close()
        }
      }
    }
    throw IllegalArgumentException("Can't execute Word template. Internal error.")
  }

  /**
   * @param id Id of the MerlinTemplateDO
   * @return Pair of filename and byte array representing the Excel file.
   */
  fun createSerialExcelTemplate(id: Int): Pair<String, ByteArray> {
    val stats = getStatistics(id)
    val serialData = SerialData()
    serialData.template = stats.template
    serialData.templateDefinition = stats.templateDefinition
    val writer = SerialDataExcelWriter(serialData)
    initTemplateRunContext(writer.templateRunContext)
    val workbook = writer.writeToWorkbook()

    val bos = org.apache.commons.io.output.ByteArrayOutputStream()
    bos.use {
      workbook.pOIWorkbook.write(bos)
      workbook.close()
      val filename = serialData.createFilenameForSerialTemplate()
      return Pair(filename, bos.toByteArray())
    }
  }

  private fun convertVariables(
    variables: Map<String, Any?>?,
    templateDefinition: TemplateDefinition?,
    context: TemplateRunContext
  ): Variables {
    val result = Variables()
    if (templateDefinition == null) {
      result.putAll(variables)
      return result
    }
    variables?.forEach { (varname, value) ->
      val variableDefinition = templateDefinition.getVariableDefinition(varname)
      if (variableDefinition != null) {
        if (variableDefinition.type == VariableType.DATE && value != null && value is String) {
          val date = RestHelper.parseJSDateTime(value)?.sqlDate
          if (date != null) {
            val formattedDate = DateTimeFormatter.instance().getFormattedDate(date)
            result.putFormatted(varname, formattedDate)
          }
        }
      }
      result.put(varname, value)
    }
    return result
  }

  private fun getAttachments(id: Int): List<Attachment>? {
    return attachmentsService.getAttachments(jcrPath, id, attachmentsAccessChecker)?.sortedByDescending { it.created }
  }

  private fun getWordTemplate(list: List<Attachment>): Attachment? {
    return list.find { it.fileExtension == "docx" }
  }

  private fun analyzeTemplate(
    istream: InputStream,
    filename: String,
    templateDefinition: TemplateDefinition? = null,
    merlinStatistics: MerlinStatistics,
  ) {
    var doc: WordDocument? = null
    try {
      doc = WordDocument(istream, filename)
      val templateChecker = WordTemplateChecker(doc)
      templateDefinition?.let {
        templateChecker.assignTemplateDefinition(it)
      }
      val statistics = templateChecker.template.statistics
      merlinStatistics.template = templateChecker.template
      merlinStatistics.template?.fileDescriptor = fakeFileDescriptor(filename)
      merlinStatistics.update(statistics, templateDefinition)
    } finally {
      doc?.close()
    }
  }

  private fun fakeFileDescriptor(filename: String):FileDescriptor {
    val fileDescriptor = FileDescriptor()
    fileDescriptor.filename = filename
    fileDescriptor.directory = "."
    fileDescriptor.relativePath = "."
    return fileDescriptor
  }

  companion object {
    fun initTemplateRunContext(templateRunContext: TemplateRunContext) {
      val contextUser = ThreadLocalUserContext.getUser()
      templateRunContext.setLocale(DateFormats.getExcelFormatString(DateFormatType.DATE), contextUser.locale)
    }
  }
}
