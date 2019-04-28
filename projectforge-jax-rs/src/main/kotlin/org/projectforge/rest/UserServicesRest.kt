package org.projectforge.rest

import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/user")
class UserServicesRest {

    private val log = org.slf4j.LoggerFactory.getLogger(UserServicesRest::class.java)

    @Autowired
    private lateinit var userDao: UserDao

    private val restHelper = RestHelper()

    /**
     * Gets the task data including kost2 information if any and its path.
     * @param id Task id.
     * @return json
     */
    @GetMapping("aco")
    fun getAutocCmpletionObjects(@RequestParam("search") searchString: String?): List<PFUserDO> {
        val filter = BaseSearchFilter()
        filter.setSearchFields("username", "firstname", "lastname", "email")
        filter.searchString = searchString
        val result = userDao.getList(filter)
        if (searchString.isNullOrBlank())
            result.removeIf { it.isDeactivated } // Remove deactivated users when returning all. Show deactivated users only if search string is given.
        return result
    }
}
