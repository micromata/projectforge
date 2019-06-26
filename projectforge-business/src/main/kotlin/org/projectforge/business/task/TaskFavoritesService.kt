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

package org.projectforge.business.task

import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.Favorites
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TaskFavoritesService {
    private val log = org.slf4j.LoggerFactory.getLogger(TaskFavoritesService::class.java)

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    fun getList(): List<TaskFavorite> {
        val userPrefs = userPrefDao.getListWithoutEntries(AREA_ID)
        return userPrefs.map { TaskFavorite(it.name!!, it.id!!) }
    }

    fun selectTaskId(id: Int): Int? {
        val taskIdString = userPrefDao.getUserPref(AREA_ID, id)?.getUserPrefEntryAsString(PARAMETER)
        try {
            return taskIdString?.toInt()
        } catch (ex: NumberFormatException) {
            log.info("Oups, can't parse task id '$taskIdString' of user's favorite with pk=${id}")
            return null
        }
    }

    fun addFavorite(name: String, taskId: Int): List<TaskFavorite> {
        val favorites = Favorites(getList())
        val newFavorite = TaskFavorite(name)
        favorites.saveNewUserPref(userPrefDao, newFavorite, AREA_ID, PARAMETER, taskId.toString())
        return getList()
    }

    fun deleteFavorite(id: Int): List<TaskFavorite> {
        Favorites.deleteUserPref(userPrefDao, AREA_ID, id)
        return getList()
    }

    fun renameFavorite(id: Int, newName: String): List<TaskFavorite> {
        Favorites.renameUserPref(userPrefDao, AREA_ID, id, newName)
        return getList()
    }

    companion object {
        private val AREA_ID = "TASK_FAVORITE"
        private val PARAMETER = "task"
    }
}
