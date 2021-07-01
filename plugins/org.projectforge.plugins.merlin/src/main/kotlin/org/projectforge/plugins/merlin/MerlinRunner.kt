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
import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.*
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.DateFormatType
import org.projectforge.common.FormatterUtils
import org.projectforge.common.logging.LogFilter
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.framework.utils.NumberHelper
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
import java.math.RoundingMode
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

  @Autowired
  private lateinit var merlinHandler: MerlinHandler

  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  @Autowired
  private lateinit var merlinTemplateDefinitionHandler: MerlinTemplateDefinitionHandler

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  /**
   * @return Pair of filename and byte array representing the Word file.
   */
  fun executeTemplate(id: Int, inputVariables: Map<String, Any?>?): Pair<String, ByteArray> {
    val analysis = merlinHandler.analyze(id)
    val dto = analysis.dto
    val templateDefinition = analysis.statistics.templateDefinition
    val wordDocumentResult = merlinHandler.getWordTemplateInputStream(id)
      ?: throw IllegalArgumentException("No Word® template given. Can't run template.")
    val istream = wordDocumentResult.second
    val fileObject = wordDocumentResult.first
    val templateFilename = fileObject.fileName ?: "untitled.docx"
    istream.use {
      WordDocument(istream, templateFilename).use { doc ->
        val runner = WordTemplateRunner(templateDefinition, doc)
        val context = TemplateRunContext()
        initTemplateRunContext(context)
        val variables = formatVariables(inputVariables, dto)
        val result = runner.run(variables)
        val filename = runner.createFilename(dto.fileNamePattern, variables)
        val byteArray = result.asByteArrayOutputStream.toByteArray()
        return Pair(filename, byteArray)
      }
    }
  }

  /**
   * @param id Id of the MerlinTemplateDO
   * @param istream Inputstream of Excel serial file.
   * @return Pair of filename and byte array representing the zip file containing all generated word documents.
   */
  fun serialExecuteTemplate(id: Int, filename: String, istream: InputStream): Pair<String, ByteArray>? {
    if (!filename.endsWith("xlsx") && !filename.endsWith(".xls")) {
      log.error { "Only Excel files are supported for serial execution. Unsupported file: '$filename'" }
      return null
    }
    val lastLogNumber = MerlinPlugin.ensureUserLogSubscription().lastEntryNumber
    var serialData: SerialData?
    val analysis = merlinHandler.analyze(id)
    val templateDefinition = analysis.statistics.templateDefinition
    val dto = analysis.dto
    val wordDocument = merlinHandler.getWordTemplateToCloseOnYourOwn(id) ?: return null
    wordDocument.use { doc ->
      val excelByteArray: ByteArray
      istream.use {
        excelByteArray = it.readAllBytes()
      }
      ByteArrayInputStream(excelByteArray).use { xlsIstream ->
        ExcelWorkbook(xlsIstream, filename).use { workbook ->
          if (!SerialDataExcelReader.isMerlinSerialRunDefinition(workbook)) {
            return null
          }
          val reader = SerialDataExcelReader(workbook)
          initTemplateRunContext(reader.templateRunContext)
          val data = reader.serialData
          data.templateDefinition = templateDefinition
          data.template = analysis.statistics.template
          data.template.statistics.inputVariables.add(VariableDefinition(PERSONAL_BOX_VARIABLE))
          data.template.statistics.inputVariables.add(VariableDefinition(PERSONAL_BOX_VARIABLE_AS_PDF))
          reader.readVariables(data.template.statistics)

          val validatedEntries = mutableListOf<Variables>()
          data.entries.forEachIndexed { index, variables ->
            var error = false
            // Validate each set of variables (one set per document to generate):
            dto.variables.filter { it.input }.forEach { variable ->
              variable.validate(variables.get(variable.name))?.let { errorMsg ->
                log.error { "Document #$index: $errorMsg" }
                error = true
              }
            }
            if (!error) {
              validatedEntries.add(variables)
            }
          }
          data.entries = validatedEntries

          serialData = data
        }
      }
      val runner = SerialTemplateRunner(serialData, doc)
      var zipByteArray: ByteArray = runner.run(filename)
      zipByteArray = postProcessSerialDocuments(zipByteArray, excelByteArray, filename, serialData!!, dto, lastLogNumber, dto.pdfExport == true)
      return Pair(runner.zipFilename, zipByteArray)
    }
  }

  /**
   * @param id Id of the MerlinTemplateDO
   * @return Pair of filename and byte array representing the Excel file.
   */
  fun createSerialExcelTemplate(id: Int): Pair<String, ByteArray> {
    val serialData = SerialData()
    val analysis = merlinHandler.analyze(id)
    serialData.templateDefinition = analysis.statistics.templateDefinition
    serialData.template = analysis.statistics.template
    val writer = SerialDataExcelWriter(serialData)
    initTemplateRunContext(writer.templateRunContext)
    writer.writeToWorkbook(false).use { workbook ->
      val bos = org.apache.commons.io.output.ByteArrayOutputStream()
      bos.use {
        workbook.pOIWorkbook.write(bos)
        val filename = serialData.createFilenameForSerialTemplate()
        return Pair(filename, bos.toByteArray())
      }
    }
  }

  /**
   * @return Pair of filename and pdf byte array.
   */
  fun convertToPdf(wordBytes: ByteArray, filename: String): Pair<String, ByteArray> {
    ByteArrayInputStream(wordBytes).use { bais ->
      WordDocument(bais, filename).use { word ->
        val options = PdfOptions.create()
        ByteArrayOutputStream().use { baos ->
          PdfConverter.getInstance().convert(word.document, baos, options)
          return Pair("${FilenameUtils.getBaseName(filename)}.pdf", baos.toByteArray())
        }
      }
    }
  }

  /**
   * Drops files in receiver's personal box (DataTransfer) if configured and adds log view events as Excel
   * file to zip archive.
   */
  private fun postProcessSerialDocuments(
    zipByteArray: ByteArray,
    excelByteArray: ByteArray,
    excelFilename: String,
    serialData: SerialData,
    dto: MerlinTemplate,
    lastLogNumber: Long?,
    pdfExport: Boolean,
  ): ByteArray {
    processPersonalBoxOfReceivers(zipByteArray, serialData, dto)
    ZipInputStream(ByteArrayInputStream(zipByteArray)).use { zipInputStream ->
      ByteArrayOutputStream().use { baos ->
        ZipOutputStream(baos).use { zipOut ->
          var zipEntry = zipInputStream.nextEntry
          while (zipEntry != null) {
            val clonedZipEntry = zipEntry.clone() as ZipEntry
            zipOut.putNextEntry(clonedZipEntry)
            if (!zipEntry.isDirectory) {
              val ba = zipInputStream.readAllBytes()
              zipOut.write(ba)
              if (pdfExport && zipEntry.name.endsWith(".docx")) {
                val result = convertToPdf(ba, zipEntry.name)
                val pdfFilename = result.first
                val pdfByteArray = result.second
                zipOut.closeEntry()
                zipOut.putNextEntry(ZipEntry(pdfFilename))
                zipOut.write(pdfByteArray)
                log.info { "Converted to pdf: $pdfFilename" }
              }
            }
            zipOut.closeEntry()
            zipEntry = zipInputStream.nextEntry
          }
          zipInputStream.closeEntry()

          ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("plugins.merlin.export.logging.excel.sheetName"))
            ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "isoTimestamp", 20)
            ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "level", 6)
            ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "message", 100)
            ExcelUtils.registerColumn(sheet, LoggingEventData::class.java, "loggerName", 60)
            val boldFont = workbook.createOrGetFont("bold", bold = true)
            val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
            val headRow = sheet.createRow() // second row as head row.
            sheet.columnDefinitions.forEachIndexed { index, it ->
              headRow.getCell(index).setCellValue(it.columnHeadname).setCellStyle(boldStyle)
            }
            val logs =
              MerlinPlugin.ensureUserLogSubscription().query(LogFilter(lastReceivedLogOrderNumber = lastLogNumber))
                .sortedBy { it.id } // In ascending order.
            logs.forEach { logEntry ->
              val row = sheet.createRow()
              ExcelUtils.autoFill(row, logEntry)
            }
            val logViewerBytes = workbook.asByteArrayOutputStream.toByteArray()
            val logViewerEntry = ZipEntry("${translate("plugins.merlin.export.logging.excel.logBaseFilename")}.xlsx")
            zipOut.putNextEntry(logViewerEntry)
            zipOut.write(logViewerBytes)
            zipOut.closeEntry()
          }
          zipOut.putNextEntry(ZipEntry(excelFilename))
          zipOut.write(excelByteArray)
          zipOut.closeEntry()
        }
        return baos.toByteArray()
      }
    }
  }

  private fun processPersonalBoxOfReceivers(zipByteArray: ByteArray, serialData: SerialData, dto: MerlinTemplate) {
    if (serialData.entries.none {
        val personalBoxVariable = it.get(PERSONAL_BOX_VARIABLE)
        val personalBoxUsed =
          personalBoxVariable != null && personalBoxVariable is String && personalBoxVariable.isNotEmpty()

        val personalBoxAsPdfVariable = it.get(PERSONAL_BOX_VARIABLE_AS_PDF)
        val personalBoxAsPdfUsed =
          personalBoxAsPdfVariable != null && personalBoxAsPdfVariable is String && personalBoxAsPdfVariable.isNotEmpty()

        personalBoxUsed || personalBoxAsPdfUsed
      }) {
      // No #PersonalBox value or #PersonalBoxAsPdf given. Nothing to do.
      return
    }
    if (!pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }) {
      log.error { "No DataTransfer activated, can't use personal box. Please contact your administrator to activate the plugin 'DataTransfer'." }
      return
    }
    log.info { "Using $PERSONAL_BOX_VARIABLE/$PERSONAL_BOX_VARIABLE_AS_PDF for sending documents via DataTransfer." }
    // First, check all usernames:
    val docReceivers = mutableListOf<PFUserDO?>()
    val pdfReceivers = mutableListOf<PFUserDO?>()
    var validUsernames = true
    serialData.entries.forEach { variables ->
      val personalBoxUserResult = getUser(variables.get(PERSONAL_BOX_VARIABLE))
      val docReceiver = personalBoxUserResult.second
      val personalBoxAsPdfUserResult = getUser(variables.get(PERSONAL_BOX_VARIABLE_AS_PDF))
      val pdfReceiver = personalBoxAsPdfUserResult.second
      var error = false
      if (!personalBoxUserResult.first || !personalBoxAsPdfUserResult.first) {
        validUsernames = false
      } else {
        dto.variables.filter { it.input }.forEach { variable ->
          if (!error) { // Log only first validation error.
            variable.validate(variables.get(variable.name))?.let { errorMsg ->
              log.error { "Not sending anything to receiver '${docReceiver?.getFullname() ?: pdfReceiver?.getFullname()}' due to validation error of variable '${variable.name}': $errorMsg" }
              error = true
            }
          }
        }
      }
      if (!error) {
        docReceivers.add(docReceiver)
        pdfReceivers.add(pdfReceiver)
        if (docReceiver != null && pdfReceiver != null && docReceiver != pdfReceiver) {
          validUsernames = false
          log.error { "Can't send Word® file and PDF file to different users: '${docReceiver.getFullname()}' != '${pdfReceiver.getFullname()}'!" }
        }
      }
    }
    if (!validUsernames) {
      log.error { "Errors for personal box users occured. No document will be send to any personal user box. Aborting." }
      return
    }
    var counter = 0
    ZipInputStream(ByteArrayInputStream(zipByteArray)).use { zipInputStream ->
      var zipEntry = zipInputStream.nextEntry
      while (zipEntry != null) {
        if (zipEntry.isDirectory) {
          continue
        }
        if (docReceivers.size <= counter || pdfReceivers.size <= counter) {
          log.warn { "Oups, found more generated files than serial variables! Stopping #PersonalBox[AsPdf] processing" }
          break
        }
        val docReceiver = docReceivers[counter]
        val pdfReceiver = pdfReceivers[counter++]
        if (docReceiver != null || pdfReceiver != null) {
          val receiver = docReceiver ?: pdfReceiver!!
          try {
            zipEntry.name
            val personalBox = dataTransferAreaDao.ensurePersonalBox(receiver.id)
            if (personalBox == null) {
              log.error { "Can't get personal box of user '${receiver.getFullname()}. Skipping user." }
              continue
            }
            val wordBytes = zipInputStream.readAllBytes()
            if (docReceiver != null) {
              try {
                attachmentsService.addAttachment(
                  dataTransferAreaPagesRest.jcrPath!!,
                  fileInfo = FileInfo(zipEntry.name, fileSize = zipEntry.size),
                  content = wordBytes,
                  baseDao = dataTransferAreaDao,
                  obj = personalBox,
                  accessChecker = dataTransferAreaPagesRest.attachmentsAccessChecker,
                )
                log.info("Document '${zipEntry.name}' of size ${FormatterUtils.formatBytes(wordBytes.size)} put in the personal box (DataTransfer) of '${receiver.displayName}'.")
              } catch (ex: Exception) {
                log.error(
                  "Can't put document '${zipEntry.name}' of size ${FormatterUtils.formatBytes(wordBytes.size)} into user '${receiver.getFullname()}' personal box: ${ex.message}",
                  ex
                )
              }
            }
            if (pdfReceiver != null) {
              val pdfResult = convertToPdf(wordBytes, zipEntry.name)
              val pdfFilename = pdfResult.first
              val pdfBytes = pdfResult.second
              try {
                attachmentsService.addAttachment(
                  dataTransferAreaPagesRest.jcrPath!!,
                  fileInfo = FileInfo(pdfFilename, fileSize = pdfBytes.size.toLong()),
                  content = pdfBytes,
                  baseDao = dataTransferAreaDao,
                  obj = personalBox,
                  accessChecker = dataTransferAreaPagesRest.attachmentsAccessChecker,
                )
                log.info("Document '$pdfFilename' of size ${FormatterUtils.formatBytes(pdfBytes.size)} put in the personal box (DataTransfer) of '${receiver.displayName}'.")
              } catch (ex: Exception) {
                log.error(
                  "Can't put document '$pdfFilename' of size ${FormatterUtils.formatBytes(pdfBytes.size)} into user '${receiver.getFullname()}' personal box: ${ex.message}",
                  ex
                )
              }
            }
          } catch (ex: Exception) {
            log.error("Can't put document into user '${receiver.getFullname()}' personal box: ${ex.message}", ex)
          }
        }
        zipEntry = zipInputStream.nextEntry
      }
      zipInputStream.closeEntry()
    }
  }

  private fun getUser(userObject: Any?): Pair<Boolean, PFUserDO?> {
    userObject ?: return Pair(true, null) // OK, no user specified.
    val userString = "$userObject" // Stringify
    if (userString.isBlank()) {
      return Pair(true, null) // OK, no user specified.
    }
    val user = UserGroupCache.getInstance().getUser(userString.trim())
      ?: UserGroupCache.getInstance().getUserByFullname(userString.trim())
    if (user != null) {
      return Pair(true, user)
    }
    log.error { "User with username/full name '$userString' not found. No document will be send to any personal user box." }
    return Pair(false, null)
  }

  /**
   * Format variables.
   */
  private fun formatVariables(
    variables: Map<String, Any?>?,
    dto: MerlinTemplate,
  ): Variables {
    val result = Variables()
    variables?.forEach { (varname, value) ->
      val variable = dto.findVariableByName(varname)
      if (variable != null) {
        if (variable.type == VariableType.DATE && value != null && value is String) {
          val date = RestHelper.parseJSDateTime(value)?.sqlDate
          if (date != null) {
            val formattedDate = DateTimeFormatter.instance().getFormattedDate(date)
            result.putFormatted(varname, formattedDate)
          }
        } else if (variable.type.isIn(VariableType.INT, VariableType.FLOAT)) {
          if (value != null && value is String) {
            NumberHelper.parseBigDecimal(value)?.let { bd ->
              if (variable.type == VariableType.INT) {
                result.putFormatted(varname, NumberFormatter.format(bd.setScale(0, RoundingMode.HALF_UP), 0))
              } else {
                val scale = variable.scale
                if (scale != null) {
                  result.putFormatted(varname, NumberFormatter.format(bd.setScale(scale, RoundingMode.HALF_UP), scale))
                } else {
                  result.putFormatted(varname, NumberFormatter.format(bd))
                }
              }
            }
          }
        }
      }
      result.put(varname, value)
    }
    return result
  }

  companion object {
    private const val PERSONAL_BOX_VARIABLE = "#PersonalBox"
    private const val PERSONAL_BOX_VARIABLE_AS_PDF = "#PersonalBoxAsPdf"
    fun initTemplateRunContext(templateRunContext: TemplateRunContext) {
      val contextUser = ThreadLocalUserContext.getUser()
      templateRunContext.setLocale(DateFormats.getFormatString(DateFormatType.DATE), contextUser.locale)
    }
  }
}
