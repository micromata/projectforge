package org.projectforge.business.task

import org.projectforge.business.user.UserPrefDao
import org.projectforge.business.user.service.UserPreferencesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TaskFavorites {
    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    fun getList(): List<TaskFavorite> {
        val userPrefs = userPrefDao.getList(AREA_ID)
        return userPrefs.map { TaskFavorite(it.name, it.id) }
    }

    companion object {
        private val AREA_ID = "TASK_FAVORITE"
    }
}
