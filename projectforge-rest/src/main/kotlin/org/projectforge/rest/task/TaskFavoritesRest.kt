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

package org.projectforge.rest.task

import org.projectforge.business.task.TaskFavoritesService
import org.projectforge.business.task.TaskTree
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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

    /**
     * A favorite for the client: its name, and the whole path of the referenced task (root first, its own
     * title last), so the picker can show the path behind a favorite as a tooltip (`formatPath`, the same
     * label the recent list and the search hits carry). The path is resolved per favorite because the
     * stored favorite keeps only the task id.
     */
    class TaskFavoriteInfo(val id: Long?, val name: String?, val pathAsString: String?)

    @GetMapping("list")
    fun getList(): List<TaskFavoriteInfo> {
        return toInfoList()
    }

    /**
     * Adds new favorite task with given id under the given name.
     * @return new list of favorites.
     */
    @GetMapping("create")
    fun new(@RequestParam("taskId", required = true) taskId: Long, @RequestParam("name", required = true) name: String): List<TaskFavoriteInfo> {
        taskFavorites.createFavorite(name, taskId)
        return toInfoList()
    }

    /**
     * Selects the task id from the filter.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("select")
    fun select(@RequestParam("id", required = true) id: Long): Long? {
        return taskFavorites.selectTaskId(id)
    }

    /**
     * Selects the task id from the filter.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("delete")
    fun delete(@RequestParam("id", required = true) id: Long): List<TaskFavoriteInfo> {
        taskFavorites.deleteFavorite(id)
        return toInfoList()
    }

    /**
     * Selects the task id from the filter.
     * @return taskId referenced by given favorite.
     */
    @GetMapping("rename")
    fun rename(@RequestParam("id", required = true) id: Long, @RequestParam("newName", required = true) newName: String): List<TaskFavoriteInfo> {
        taskFavorites.renameFavorite(id, newName)
        return toInfoList()
    }

    // POST variants for the Next.js frontend, which sends the CSRF token on state changing calls. They
    // delegate to the same service as the GET mappings above (kept for the legacy React frontend).

    @PostMapping("create")
    fun createPost(@RequestParam("taskId", required = true) taskId: Long, @RequestParam("name", required = true) name: String): List<TaskFavoriteInfo> {
        taskFavorites.createFavorite(name, taskId)
        return toInfoList()
    }

    @PostMapping("select")
    fun selectPost(@RequestParam("id", required = true) id: Long): Long? {
        return taskFavorites.selectTaskId(id)
    }

    @PostMapping("delete")
    fun deletePost(@RequestParam("id", required = true) id: Long): List<TaskFavoriteInfo> {
        taskFavorites.deleteFavorite(id)
        return toInfoList()
    }

    @PostMapping("rename")
    fun renamePost(@RequestParam("id", required = true) id: Long, @RequestParam("newName", required = true) newName: String): List<TaskFavoriteInfo> {
        taskFavorites.renameFavorite(id, newName)
        return toInfoList()
    }

    /**
     * The favorites with the referenced task's whole path resolved for the tooltip. The favorite keeps
     * only the task id, so the path is looked up per favorite (the list is a handful of entries).
     */
    private fun toInfoList(): List<TaskFavoriteInfo> {
        return taskFavorites.getListWithTaskId().map { favorite ->
            TaskFavoriteInfo(favorite.id, favorite.name, formatPath(favorite.taskId))
        }
    }

    /**
     * The whole path of the task, root first and the task's own title last, joined by ` | ` — the same
     * label the recent list and the search hits use (see `TaskServicesRest.formatPath`). Null for a
     * favorite whose task no longer resolves.
     */
    private fun formatPath(taskId: Long?): String? {
        taskId ?: return null
        val path = TaskTree.instance.getPathToRoot(taskId)
        if (path.isEmpty()) {
            return translate("task.path.rootTask")
        }
        return path.joinToString(" | ") { it.task.title ?: "" }
    }
}
