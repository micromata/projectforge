package org.projectforge.business.task

import org.projectforge.business.user.UserPrefDao
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.favorites.Favorites
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TaskFavoritesService {
    private val log = org.slf4j.LoggerFactory.getLogger(TaskFavoritesService::class.java)

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    fun getList(): List<TaskFavorite> {
        val userPrefs = userPrefDao.getListWithoutEntries(AREA_ID)
        return userPrefs.map { TaskFavorite(it.name, it.id) }
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
