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

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.coroutines.Job
import mu.KotlinLogging
import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.framework.jobs.JobHandler.Companion.KEEP_TERMINATED_JOBS_INTERVALL_MS
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @param title Human readable title for displaying and logging purposes.
 * @param area Area describing type of job. If [queueStrategy] is true then jobs of same area and same user are queued.
 * @param userId Job is started by this user.
 */
abstract class AbstractJob(
  val title: String,
  val area: String? = null,
  val userId: Int? = null,
  /**
   * If true then jobs of same area and same user are queued.
   */
  val queueStrategy: QueueStrategy = QueueStrategy.NONE,
  /**
   * At default, this job will be cancelled after 120s +.
   */
  timeoutSeconds: Int = 120
) {
  enum class QueueStrategy { NONE, GLOBAL, PER_USER }
  enum class Status { WAITING, RUNNING, CANCELLED, FINISHED }

  @JsonIgnore
  internal lateinit var coroutinesJob: Job

  var id: Int = ++counter

  var startTime: Date? = null
    internal set
  val startTimeMillis: Long?
    get() = startTime?.time

  var terminatedTime: Date? = null
    internal set
  val terminatedTimeMillis: Long?
    get() = terminatedTime?.time

  var status: Status = Status.WAITING
    internal set

  var totalNumber: Int = -1
    protected set

  var processedNumber: Int = -1
    protected set

  val progressPercentage: Int
    get() = if (processedNumber < 0 || totalNumber <= 0) 0 else processedNumber * 100 / totalNumber

  val timeout = timeoutSeconds * 1000

  val timeoutReached: Boolean
    get() {
      if (status != Status.RUNNING)
        return false
      startTimeMillis.let {
        return it != null && System.currentTimeMillis() - it > timeout
      }
    }

  val terminatedForDeletion: Boolean
    get() {
      if (status == Status.RUNNING || status == Status.WAITING) {
        return false
      }
      val ms = terminatedTimeMillis
      return ms == null || System.currentTimeMillis() - ms > KEEP_TERMINATED_JOBS_INTERVALL_MS
    }


  internal suspend fun start() {
    startTime = Date()
    status = Status.RUNNING
    run()
  }

  fun isBlocking(newJob: AbstractJob): Boolean {
    if (newJob.queueStrategy == QueueStrategy.NONE || newJob.status != Status.WAITING && status != Status.RUNNING) {
      // Other job is not marked to be queued or this job isn't running or the other job isn't waiting for RUNNING.
      return false
    }
    if (area != newJob.area) {
      return false
    }
    return newJob.queueStrategy != QueueStrategy.PER_USER || userId == newJob.userId
  }

  abstract suspend fun run()

  open fun onBeforeCancel() {}

  open fun onAfterCancel() {}

  open fun onAfterFinish() {}

  val logInfo: String
    get() {
      val sb = StringBuilder()
      sb.append("Job #$id")
      area.let {
        sb.append(", area=$it")
      }
      userId.let {
        sb.append(", user=$it")
      }
      sb.append(", title=$title")
      return sb.toString()
    }

  internal fun finished() {
    log.info { "Job is finished: $logInfo" }
    terminatedTime = Date()
    status = Status.FINISHED
    onAfterFinish()
  }

  internal fun cancel() {
    log.info { "Job is cancelled: $logInfo" }
    onBeforeCancel()
    coroutinesJob.cancel()
    terminatedTime = Date()
    status = Status.CANCELLED
    onAfterCancel()
  }

  override fun toString(): String {
    return ToStringBuilder(this)
      .append("id", id)
      .append("title", title)
      .append("area", area)
      .append("userId", userId)
      .append("status", status)
      .append("startTime", startTime?.toString())
      .append("terminatedTime", terminatedTime?.toString())
      .append("timeoutReached", timeoutReached)
      .append("terminatedForDeletion", terminatedForDeletion)
      .append("progress", progressPercentage)
      .toString()
  }

  companion object {
    private var counter = 0
  }
}
