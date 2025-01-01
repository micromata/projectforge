/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.timesheet

import mu.KotlinLogging
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.RecentQueue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
open class TimesheetRecentService {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  open fun getRecentTimesheets(): List<TimesheetRecentEntry> {
    return getRecentTimesheetsQueue(ThreadLocalUserContext.loggedInUserId!!).recentList ?: emptyList()
  }

  open fun getRecentTimesheet(): TimesheetRecentEntry? {
    return getRecentTimesheetsQueue(ThreadLocalUserContext.loggedInUserId!!).recent
  }

  /**
   * Adds given timesheet to recent queue as well as any location and/or task id if given.
   */
  open fun addRecentTimesheet(timesheet: TimesheetDO) {
    addRecentTimesheet(TimesheetRecentEntry(timesheet))
  }

  open fun addRecentTimesheet(entry: TimesheetRecentEntry) {
    if (entry.description.isNullOrBlank() && entry.location.isNullOrBlank() && entry.taskId == null) {
      // Don't append empty entries.
      return
    }
    getRecentTimesheetsQueue(ThreadLocalUserContext.loggedInUserId!!).append(entry)
    if (!entry.location.isNullOrBlank()) {
      getRecentLocationsQueue(ThreadLocalUserContext.loggedInUserId!!).append(entry.location)
    }
    entry.taskId?.let {
      getRecentTaskIdsQueue(ThreadLocalUserContext.loggedInUserId!!).append(it)
    }
  }

  open fun getRecentLocations(): List<String> {
    return getRecentLocationsQueue(ThreadLocalUserContext.loggedInUserId!!).recentList ?: emptyList()
  }

  open fun getRecentTaskIds(): List<Long> {
    return getRecentTaskIdsQueue(ThreadLocalUserContext.loggedInUserId!!).recentList ?: emptyList()
  }

  private fun getRecentTimesheetsQueue(userId: Long): RecentQueue<TimesheetRecentEntry> {
    return getRecentQueue(userId, "recent.timesheets") { recentQueue -> readRecentTimesheets(recentQueue, userId) }
  }

  private fun getRecentLocationsQueue(userId: Long): RecentQueue<String> {
    return getRecentQueue(userId, "recent.locations") { recentQueue -> readRecentLocations(recentQueue, userId) }
  }

  private fun getRecentTaskIdsQueue(userId: Long): RecentQueue<Long> {
    return getRecentQueue(userId, "recent.taskIds") { recentQueue -> readRecentTaskIds(recentQueue, userId) }
  }

  private fun <T> getRecentQueue(userId: Long, id: String, read: (recentQueue: RecentQueue<T>) -> Unit): RecentQueue<T> {
    var recentQueue: RecentQueue<T>? = null
    try {
      @Suppress("UNCHECKED_CAST")
      recentQueue = userPrefService.getEntry(PREF_AREA, id, RecentQueue::class.java, userId) as? RecentQueue<T>
    } catch (ex: Exception) {
      log.error("Unexpected exception while getting recent $id for user #$userId: ${ex.message}.", ex)
    }
    if (recentQueue == null || recentQueue.size() == 0) {
      recentQueue = RecentQueue()
      // Put as volatile entry (will be created after restart oder re-login.
      userPrefService.putEntry(PREF_AREA, id, recentQueue, false, userId)
      read(recentQueue)
    }
    return recentQueue
  }


  private fun readRecentTimesheets(recentQueue: RecentQueue<TimesheetRecentEntry>, userId: Long) {
    log.info { "Getting recent timesheets for user #$userId." }
    val added = mutableSetOf<TimesheetRecentEntry>()
    val list = timesheetDao.select(getTimesheetFilter(userId))
    for (timesheet in list) {
      val entry = TimesheetRecentEntry(
        taskId = timesheet.taskId,
        userId = timesheet.userId,
        kost2Id = timesheet.kost2Id,
        location = timesheet.location,
        tag = timesheet.tag,
        reference = timesheet.reference,
        description = timesheet.description
      )
      if (!added.contains(entry)) {
        added.add(entry)
        recentQueue.addOnly(entry)
        if (added.size >= MAX_RECENT) {
          break
        }
      }
    }
    log.info { "Found ${recentQueue.size()}/$MAX_RECENT distinct entries in ${list.size} timesheets of user #$userId since ${sinceDate.isoString}." }
  }

  private fun readRecentLocations(recentQueue: RecentQueue<String>, userId: Long) {
    log.info { "Getting recent timesheet locations for user #$userId." }
    val added = mutableSetOf<String>()
    val list = timesheetDao.getRecentLocation(sinceDate.utilDate)
    for (location in list) {
      if (!added.contains(location)) {
        added.add(location)
        recentQueue.addOnly(location)
        if (added.size >= MAX_RECENT) {
          break
        }
      }
    }
    log.info { "Found ${recentQueue.size()}/$MAX_RECENT distinct locations of ${list.size} timesheet locations of user #$userId since ${sinceDate.isoString}." }
  }

  private fun readRecentTaskIds(recentQueue: RecentQueue<Long>, userId: Long) {
    log.info { "Getting recent timesheet task id's for user #$userId." }
    val added = mutableSetOf<Long>()
    val list = timesheetDao.select(getTimesheetFilter(userId))
    for (timesheet in list) {
      val taskId = timesheet.taskId ?: continue
      if (!added.contains(taskId)) {
        added.add(taskId)
        recentQueue.addOnly(taskId)
        if (added.size >= MAX_RECENT) {
          break
        }
      }
    }
    log.info { "Found ${recentQueue.size()}/$MAX_RECENT distinct task id's of ${list.size} timesheets of user #$userId since ${sinceDate.isoString}." }
  }

  private val sinceDate
    get() = PFDateTime.now().minusYears(YEARS_AGO).beginOfDay

  private fun getTimesheetFilter(userId: Long): TimesheetFilter {
    val filter = TimesheetFilter()
    filter.userId = userId
    filter.startTime = sinceDate.utilDate
    return filter
  }

  companion object {
    private const val MAX_RECENT = 50
    private const val YEARS_AGO = 1L

    private val PREF_AREA = TimesheetRecentService::class.java.name
  }
}
