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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import mu.KotlinLogging
import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.StringHelper
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler.Companion.KEEP_TERMINATED_JOBS_INTERVALL_MS
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.MarkdownBuilder
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.math.absoluteValue

private val log = KotlinLogging.logger {}

/**
 * IMPORTANT: Don't forget to check [isActive] in your import job in every loop to enable cancellation.
 * @param title Human readable title for displaying and logging purposes.
 * @param area Area describing type of job. If [queueStrategy] is true then jobs of same area and same user are queued.
 * @param ownerId Job is started by this user.
 * @param queueName For queueing strategy you may define multiple queues per area, If not given, only one queue is used
 * per area (and user).
 * @param queueStrategy Should this job be queued if any other job is already running.
 */
abstract class AbstractJob(
  val title: String,
  val area: String? = null,
  val ownerId: Int? = ThreadLocalUserContext.userId,
  val queueName: String? = null,
  /**
   * If true then jobs of same area, same queueName and same user are queued.
   */
  val queueStrategy: QueueStrategy = QueueStrategy.NONE,
  /**
   * At default, this job will be cancelled after 120s +.
   */
  timeoutSeconds: Int = 120
) : Comparable<AbstractJob> {
  enum class QueueStrategy {
    /**
     * No queuing, every job will be started immediately.
     */
    NONE,

    /**
     * Existing running jobs with same area and title will block new jobs until termination. New jobs will be queued.
     */
    PER_QUEUE,

    /**
     * Same as PER_QUEUE but parallel jobs per queue for different users are allowed.
     */
    PER_QUEUE_AND_USER,

    /**
     * New jobs are refused if there is already a running job in the same queue.
     */
    REFUSE_PER_QUEUE,

    /**
     * New jobs are refused if there is already a running job in the same queue for the same user.
     */
    REFUSE_PER_QUEUE_AND_USER,
  }

  enum class ErrorCode { TIMEOUT_WHILE_WAITING, REFUSED_BY_ANOTHER_RUNNING_JOB }

  // Order of status used for ordering job list:
  enum class Status(val key: String) : I18nEnum {
    RUNNING("running"), WAITING("waiting"), FINISHED("finished"), FAILED("failed"), CANCELLED("cancelled"),
    REFUSED("refused");

    /**
     * @return The full i18n key including the i18n prefix "book.type.".
     */
    override val i18nKey: String
      get() = "jobs.job.status.$key"
  }

  @Autowired
  protected lateinit var accessChecker: AccessChecker

  @JsonIgnore
  internal lateinit var coroutinesJob: Job

  var id: Int = ++counter

  var exception: Exception? = null

  var errorMessage: String? = null

  val createdTime: Date = Date()
  val createdTimeMillis = createdTime.time

  var startTime: Date? = null
    internal set
  val startTimeMillis: Long?
    get() = startTime?.time

  var terminatedTime: Date? = null
    internal set
  val terminatedTimeMillis: Long?
    get() = terminatedTime?.time

  val lastActionTime: Date
    get() = terminatedTime ?: startTime ?: createdTime

  var status: Status = Status.WAITING
    internal set

  var totalNumber: Int = -1
    protected set

  var processedNumber: Int = -1
    protected set

  val progressPercentage: Int
    get() = if (processedNumber < 0 || totalNumber <= 0) 0 else processedNumber * 100 / totalNumber

  /**
   * Checks, if the current logged-in user is the owner (ownerId).
   */
  val isOwner: Boolean
    get() = ThreadLocalUserContext.userId == ownerId

  /**
   * For displaying purposes (e. g. progress, errors etc.). Use pure text or markdown format.
   */
  open val info: String
    get() {
      val md = MarkdownBuilder()
      md.appendPipedValue("status", translate(status.i18nKey)) // user, started, terminated, status, title
      UserGroupCache.getInstance().getUser(ownerId)?.let { user ->
        md.appendPipedValue("user", user.getFullname())
      }
      startTime?.let { started ->
        md.appendPipedValue("jobs.job.startedAt", TimeAgo.getMessage(started))
      }
      terminatedTime?.let { terminated ->
        md.appendPipedValue("jobs.job.terminatedAt", TimeAgo.getMessage(terminated))
        startTime?.let { started ->
          md.appendPipedValue(
            "jobs.job.runtime", displayDuration(started, terminated)
          )
        }
      }
      md.appendPipedValue("jobs.job.title", title)
      return md.toString()
    }

  val timeout = timeoutSeconds * 1000

  val timeoutReached: Boolean
    get() {
      if (status != Status.RUNNING)
        return false
      startTimeMillis.let {
        return it != null && System.currentTimeMillis() - it > timeout
      }
    }

  /**
   * Job is deleted and is to be deleted after [KEEP_TERMINATED_JOBS_INTERVALL_MS] (1 hour).
   */
  val terminatedForDeletion: Boolean
    get() {
      if (status == Status.RUNNING || status == Status.WAITING) {
        return false
      }
      val ms = terminatedTimeMillis
      return ms == null || System.currentTimeMillis() - ms > KEEP_TERMINATED_JOBS_INTERVALL_MS
    }

  /**
   * @return true if status is finished, cancelled or failed.
   */
  val terminated: Boolean
    get() = TERMINATED_STATUS_VALUES.contains(status)

  internal suspend fun start() {
    startTime = Date()
    status = Status.RUNNING
    try {
      log.info { "Job is starting: $logInfo" }
      run()
    } catch (ex: Exception) {
      if (ex is CancellationException) {
        // OK, not failed.
      } else {
        status = Status.FAILED
        log.error("Error while executing job $logInfo: ${ex.message}", ex)
        exception = ex
        errorMessage = ex.message
        onAfterException(ex)
        onAfterTermination()
      }
    }
  }

  /**
   * This job is only able to block any other job, if this job is running and is matching the queueing strategy
   * of the given new job.
   * @param newJob Checks if this job is blocking the given newJob.
   */
  fun isBlocking(newJob: AbstractJob): Status? {
    if (newJob.queueStrategy == QueueStrategy.NONE || newJob.status != Status.WAITING || status != Status.RUNNING) {
      // Other job is not marked to be queued or this job isn't running or the other job isn't waiting for RUNNING.
      return null
    }
    if (area != newJob.area) {
      return null
    }
    var blocked = false
    when (newJob.queueStrategy) {
      QueueStrategy.PER_QUEUE_AND_USER, QueueStrategy.REFUSE_PER_QUEUE_AND_USER -> {
        blocked = ownerId == newJob.ownerId
      }
      else -> {
        blocked = true
      }
    }
    if (!blocked) {
      return null
    }
    if (newJob.queueStrategy == QueueStrategy.REFUSE_PER_QUEUE || newJob.queueStrategy == QueueStrategy.REFUSE_PER_QUEUE_AND_USER) {
      return Status.REFUSED
    }
    return Status.WAITING
  }

  /**
   * Checks for status running and cancellation.
   */
  protected val isActive: Boolean
    get() = this.status == Status.RUNNING && this.coroutinesJob.isActive

  internal fun markJobAsFailed(ex: Exception? = null, errorMessage: String? = ex?.message) {
    status = Status.FAILED
    setError(ex, errorMessage)
  }

  internal fun markJobAsRefused(ex: Exception? = null, errorMessage: String? = ex?.message) {
    status = Status.REFUSED
    setError(ex, errorMessage)
  }

  private fun setError(ex: Exception? = null, errorMessage: String? = ex?.message) {
    if (ex != null) {
      exception = ex
    }
    if (errorMessage != null) {
      this.errorMessage = errorMessage
    }
  }

  abstract suspend fun run()


  open fun onBeforeStart() {}

  /**
   * IMPORTANT: Don't forget to check [isActive] in your import job in every loop to enable cancellation.
   */
  open fun onBeforeCancel() {}

  /**
   * IMPORTANT: Don't forget to check [isActive] in your import job in every loop to enable cancellation.
   */
  open fun onAfterCancel() {}

  /**
   * Job failed.
   */
  open fun onAfterFailure(error: ErrorCode) {}

  open fun onAfterFinish() {}

  open fun onAfterException(ex: Exception) {}

  /**
   * Will be called after termination (finished, failed, cancelled).
   */
  open fun onAfterTermination() {}

  val logInfo: String
    get() {
      val sb = StringBuilder()
      sb.append("Job #$id")
      area.let {
        sb.append(", area=$it")
      }
      queueName.let {
        sb.append(", queueName=$it")
      }
      ownerId.let {
        sb.append(", user=$it")
      }
      sb.append(", title=$title")
      return sb.toString()
    }

  internal fun onFinish() {
    if (status == Status.RUNNING) {
      log.info { "Job is finished: $logInfo" }
      terminatedTime = Date()
      status = Status.FINISHED
      onAfterFinish()
      onAfterTermination()
    }
  }

  internal fun cancel() {
    log.info { "Job is cancelled: $logInfo" }
    onBeforeCancel()
    coroutinesJob.cancel()
    terminatedTime = Date()
    status = Status.CANCELLED
    onAfterCancel()
    onAfterTermination()
  }

  /**
   * Default implementation is to call [writeAccess]
   */
  open fun readAccess(user: PFUserDO? = ThreadLocalUserContext.user): Boolean {
    return writeAccess(user)
  }

  abstract fun writeAccess(user: PFUserDO? = ThreadLocalUserContext.user): Boolean

  override fun compareTo(other: AbstractJob): Int {
    if (id == other.id) {
      return 0 // Equals
    }
    if ((status == Status.RUNNING || other.status == Status.RUNNING) && status != other.status) {
      return if (status == Status.RUNNING) -1 else 1 // Show running jobs first.
    }
    if ((status == Status.WAITING || other.status == Status.WAITING) && status != other.status) {
      return if (status == Status.WAITING) -1 else 1 // Show waiting jobs second.
    }

    compare(other.lastActionTime, lastActionTime).let { if (it != 0) return it }
    title.compareTo(other.title, ignoreCase = true).let { if (it != 0) return it }
    return id.compareTo(other.id) // OK, no more order fields.
  }

  private fun compare(date1: Date?, date2: Date?): Int {
    if (date1 == null || date2 == null) {
      return 0 // Can't compare -> assume equals.
    }
    return date1.compareTo(date2)
  }

  override fun toString(): String {
    return ToStringBuilder(this)
      .append("id", id)
      .append("title", title)
      .append("area", area)
      .append("userId", ownerId)
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
    private val TERMINATED_STATUS_VALUES = arrayOf(
      Status.CANCELLED,
      Status.FINISHED,
      Status.FAILED,
      Status.REFUSED,
    )

    fun displayDuration(start: Date, stop: Date? = Date()): String {
      val sb = StringBuilder()
      var seconds = ((stop ?: Date()).time - start.time).absoluteValue / 1000
      val hours = seconds / 3600
      if (hours > 0) {
        seconds -= hours * 3600
        sb.append(StringHelper.format2DigitNumber(hours))
          .append(":")
      }
      val minutes = seconds / 60
      if (minutes > 0) {
        seconds -= minutes * 60
      }
      sb.append(StringHelper.format2DigitNumber(minutes))
        .append(":")
        .append(StringHelper.format2DigitNumber(seconds))
        .append("s")
      return sb.toString()
    }
  }
}
