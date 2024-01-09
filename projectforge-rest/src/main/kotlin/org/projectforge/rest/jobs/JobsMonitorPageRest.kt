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

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
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
@RequestMapping("${Rest.URL}/jobsMonitor")
class JobsMonitorPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var jobHandler: JobHandler

  /**
   * @param id Job id of [JobHandler]
   */
  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("jobId") jobId: Int?,
    @RequestParam("all") all: Boolean?,
  ): FormLayoutData {
    val layout = UILayout("jobs.monitor.title")
    val jobMonitor = UICustomized("jobs.monitor")
    if (jobId != null) {
      jobMonitor.add("jobId", jobId)
    }
    jobMonitor.add("cancelConfirmMessage", translate("jobs.job.cancel.confirmationMessage"));
    layout.add(jobMonitor)
    LayoutUtils.process(layout)
    layout.addTranslations("jobs.monitor.noJobsAvailable")
    return FormLayoutData(null, layout, createServerData(request), variables = getJobsAsVariable(jobId, all))
  }

  /**
   * @param jobId Job id of [JobHandler] to get.
   * @param all If true, all jobs of the user will be returned.
   */
  @GetMapping("jobs")
  fun getJobs(@RequestParam("jobId") jobId: Int?, @RequestParam("all") all: Boolean?): ResponseAction {
    return ResponseAction(
      targetType = TargetType.UPDATE,
      merge = true,
    ).addVariable("variables", getJobsAsVariable(jobId, all))
  }

  /**
   * @param id Job id of [JobHandler]
   */
  @GetMapping("cancel")
  fun cancelJob(
    @RequestParam("jobId", required = true) jobId: Int,
    @RequestParam("all") all: Boolean?
  ): ResponseAction {
    jobHandler.getJobById(jobId)?.let { job ->
      jobHandler.cancelJob(job)
    }
    return ResponseAction(
      targetType = TargetType.UPDATE,
      merge = true,
    ).addVariable("variables", getJobsAsVariable(jobId, all))
  }

  private fun getJobsAsVariable(jobId: Int?, all: Boolean?): MutableMap<String, Any> {
    val jobs = jobHandler.getJobsOfUser(jobId, all != false).map { job -> JobInfo.create(job) }
    return mutableMapOf("jobs" to jobs)
  }
}
