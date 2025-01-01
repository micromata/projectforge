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

package org.projectforge.rest

import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetFavorite
import org.projectforge.business.timesheet.TimesheetFavoritesService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.Timesheet
import org.projectforge.rest.task.TaskServicesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * For serving the user's favorite tasks for quick select.
 */
@RestController
@RequestMapping("${Rest.URL}/timesheet/favorites")
class TimesheetFavoritesRest {
  /**
   * Only for rest call to create a new timesheet favorite.
   */
  class NewTimesheetFavorite(var name: String, var timesheet: Timesheet)

  /**
   * Only for rest call to select a timesheet favorite.
   */
  class SelectTimesheetFavorite(var id: Long, var timesheet: Timesheet)

  @Autowired
  private lateinit var timsheetFavoritesService: TimesheetFavoritesService

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var kost2Dao: Kost2Dao

  @Autowired
  private lateinit var taskTree: TaskTree

  @GetMapping("list")
  fun getList(): List<TimesheetFavorite> {
    return timsheetFavoritesService.getList()
  }

  /**
   * Adds new favorite timesheet with given name.
   * @return new list of favorites.
   */
  @RequestMapping("create")
  fun new(@RequestBody newFavorite: NewTimesheetFavorite): Map<String, Any> {
    val favorite = TimesheetFavorite()
    favorite.name = newFavorite.name
    val timesheetDO = TimesheetDO()
    newFavorite.timesheet.copyTo(timesheetDO)
    favorite.fillFromTimesheet(timesheetDO)
    timsheetFavoritesService.createFavorite(favorite)
    return mapOf("timesheetFavorites" to getList())
  }

  /**
   * Selects the timesheet favorite and prefills the edit data.
   */
  @RequestMapping("select")
  fun select(@RequestBody selectFavorite: SelectTimesheetFavorite): Map<String, Any> {
    val fav = timsheetFavoritesService.selectTimesheet(selectFavorite.id) ?: return mapOf()
    val timesheet = TimesheetDO()
    fav.copyToTimesheet(timesheet)
    val result = mutableMapOf<String, Any>("data" to timesheet)
    if (timesheet.taskId != null) {
      val task = taskTree.getTaskById(timesheet.taskId)
      timesheet.task = task
      result["variables"] = mapOf("task" to TaskServicesRest.createTask(timesheet.taskId))
    }
    if (timesheet.userId != null) {
      timesheet.user = userService.getUser(timesheet.userId)
    }
    if (timesheet.kost2Id != null) {
      // Load without check access. User needs now select access for using kost2.
      val kost2 = kost2Dao.find(timesheet.kost2Id, checkAccess = false)
      timesheet.kost2?.description = kost2?.description
    }
    return result
  }

  /**
   * Deletes the given timesheet template/favorite.
   */
  @GetMapping("delete")
  fun delete(@RequestParam("id", required = true) id: Long): Map<String, Any> {
    timsheetFavoritesService.deleteFavorite(id)
    timsheetFavoritesService.refreshMigrationCache(ThreadLocalUserContext.loggedInUserId!!)
    return mapOf("timesheetFavorites" to getList())
  }

  @GetMapping("rename")
  fun rename(
    @RequestParam("id", required = true) id: Long,
    @RequestParam("newName", required = true) newName: String
  ): Map<String, Any> {
    timsheetFavoritesService.renameFavorite(id, newName)
    timsheetFavoritesService.refreshMigrationCache(ThreadLocalUserContext.loggedInUserId!!)
    return mapOf("timesheetFavorites" to getList())
  }

  @GetMapping("migrateOldTemplates")
  fun migrateOldTemplates(): Map<String, Any> {
    timsheetFavoritesService.migrateFromLegacyFavorites(timsheetFavoritesService.getFavorites())
    return mapOf("timesheetFavorites" to getList())
  }
}
