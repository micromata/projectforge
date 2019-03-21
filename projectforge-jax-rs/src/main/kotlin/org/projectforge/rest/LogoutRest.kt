package org.projectforge.rest

import org.projectforge.business.user.UserXmlPreferencesCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This rest service should be available without login (public).
 */
@Controller
@Path("logout")
open class LogoutRest {
    private val log = org.slf4j.LoggerFactory.getLogger(LogoutRest::class.java)

    @Autowired
    open var cookieService: CookieService? = null

    @Autowired
    open var userXmlPreferencesCache : UserXmlPreferencesCache? = null

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun logout(@Context request: HttpServletRequest,
              @Context response: HttpServletResponse)
            : Response {
        val stayLoggedInCookie = cookieService!!.getStayLoggedInCookie(request)
        val user =  UserFilter.getUser(request)
        if (user != null) {
            userXmlPreferencesCache!!.flushToDB(user.getId())
            userXmlPreferencesCache!!.clear(user.getId())
        }
        UserFilter.logout(request)
        if (stayLoggedInCookie != null) {
            stayLoggedInCookie.maxAge = 0
            stayLoggedInCookie.value = null
            stayLoggedInCookie.path = "/"
        }
        if (stayLoggedInCookie != null) {
            response.addCookie(stayLoggedInCookie)
        }
        return Response.ok("logged-out").build()
    }
}