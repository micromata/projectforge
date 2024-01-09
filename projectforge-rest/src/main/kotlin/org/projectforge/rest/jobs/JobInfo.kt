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

package org.projectforge.rest.jobs

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.rest.dto.User
import org.projectforge.ui.UIColor

/**
 * @param cancelId If given, a cancel button is displayed.
 */
class JobInfo(
  var id: Int? = null,
  var title: String? = null,
  var area: String? = null,
  var queueName: String? = null,
  var user: User? = null,

  var progressPercentage: Int? = 0,
  var progressBarColor: UIColor? = null,
  var progressTitle: String? = null,
  var info: String? = null,
  var infoColor: UIColor? = null,
  var animated: Boolean? = false,
  var cancelId: Int? = null,
) {
  companion object {
    fun create(job: AbstractJob?): JobInfo {
      val info = JobInfo(
        job?.id,
        job?.title,
        area = job?.area,
        queueName = job?.queueName,
        progressPercentage = job?.progressPercentage,
        info = job?.info,
      )
      job?.ownerId?.let { userId ->
        info.user = User.getUser(userId)
      }
      when (job?.status) {
        AbstractJob.Status.RUNNING -> {
          info.animated = true
          info.cancelId = job.id
          info.progressBarColor = UIColor.SUCCESS
        }
        AbstractJob.Status.WAITING -> {
          info.cancelId = job.id
          info.progressBarColor = UIColor.LIGHT
        }
        AbstractJob.Status.FINISHED -> {
          info.progressBarColor = UIColor.INFO
        }
        AbstractJob.Status.FAILED, AbstractJob.Status.CANCELLED, AbstractJob.Status.REFUSED -> {
          info.infoColor = UIColor.DANGER
          info.progressBarColor = UIColor.DANGER
        }
        else -> {}
      }
      job?.let {
        val progressTitle = StringBuilder()
        progressTitle.append("#")
          .append(NumberFormatter.format(job.id))
          .append(", ")
          .append(translate(it.status.i18nKey))
          .append(": ")
          .append(NumberFormatter.format(it.processedNumber))
          .append("/")
          .append(NumberFormatter.format(it.totalNumber))
        info.progressTitle = progressTitle.toString()
      }
      return info
    }
  }
}
