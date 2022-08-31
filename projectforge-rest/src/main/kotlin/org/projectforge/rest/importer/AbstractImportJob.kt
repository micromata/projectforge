/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.importer

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.MarkdownBuilder
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.ui.UIAlert
import org.projectforge.ui.UIColor

/**
 * IMPORTANT: Don't forget to check [isActive] in your import job in every loop to enable cancellation.
 * @param title Human-readable title for displaying and logging purposes.
 * @param area Area describing type of job. If [queueStrategy] is true then jobs of same area and same user are queued.
 * @param userId Job is started by this user.
 * @param queueName For queueing strategy you may define multiple queues per area, If not given, only one queue is used
 * per area (and user).
 * @param queueStrategy Should this job be queued if any other job is already running.
 */
abstract class AbstractImportJob(
  title: String,
  area: String? = null,
  userId: Int? = ThreadLocalUserContext.userId,
  queueName: String? = null,
  /**
   * If true then jobs of same area, same queueName and same user are queued.
   */
  queueStrategy: QueueStrategy = QueueStrategy.NONE,
  /**
   * At default, this job will be cancelled after 120s +.
   */
  timeoutSeconds: Int = 120,
  importStorage: ImportStorage<*>? = null,
  ) : AbstractJob(
  title = title,
  area = area,
  ownerId = userId,
  queueName = queueName,
  queueStrategy = queueStrategy,
  timeoutSeconds = timeoutSeconds,
) {
  class JobResult(errorMessage: String? = null) {
    var inserted: Int = 0
    var deleted: Int = 0
    var updated: Int = 0
    var unmodified: Int = 0
    var errorMessages: List<String>? = null

    var totalToBeInserted: Int? = null
    var totalToBeDeleted: Int? = null
    var totalToBeUpdated: Int? = null

    init {
      if (errorMessage != null) {
        errorMessages = listOf(errorMessage)
      }
    }

    val markdown: String
      get() {
        val md = MarkdownBuilder()
        md.appendPipedValue(
          "import.result.numberOfCreated",
          NumberFormatter.format(inserted),
          MarkdownBuilder.Color.GREEN,
          NumberFormatter.format(totalToBeInserted),
        )
        md.appendPipedValue(
          "import.result.numberOfUpdated",
          NumberFormatter.format(updated),
          MarkdownBuilder.Color.BLUE,
          NumberFormatter.format(totalToBeUpdated),
        )
        md.appendPipedValue(
          "import.result.numberOfDeleted",
          NumberFormatter.format(deleted),
          MarkdownBuilder.Color.RED,
          NumberFormatter.format(totalToBeDeleted),
        )
        md.appendPipedValue(
          "import.result.numberOfUnmodified",
          NumberFormatter.format(unmodified),
        )
        val msg = StringBuilder()
        msg.appendLine(md.toString())
        errorMessages?.let {
          msg.appendLine("### ${translate("errors")}")
          msg.appendLine(it.joinToString("\n", "- ") { it })
        }
        return msg.toString()
      }

    val asUIAlert: UIAlert
      get() {
        var color = UIColor.SECONDARY
        if (errorMessages?.isNotEmpty() == true) {
          color = UIColor.DANGER
        }
        return UIAlert(
          title = "import.result.title",
          message = "$markdown\n\n",
          markdown = true,
          color = color,
        )
      }
  }

  val result = JobResult()

  init {
    updateTotals(importStorage)
  }

  fun updateTotals(importStorage: ImportStorage<*>?) {
    if (importStorage != null) {
      result.totalToBeInserted = importStorage.info.numberOfNewEntries
      result.totalToBeDeleted = importStorage.info.numberOfDeletedEntries
      result.totalToBeUpdated = importStorage.info.numberOfModifiedEntries
    }
  }

  /**
   * For displaying purposes (e. g. progress, errors etc.). Use pure text or markdown format.
   */
  override val info: String
    get() {
      val sb = StringBuilder()
      sb.append(super.info)
        .appendLine()
        .appendLine()
        .append(result.markdown)
      return sb.toString()
    }
}
