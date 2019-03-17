package org.projectforge.rest.pub

import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.rest.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Controller
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This rest service should be available without login (public).
 */
@Controller
@Path("userStatus")
open class UserStatusRest {
    data class UserData(var username: String? = null,
                        var organization: String? = null,
                        var fullname: String? = null,
                        var locale: Locale? = null,
                        var timeZone: String? = null,
                        var timeZoneDisplayName: String? = null)

    private val log = org.slf4j.LoggerFactory.getLogger(UserStatusRest::class.java)

    @Autowired
    open var applicationContext: ApplicationContext? = null

    @Autowired
    open var userService: UserService? = null

    @Autowired
    open var cookieService: CookieService? = null

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun loginTest(@Context request: HttpServletRequest): Response {
        val user = UserFilter.getUser(request)
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build()
        }
        val userData = UserData(username = user.username,
                organization = user.organization,
                fullname = user.fullname,
                locale = user.locale,
                timeZone = user.timeZone,
                timeZoneDisplayName = user.timeZoneDisplayName)
        return RestHelper.buildResponse(userData)
    }
}