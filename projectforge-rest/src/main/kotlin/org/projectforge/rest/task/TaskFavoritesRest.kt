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

package org.projectforge.rest.task

import org.projectforge.business.task.TaskFavorite
import org.projectforge.business.task.TaskFavoritesService
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * For serving the user's favorite tasks for quick select.
 */
@RestController
@RequestMapping("${Rest.URL}/task/favorites")
class TaskFavoritesRest {
    @Autowired
    private lateinit var taskFavorites: TaskFavoritesService


    @GetMapping("list")
    fun getList(): List<TaskFavorite> {
        return taskFavorites.getList()
    }

    /**
     * Adds new favorite task with given id under the given name.
     * @return new list of favorites.
     */
    @GetMapping("create")
    fun new(@RequestParam("taskId", required = true) taskId: Int, @RequestParam("name", required = true) name: String): List<TaskFavorite> {
        return taskFavorites.createFavorite(name, taskId)
    }

    /**
     * Selects the task id from the filter.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("select")
    fun select(@RequestParam("id", required = true) id: Int): Int? {
        return taskFavorites.selectTaskId(id)
    }

    /**
     * Selects the task id from the filter.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("delete")
    fun delete(@RequestParam("id", required = true) id: Int):  List<TaskFavorite> {
        return taskFavorites.deleteFavorite(id)
    }

    /**
     * Selects the task id from the filter.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("rename")
    fun rename(@RequestParam("id", required = true) id: Int, @RequestParam("newName", required = true) newName: String):  List<TaskFavorite> {
        return taskFavorites.renameFavorite(id, newName)
    }
}
