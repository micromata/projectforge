package org.projectforge.rest

import org.projectforge.business.user.UserXmlPreferencesCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This rest service should be available without login (public).
 */
@Component
@Path("logout")
open class LogoutRest {
    private val log = org.slf4j.LoggerFactory.getLogger(LogoutRest::class.java)

    @Autowired
    private lateinit var cookieService: CookieService

    @Autowired
    private lateinit  var userXmlPreferencesCache : UserXmlPreferencesCache

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun logout(@Context request: HttpServletRequest,
              @Context response: HttpServletResponse)
            : Response {
        val stayLoggedInCookie = cookieService.getStayLoggedInCookie(request)
        val user =  UserFilter.getUser(request)
        if (user != null) {
            userXmlPreferencesCache.flushToDB(user.getId())
            userXmlPreferencesCache.clear(user.getId())
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