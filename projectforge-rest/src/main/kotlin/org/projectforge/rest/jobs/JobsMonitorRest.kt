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

package org.projectforge.rest.jobs

import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The background jobs of the logged-in user, polled by projectforge-next: by the job monitor page
 * (`/next/jobsMonitor`) and by the progress toasts of every page. The dynamic React page it served formerly
 * (`react/jobsMonitor/dynamic`) is gone without a redirect: it was never linked from outside ProjectForge.
 *
 * Nobody is refused as a whole: [JobHandler] answers only the jobs the user may read and cancels only the ones
 * the user may write ([org.projectforge.framework.jobs.AbstractJob.readAccess] / `writeAccess`).
 */
@RestController
@RequestMapping("${Rest.URL}/jobsMonitor")
class JobsMonitorRest {
    @Autowired
    private lateinit var jobHandler: JobHandler

    /**
     * @param jobId Job id of [JobHandler], which is put first if the user may read it.
     * @param all If not false, all jobs of the user are returned.
     */
    @AccessChecked("Own jobs only (JobHandler: readAccess per job)")
    @GetMapping("jobs")
    fun getJobs(
        @RequestParam("jobId") jobId: Int?,
        @RequestParam("all") all: Boolean?,
    ): ResponseAction {
        return ResponseAction(
            targetType = TargetType.UPDATE,
            merge = true,
        ).addVariable("variables", getJobsAsVariable(jobId, all))
    }

    /**
     * @param jobId Job id of [JobHandler] to cancel.
     * @return The jobs of the user, the cancelled one included.
     */
    @AccessChecked("Own jobs only (JobHandler: writeAccess per job)")
    @PostMapping("cancel")
    fun cancelJob(
        @RequestParam("jobId", required = true) jobId: Int,
        @RequestParam("all") all: Boolean?,
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
