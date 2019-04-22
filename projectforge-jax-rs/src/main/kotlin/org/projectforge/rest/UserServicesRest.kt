package org.projectforge.rest

import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * For uploading address immages.
 */
@Component
@Path("user")
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
    @GET
    @Path("aco")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAutocCmpletionObjects(@QueryParam("search") searchString: String?): Response {
        val filter = BaseSearchFilter()
        filter.setSearchFields("username", "firstname", "lastname", "email")
        filter.searchString = searchString
        val result = userDao.getList(filter)
        if (searchString.isNullOrBlank())
            result.removeIf { it.isDeactivated } // Remove deactivated users when returning all. Show deactivated users only if search string is given.
        return restHelper.buildResponse(result)
    }
}
