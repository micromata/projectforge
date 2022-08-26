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

package org.projectforge.framework.jobs

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

@Service
class JobHandler {
  private val jobs = mutableListOf<AbstractJob>()

  // Runs every minute
  @Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000)
  fun tidyUp() {
    runningJobs.forEach { job ->
      if (job.timeoutReached) {
        internalCancelJob(job)
      }
    }
    terminatedJobs.forEach { job ->
      job.terminatedTimeMillis.let {
        if (it == null || System.currentTimeMillis() - it > KEEP_TERMINATED_JOBS_INTERVALL_MS) {
          synchronized(jobs) {
            jobs.remove(job)
          }
        }
      }
    }
  }

  /**
   * @return Given job for chaining.
   */
  fun addJob(job: AbstractJob): AbstractJob {
    synchronized(jobs) {
      jobs.add(job)
    }
    thread {
      runBlocking {
        job.coroutinesJob = launch {
          job.start()
        }
        job.coroutinesJob.join()
        job.onFinish()
      }
    }
    return job
  }

  fun getJobById(id: Int): AbstractJob? {
    val job = synchronized(jobs) {
      jobs.find { it.id == id }
    } ?: return null
    if (job.readAccess()) {
      return job
    }
    log.warn { "Logged-in user ${ThreadLocalUserContext.getUser()?.username} has no read access to requested job." }
    return null
  }

  fun cancelJob(job: AbstractJob) {
    if (job.writeAccess()) {
      internalCancelJob(job)
    }
    log.warn { "Logged-in user ${ThreadLocalUserContext.getUser()?.username} has no write access to requested job. So job will not be cancelled." }
  }

  /**
   * @param jobId If given, this job will be the first of the result list (if the user has read-access.
   * @param all If true, all jobs of the user will be part of the result list. The list is ordered by status and timestamps.
   */
  fun getJobsOfUser(jobId: Int?, all: Boolean? = true): List<AbstractJob> {
    val list = mutableListOf<AbstractJob>()
    if (all == true) {
      synchronized(jobs) {
        list.addAll(jobs.filter { it.readAccess() })
      }
    }
    list.sort()
    if (jobId != null) {
      val job = getJobById(jobId)
      if (job != null) {
        list.removeIf { it.id == job.id }
        list.add(0, job)
      }
    }
    return list
  }

  private fun internalCancelJob(job: AbstractJob) {
    log.warn { "Job ${job.logInfo} is going to be cancelled." }
    job.cancel()
  }

  private val runningJobs: List<AbstractJob>
    get() {
      synchronized(jobs) {
        return jobs.filter { it.status == AbstractJob.Status.RUNNING }
      }
    }

  /**
   * Finished and cancelled jobs.
   */
  private val terminatedJobs: List<AbstractJob>
    get() {
      synchronized(jobs) {
        return jobs.filter { it.terminated }
      }
    }

  @PreDestroy
  fun shutdownJobHandler() {
    runBlocking {
      runningJobs.forEach { job ->
        if (job.status == AbstractJob.Status.RUNNING) {
          job.cancel()
          job.coroutinesJob.join()
        }
      }
    }
  }

  companion object {
    /**
     * Keep terminated jobs not older than this given intervall in ms.
     */
    internal const val KEEP_TERMINATED_JOBS_INTERVALL_MS = Constants.MILLIS_PER_HOUR
  }
}
