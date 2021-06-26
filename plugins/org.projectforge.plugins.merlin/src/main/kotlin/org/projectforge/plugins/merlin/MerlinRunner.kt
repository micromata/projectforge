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
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.DateFormatType
import org.projectforge.common.FormatterUtils
import org.projectforge.common.logging.LogFilter
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferPlugin
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class MerlinRunner {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  internal lateinit var attachmentsAccessChecker: AttachmentsAccessChecker // Set by MerlinPagesRest

  internal lateinit var jcrPath: String // Set by MerlinPagesRest

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  /**
   * @param id Id of the MerlinTemplateDO
   * @param keepWordDocument If true, you have to close the workbook finally.
   */
  fun getStatistics(id: Int, keepWordDocument: Boolean = false): MerlinStatistics {
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
          stats.wordDocument = analyzeTemplate(
            istream,
            fileObject.fileName ?: "untitled.docx",
            templateDefinition,
            stats,
            keepWordDocument
          )
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
      template.fileDescriptor.filename = stats.wordTemplateFilename
    }
    val templateDefinition =
      stats.templateDefinition ?: stats.template?.createAutoTemplateDefinition() ?: TemplateDefinition()
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
          val variables = convertVariables(inputVariables, templateDefinition)
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
   * @return Pair of filename and byte array representing the zip file containing all generated word documents.
   */
  fun serialExecuteTemplate(id: Int, filename: String, istream: InputStream): Pair<String, ByteArray>? {
    if (!filename.endsWith("xlsx") && !filename.endsWith(".xls")) {
      log.error { "Only Excel files are supported for serial execution. Unsupported file: '$filename'" }
      return null
    }
    val lastLogNumber = MerlinPlugin.ensureUserLogSubscription().lastEntryNumber
    var workbook: ExcelWorkbook? = null
    var wordDocument: WordDocument? = null
    var stats: MerlinStatistics?
    var serialData: SerialData?
    try {
      stats = getStatistics(id, true)
      wordDocument = stats.wordDocument
      if (wordDocument == null) {
        return null
      }
      istream.use {
        workbook = ExcelWorkbook(istream, filename)
        if (!SerialDataExcelReader.isMerlinSerialRunDefinition(workbook)) {
          return null
        }
        val reader = SerialDataExcelReader(workbook)
        initTemplateRunContext(reader.templateRunContext)
        val data = reader.serialData
        data.template = stats.template
        data.templateDefinition = stats.templateDefinition
        data.template.statistics.inputVariables.add(VariableDefinition(PERSONAL_BOX_VARIABLE))
        reader.readVariables(data.template.statistics)
        serialData = data
      }
      val runner = SerialTemplateRunner(serialData, wordDocument)
      var zipByteArray: ByteArray = runner.run(filename)

      zipByteArray = postProcessSerialDocuments(zipByteArray, serialData!!, lastLogNumber)
      return Pair(runner.zipFilename, zipByteArray)
    } finally {
      workbook?.close()
      wordDocument?.close()
    }
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
    val workbook = writer.writeToWorkbook(false)

    val bos = org.apache.commons.io.output.ByteArrayOutputStream()
    bos.use {
      workbook.pOIWorkbook.write(bos)
      workbook.close()
      val filename = serialData.createFilenameForSerialTemplate()
      return Pair(filename, bos.toByteArray())
    }
  }

  /**
   * Drops files in receiver's personal box (DataTransfer) if configured and adds log view events as Excel
   * file to zip archive.
   */
  private fun postProcessSerialDocuments(
    zipByteArray: ByteArray,
    serialData: SerialData,
    lastLogNumber: Long?
  ): ByteArray {
    processPersonalBoxOfReceivers(zipByteArray, serialData)
    ZipInputStream(ByteArrayInputStream(zipByteArray)).use { zipInputStream ->
      val baos = ByteArrayOutputStream()
      baos.use { baos ->
        ZipOutputStream(baos).use { zipOut ->
          var zipEntry = zipInputStream.nextEntry
          while (zipEntry != null) {
            val clonedZipEntry = zipEntry.clone() as ZipEntry
            zipOut.putNextEntry(clonedZipEntry)
            if (!zipEntry.isDirectory) {
              zipInputStream.copyTo(zipOut)
            }
            zipOut.closeEntry()
            zipEntry = zipInputStream.nextEntry
          }
          zipInputStream.closeEntry()

          val workbook = ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.getLocale())
          val sheet = workbook.createOrGetSheet(translate("plugins.merlin.export.logging.excel.sheetName"))
          ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "isoTimestamp")
          ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "level", 6)
          ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "message", 100)
          ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "loggerName", 30)
          val boldFont = workbook.createOrGetFont("bold", bold = true)
          val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
          val headRow = sheet.createRow() // second row as head row.
          sheet.columnDefinitions.forEachIndexed { index, it ->
            headRow.getCell(index).setCellValue(it.columnHeadname).setCellStyle(boldStyle)
          }
          val logs =
            MerlinPlugin.ensureUserLogSubscription().query(LogFilter(lastReceivedLogOrderNumber = lastLogNumber))
          logs.forEach { logEntry ->
            val row = sheet.createRow()
            ExcelUtils.autoFill(row, logEntry)
          }
          val logViewerBytes = workbook.asByteArrayOutputStream.toByteArray()
          workbook.close()
          val logViewerEntry = ZipEntry("${translate("plugins.merlin.export.logging.excel.logBaseFilename")}.xlsx")
          zipOut.putNextEntry(logViewerEntry)
          zipOut.write(logViewerBytes)
          zipOut.closeEntry()
        }
        return baos.toByteArray()
      }
    }
  }

  private fun processPersonalBoxOfReceivers(zipByteArray: ByteArray, serialData: SerialData) {
    if (serialData.entries.none {
        val personalBoxVariable = it.get(PERSONAL_BOX_VARIABLE)
        personalBoxVariable != null && personalBoxVariable is String && personalBoxVariable.isNotEmpty()
      }) {
      // No #PersonalBox value given. Nothing to do.
      return
    }
    if (!pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }) {
      log.error { "No DataTransfer activated, can't use personal box. Please contact your administrator to activate the plugin 'DataTransfer'." }
      return
    }
    log.info { "Using #PersonalBox for sending documents via DataTransfer." }
    // First, check all usernames:
    val users = mutableListOf<PFUserDO?>()
    serialData.entries.forEach { variables ->
      val username = variables.get(PERSONAL_BOX_VARIABLE)
      if (username != null) {
        val user = UserGroupCache.getInstance().getUser("$username") // username is of type Any.
        if (user == null) {
          log.error { "User with username '$username' not found. No document will be send to any personal user box. Aborting." }
          return
        }
        users.add(user)
      } else {
        users.add(null)
      }
    }
    var counter = 0
    ZipInputStream(ByteArrayInputStream(zipByteArray)).use { zipInputStream ->
      var zipEntry = zipInputStream.nextEntry
      while (zipEntry != null) {
        if (zipEntry.isDirectory) {
          continue
        }
        if (users.size <= counter) {
          log.warn { "Oups, found more generated files than serial variables! Stopping #PersonalBox processing" }
          break
        }
        val user = users[counter++]
        if (user != null) {
          try {
            zipEntry.name
            val personalBox = dataTransferAreaDao.ensurePersonalBox(user.id)
            if (personalBox == null) {
              log.error { "Can't get personal box of user '${user.username}. Skipping user." }
              continue
            }
            attachmentsService.addAttachment(
              dataTransferAreaPagesRest.jcrPath!!,
              fileInfo = FileInfo(zipEntry.name, fileSize = zipEntry.size),
              content = zipInputStream.readAllBytes(),
              baseDao = dataTransferAreaDao,
              obj = personalBox,
              accessChecker = dataTransferAreaPagesRest.attachmentsAccessChecker,
            )
            log.info("Document '${zipEntry.name}' of size ${FormatterUtils.formatBytes(zipEntry.size)} put in the personal box (DataTransfer) of '${user.displayName}'.")
          } catch (ex: Exception) {
            log.error("Can't put document into user '${user.username}' personal box: ${ex.message}", ex)
          }
        }
        zipEntry = zipInputStream.nextEntry
      }
      zipInputStream.closeEntry()
    }
  }

  private fun convertVariables(
    variables: Map<String, Any?>?,
    templateDefinition: TemplateDefinition?,
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
    keepWordDocument: Boolean = false,
  ): WordDocument? {
    var doc: WordDocument? = null
    try {
      doc = WordDocument(istream, filename)
      val templateChecker = WordTemplateChecker(doc)
      templateDefinition?.let {
        templateChecker.assignTemplateDefinition(it)
      }
      val statistics = templateChecker.template.statistics
      merlinStatistics.template = templateChecker.template
      merlinStatistics.template?.fileDescriptor = createFileDescriptor(filename)
      merlinStatistics.update(statistics, templateDefinition)
    } finally {
      if (!keepWordDocument) {
        doc?.close()
      }
    }
    return doc
  }

  private fun createFileDescriptor(filename: String): FileDescriptor {
    val fileDescriptor = FileDescriptor()
    fileDescriptor.filename = filename
    return fileDescriptor
  }

  companion object {
    private const val PERSONAL_BOX_VARIABLE = "#PersonalBox"
    fun initTemplateRunContext(templateRunContext: TemplateRunContext) {
      val contextUser = ThreadLocalUserContext.getUser()
      templateRunContext.setLocale(DateFormats.getFormatString(DateFormatType.DATE), contextUser.locale)
    }
  }
}
