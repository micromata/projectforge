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

package org.projectforge.plugins.merlin

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.WordTemplateChecker
import mu.KotlinLogging
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.FileObject
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream


private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class MerlinHandler {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var merlinTemplateDefinitionHandler: MerlinTemplateDefinitionHandler

  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  internal lateinit var attachmentsAccessChecker: AttachmentsAccessChecker // Set by MerlinPagesRest

  internal lateinit var jcrPath: String // Set by MerlinPagesRest

  class Result(val statistics: MerlinStatistics, val dto: MerlinTemplate) {
    var wordDocument: WordDocument? = null
    var wordTemplateFilename: String? = null
  }

  internal fun getDto(id: Long): MerlinTemplate {
    val dbo = merlinTemplateDao.find(id) ?: return MerlinTemplate()
    return getDto(dbo)
  }

  internal fun getDto(dbo: MerlinTemplateDO): MerlinTemplate {
    val dto = MerlinTemplate()
    dto.copyFrom(dbo)
    dto.dependentVariables.forEach { variable ->
      variable.dependsOnName?.let { dependsOnName ->
        // Restore dependsOn variable:
        variable.dependsOn = dto.variables.find { it.name == dependsOnName }
      }
    }
    dto.variables.forEach { it.defined = true }
    dto.dependentVariables.forEach { it.defined = true }
    dto.ensureVariableIds()
    return dto
  }

  internal fun getAttachments(id: Long): List<Attachment>? {
    return attachmentsService.getAttachments(jcrPath, id, attachmentsAccessChecker)?.sortedByDescending { it.created }
  }

  internal fun getWordTemplate(list: List<Attachment>): Attachment? {
    return list.find { it.fileExtension == "docx" }
  }

  internal fun getWordTemplateInputStream(id: Long): Pair<FileObject, InputStream>? {
    val list = getAttachments(id) ?: return null
    val wordAttachment = getWordTemplate(list) ?: return null
    return attachmentsService.getAttachmentInputStream(
      jcrPath,
      id,
      wordAttachment.fileId!!,
      accessChecker = attachmentsAccessChecker,
    )
  }

  /**
   * Don't forget to close the returned WordDocument!!!!!!
   */
  internal fun getWordTemplateToCloseOnYourOwn(id: Long): WordDocument? {
    val result = getWordTemplateInputStream(id) ?: return null
    val fileObject = result.first
    val inputStream = result.second
    return WordDocument(inputStream, fileObject.fileName)
  }

  fun analyze(id: Long, keepWordDocument: Boolean = false): Result {
    return analyze(getDto(id), keepWordDocument)
  }

  /**
   * @param dto If given and variables are already defined, the settings of this given dto will override the stats settings.
   * @param keepWordDocument If true, you have to close the workbook finally on your own.
   */
  fun analyze(dto: MerlinTemplate, keepWordDocument: Boolean = false): Result {
    val stats = MerlinStatistics()
    val result = Result(stats,dto)
    val id = dto.id ?: return result

    val wordDocumentResult = getWordTemplateInputStream(id) ?: return result
    val istream = wordDocumentResult.second
    val fileObject = wordDocumentResult.first
    istream.use {
      val wordDocument = analyzeTemplate(
        istream,
        fileObject.fileName ?: "untitled.docx",
        dto,
        stats,
        keepWordDocument,
      )
      result.wordTemplateFilename = fileObject.fileName
      if (keepWordDocument) {
        result.wordDocument = wordDocument
      }
    }
    /**
     * Copy sortName of dto variables to stats variables.
     */
    stats.variables.forEach { inputVariable -> inputVariable.sortName = dto.variables.find { it.name == inputVariable.name }?.sortName }
    stats.variables.sort() // Sorted by sortName / name.
    return result
  }

  internal fun analyzeTemplate(
    istream: InputStream,
    filename: String,
    dto: MerlinTemplate,
    merlinStatistics: MerlinStatistics,
    keepWordDocument: Boolean = false,
  ): WordDocument? {
    val templateDefinition = merlinTemplateDefinitionHandler.readOrCreateTemplateDefinitionFrom(dto)
    var doc: WordDocument? = null
    try {
      doc = WordDocument(istream, filename)
      val templateChecker = WordTemplateChecker(doc)
      templateChecker.assignTemplateDefinition(templateDefinition)
      merlinStatistics.update(templateChecker, filename)
    } finally {
      if (!keepWordDocument) {
        doc?.close()
      }
    }
    return doc
  }

  internal fun dataTransferPluginAvailable(): Boolean {
    return DataTransferBridge.available
  }
}
