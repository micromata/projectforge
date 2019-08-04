/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetFavorite
import org.projectforge.business.timesheet.TimesheetFavoritesService
import org.projectforge.business.user.service.UserService
import org.projectforge.rest.config.Rest
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
    class NewTimesheetFavorite(var name: String, var timesheet: TimesheetDO)

    @Autowired
    private lateinit var timsheetFavoritesService: TimesheetFavoritesService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @GetMapping("list")
    fun getList(): List<TimesheetFavorite> {
        return timsheetFavoritesService.getList()
    }

    /**
     * Adds new favorite timesheet with given id under the given name.
     * @return new list of favorites.
     */
    @RequestMapping("create")
    fun new(@RequestBody newFavorite: NewTimesheetFavorite): Map<String, Any> {
        val favorite = TimesheetFavorite()
        favorite.name = newFavorite.name
        favorite.fillFromTimesheet(newFavorite.timesheet)
        timsheetFavoritesService.createFavorite(favorite)
        return mapOf("timesheetFavorites" to getList())
    }

    /**
     * Selects the timesheet favorite and prefills the edit data.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("select")
    fun select(@RequestParam("id", required = true) id: Int): Map<String, Any> {
        val fav = timsheetFavoritesService.selectTimesheet(id) ?: return mapOf()
        val timesheet = TimesheetDO()
        fav.copyToTimesheet(timesheet)
        if (timesheet.taskId != null) {
            timesheet.task = TaskTreeHelper.getTaskTree().getTaskById(timesheet.taskId)
        }
        if (timesheet.userId != null) {
            timesheet.user = userService.getUser(timesheet.userId)
        }
        if (timesheet.kost2Id != null) {
            // Load without check access. User needs now select access for using kost2.
            val kost2 = kost2Dao.internalGetById(timesheet.kost2Id)
            timesheet.kost2?.description = kost2?.description
        }
        return mapOf("data" to timesheet)
    }

    /**
     * Deletes the given timesheet template/favorite.
     */
    @GetMapping("delete")
    fun delete(@RequestParam("id", required = true) id: Int): Map<String, Any> {
        timsheetFavoritesService.deleteFavorite(id)
        return mapOf("timesheetFavorites" to getList())
    }

    @GetMapping("rename")
    fun rename(@RequestParam("id", required = true) id: Int, @RequestParam("newName", required = true) newName: String): Map<String, Any> {
        timsheetFavoritesService.renameFavorite(id, newName)
        return mapOf("timesheetFavorites" to getList())
    }
}
