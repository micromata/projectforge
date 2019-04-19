package org.projectforge.rest.pub

import org.projectforge.ProjectForgeVersion
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.rest.core.RestHelper
import org.springframework.stereotype.Component
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
@Component
@Path("systemStatus")
open class SystemStatusRest {
    data class SystemData(var appname: String? = null,
                          var version: String? = null,
                          var releaseTimestamp: String? = null,
                          var releaseDate: String? = null,
                          var messageOfTheDay: String? = null)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun loginTest(@Context request: HttpServletRequest): Response {
        return RestHelper().buildResponse(getSystemData())
    }

    companion object {
        fun getSystemData(): SystemData {
            val systemStatus = SystemData(appname = ProjectForgeVersion.APP_ID,
                    version = ProjectForgeVersion.VERSION_STRING,
                    releaseTimestamp = ProjectForgeVersion.RELEASE_TIMESTAMP,
                    releaseDate = ProjectForgeVersion.RELEASE_DATE)
            systemStatus.messageOfTheDay = GlobalConfiguration.getInstance()
                    .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY)
            return systemStatus
        }
    }
}