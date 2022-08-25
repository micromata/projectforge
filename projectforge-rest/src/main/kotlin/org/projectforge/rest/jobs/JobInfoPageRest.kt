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

package org.projectforge.rest.jobs

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/jobInfo")
class JobInfoPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var jobHandler: JobHandler

  /**
   * @param id Job id of [JobHandler]
   */
  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("id", required = true) id: Int,
  ): FormLayoutData {
    val pair = getJob(id)
    val job = pair.second
    val data = pair.first
    val layout = UILayout("jobs.viewJob.title")
    layout.add(
      UIProgress(
        getProgressId(data),
        fetchUpdateUrl = RestResolver.getRestUrl(this::class.java, "progress", params = mapOf("id" to id))
      )
    )
    return FormLayoutData(data, layout, createServerData(request), variables = getProgressDataAsVariables(data, job))
  }

  /**
   * @param id Job id of [JobHandler]
   */
  @GetMapping("progress")
  fun getProgress(@RequestParam("id", required = true) id: Int): ResponseAction {
    val pair = getJob(id)
    return ResponseAction(
      variables = getProgressDataAsVariables(pair.first, pair.second),
      targetType = TargetType.UPDATE,
      merge = true,
    )
  }

  /**
   * @param id Job id of [JobHandler]
   */
  @GetMapping("cancel")
  fun cancelJob(@RequestParam("id", required = true) id: Int): ResponseAction {
    val pair = getJob(id)
    pair.second?.let { job ->
      jobHandler.cancelJob(job)
    }
    return ResponseAction(
      variables = getProgressDataAsVariables(pair.first, pair.second),
      targetType = TargetType.UPDATE,
      merge = true,
    )
  }

  private fun getJob(id: Int): Pair<JobDTO, AbstractJob?> {
    val job = jobHandler.getJobById(id)
    val data = if (job != null) {
      JobDTO.create(job)
    } else {
      JobDTO(id = id)
    }
    return Pair(data, job)
  }

  private fun getProgressDataAsVariables(jobDTO: JobDTO, job: AbstractJob?): MutableMap<String, Any> {
    return mutableMapOf(getProgressId(jobDTO) to getProgressData(job))
  }

  private fun getProgressData(job: AbstractJob?): UIProgress.Data {
    val title = StringBuilder()
    title.append("${job?.progressPercentage} %")
    job?.let {
      title.append(" (")
        .append(NumberFormatter.format(it.processedNumber))
        .append("/")
        .append(NumberFormatter.format(it.totalNumber))
        .append(")")
    }
    job?.status?.let { status ->
      title.append(": ").append(translate("jobs.status.${status.name.lowercase()}"))
    }
    var color: UIColor? = null
    var anmiated: Boolean? = null
    when (job?.status) {
      AbstractJob.Status.RUNNING -> {
        color = UIColor.INFO
        anmiated = true
      }
      AbstractJob.Status.CANCELLED, AbstractJob.Status.FAILED -> color = UIColor.DANGER
      AbstractJob.Status.FINISHED -> color = UIColor.SUCCESS
      else -> {}
    }
    return UIProgress.Data(job?.progressPercentage, title = title.toString(), color = color, animated = anmiated)
  }

  private fun getProgressId(jobDTO: JobDTO): String {
    return "job${jobDTO.id}"
  }
}
