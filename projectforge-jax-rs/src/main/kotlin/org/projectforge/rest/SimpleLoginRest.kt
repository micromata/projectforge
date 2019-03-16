package org.projectforge.rest

import org.projectforge.Const
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.login.LoginResult
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.multitenancy.TenantRegistry
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Controller
import java.net.InetAddress
import java.net.UnknownHostException
import javax.servlet.ServletRequest
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Controller
@Path("login")
open class SimpleLoginRest {
    data class LoginData(var username: String? = null, var password: String? = null, var stayLoggedIn: Boolean? = null)

    private val log = org.slf4j.LoggerFactory.getLogger(SimpleLoginRest::class.java)

    @Autowired
    open var applicationContext: ApplicationContext? = null

    @Autowired
    open var userService: UserService? = null

    @Autowired
    open var cookieService: CookieService? = null

    @GET
    @Path("test")
    fun loginTest(@Context request: HttpServletRequest,
                  @Context response: HttpServletResponse,
                  @QueryParam("username") username: String?,
                  @QueryParam("password") password: String?)
            : Response {
        if (getClientIp(request) != "127.0.0.1") {
            log.warn("****** This simple login service (as GET) is only available for localhost authentication for development purposes due to security reasons.")
            return Response.status(Response.Status.FORBIDDEN).build()
        }
        val loginResultStatus = _login(request, response, LoginData(username, password))
        if (loginResultStatus == LoginResultStatus.SUCCESS)
            return Response.ok().build()
        return Response.status(Response.Status.UNAUTHORIZED).build()
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun login(@Context request: HttpServletRequest,
              @Context response: HttpServletResponse,
              loginData: LoginData)
            : Response {
        val loginResultStatus = _login(request, response, loginData)
        if (loginResultStatus == LoginResultStatus.SUCCESS)
            return Response.ok().build()
        return Response.status(Response.Status.UNAUTHORIZED).build()
    }

    private fun _login(request: HttpServletRequest, response: HttpServletResponse, loginData: LoginData): LoginResultStatus {
        if (getClientIp(request) != "127.0.0.1") {
            log.warn("****** This simple login service is only available for localhost authentication for development purposes due to security reasons. It's under construction.")
            return LoginResultStatus.FAILED
        }
        log.warn("******* Please use SimpleLogin only for development purposes. It doesn't yet support the full login functionality (such as LDAP and support of Wicket).")
        val loginResult = checkLogin(request, loginData)
        val user = loginResult.user
        if (user == null || loginResult.loginResultStatus != LoginResultStatus.SUCCESS) {
            return loginResult.loginResultStatus
        }
        if (UserFilter.isUpdateRequiredFirst() == true) {
            log.warn("******* Update of ProjectForge required first. Please login via old login page. LoginService should be used instead.")
            return LoginResultStatus.FAILED
        }
        log.info("User successfully logged in: " + user.displayUsername)
        if (loginData.stayLoggedIn == true) {
            val loggedInUser = userService!!.getById(user.id)
            val cookie = Cookie(Const.COOKIE_NAME_FOR_STAY_LOGGED_IN, "${loggedInUser.getId()}:${loggedInUser.getUsername()}:${userService!!.getStayLoggedInKey(user.id)}")
            cookieService!!.addStayLoggedInCookie(request, response, cookie)
        }
        // Execute login:
        val userContext = UserContext(PFUserDO.createCopyWithoutSecretFields(user), getUserGroupCache())
        // Wicket part: (page.getSession() as MySession).login(userContext, page.getRequest())
        UserFilter.login(request, userContext)
        return LoginResultStatus.SUCCESS
    }

    private fun checkLogin(request: HttpServletRequest, loginData: SimpleLoginRest.LoginData): LoginResult {
        if (loginData.username == null || loginData.password == null) {
            return LoginResult().setLoginResultStatus(LoginResultStatus.FAILED)
        }
        val loginProtection = LoginProtection.instance()
        val clientIpAddress = getClientIp(request)
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(loginData.username, clientIpAddress)
        if (offset > 0) {
            val seconds = (offset / 1000).toString()
            log.warn("The account for '${loginData.username}' is locked for ${seconds} seconds due to failed login attempts. Please try again later.")

            val numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(loginData.username, clientIpAddress)
            return LoginResult().setLoginResultStatus(LoginResultStatus.LOGIN_TIME_OFFSET).setMsgParams(seconds,
                    numberOfFailedAttempts.toString())
        }
        val loginHandler = applicationContext!!.getBean(LoginDefaultHandler::class.java)

        val result = loginHandler.checkLogin(loginData.username, loginData.password)
        if (result.getLoginResultStatus() == LoginResultStatus.SUCCESS) {
            loginProtection.clearLoginTimeOffset(loginData.username, clientIpAddress)
        } else if (result.getLoginResultStatus() == LoginResultStatus.FAILED) {
            loginProtection.incrementFailedLoginTimeOffset(loginData.username, clientIpAddress)
        }
        return result
    }

    private fun getClientIp(request: ServletRequest): String? {
        var remoteAddr: String? = null
        if (request is HttpServletRequest) {
            remoteAddr = request.getHeader("X-Forwarded-For")
        }
        if (remoteAddr != null) {
            if (remoteAddr.contains(",")) {
                // sometimes the header is of form client ip,proxy 1 ip,proxy 2 ip,...,proxy n ip,
                // we just want the client
                remoteAddr = remoteAddr.split(',')[0].trim({ it <= ' ' })
            }
            try {
                // If ip4/6 address string handed over, simply does pattern validation.
                InetAddress.getByName(remoteAddr)
            } catch (e: UnknownHostException) {
                remoteAddr = request.remoteAddr
            }

        } else {
            remoteAddr = request.remoteAddr
        }
        return remoteAddr
    }

    private fun getTenantRegistry(): TenantRegistry {
        return TenantRegistryMap.getInstance().tenantRegistry
    }

    private fun getUserGroupCache(): UserGroupCache {
        return getTenantRegistry().userGroupCache
    }
}