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
import de.micromata.merlin.word.templating.TemplateDefinition
import de.micromata.merlin.word.templating.TemplateDefinitionExcelReader
import de.micromata.merlin.word.templating.TemplateRunContext
import de.micromata.merlin.word.templating.WordTemplateChecker
import mu.KotlinLogging
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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

  fun analyzeTemplate(
    istream: InputStream,
    filename: String,
    templateDefinition: TemplateDefinition? = null,
    merlinStatistics: MerlinStatistics,
  ) {
    val doc = WordDocument(istream, filename)
    val templateChecker = WordTemplateChecker(doc)
    templateDefinition?.let {
      templateChecker.assignTemplateDefinition(it)
    }
    val statistics = templateChecker.template.statistics
    merlinStatistics.template = templateChecker.template
    merlinStatistics.update(statistics, templateDefinition)
  }

  /**
   * @param id Id of the MerlinTemplateDO
   */
  fun getStatistics(id: Int): MerlinStatistics {
    val list =
      attachmentsService.getAttachments(jcrPath, id, attachmentsAccessChecker)?.sortedByDescending { it.created }
        ?: return MerlinStatistics()
    val wordAttachment = list.find { it.fileExtension == "docx" }
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

  companion object {
    fun initTemplateRunContext(templateRunContext: TemplateRunContext) {
      val contextUser = ThreadLocalUserContext.getUser()
      templateRunContext.setLocale(contextUser.excelDateFormat, contextUser.locale)
    }
  }
}
