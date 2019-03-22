package org.projectforge.rest

import com.google.gson.annotations.SerializedName
import org.projectforge.ProjectForgeVersion
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.common.DateFormatType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.TimeNotation
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
                        @SerializedName("last-name")
                        var lastName: String? = null,
                        @SerializedName("first-name")
                        var firstName: String? = null,
                        var locale: Locale? = null,
                        @SerializedName("timezone")
                        var timeZone: String? = null,
                        @SerializedName("date-format")
                        var dateFormat: String? = null,
                        @SerializedName("date-format-short")
                        var dateFormatShort: String? = null,
                        @SerializedName("date-format-minutes")
                        var dateFormatMinutes: String? = null,
                        @SerializedName("date-format-seconds")
                        var dateFormatSeconds: String? = null,
                        @SerializedName("date-format-millis")
                        var dateFormatMillis: String? = null,
                        @SerializedName("first-day-of-week")
                        var firstDayOfWeek : Int? = null,
                        @SerializedName("time-notation")
                        var timeNotation : TimeNotation? = null)

    data class SystemData(var appname: String? = null,
                          var version: String? = null,
                          @SerializedName("release-timestamp")
                          var releaseTimestamp: String? = null,
                          @SerializedName("release-date")
                          var releaseDate: String? = null)

    data class Result(@SerializedName("user-data")
                      val userData: UserData,
                      @SerializedName("system-data")
                      val systemData: SystemData)

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
                firstName = user.firstname,
                lastName = user.lastname,
                locale = user.locale,
                timeZone = ThreadLocalUserContext.getTimeZone().id,
                timeNotation = DateFormats.ensureAndGetDefaultTimeNotation(),
                dateFormat = DateFormats.getFormatString(DateFormatType.DATE),
                dateFormatShort = DateFormats.getFormatString(DateFormatType.DATE_SHORT),
                dateFormatMinutes = DateFormats.getFormatString(DateFormatType.DATE_TIME_MINUTES),
                dateFormatSeconds = DateFormats.getFormatString(DateFormatType.DATE_TIME_SECONDS),
                dateFormatMillis = DateFormats.getFormatString(DateFormatType.DATE_TIME_MILLIS),
                firstDayOfWeek = user.firstDayOfWeek)

        val systemData = SystemData(appname = ProjectForgeVersion.APP_ID,
                version = ProjectForgeVersion.VERSION_STRING,
                releaseTimestamp = ProjectForgeVersion.RELEASE_TIMESTAMP,
                releaseDate = ProjectForgeVersion.RELEASE_DATE)
        return RestHelper.buildResponse(Result(userData, systemData))
    }
}