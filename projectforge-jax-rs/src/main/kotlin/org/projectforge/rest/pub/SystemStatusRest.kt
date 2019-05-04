package org.projectforge.rest.pub

import org.projectforge.ProjectForgeVersion
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}")
class SystemStatusRest {
    data class SystemData(var appname: String? = null,
                          var version: String? = null,
                          var releaseTimestamp: String? = null,
                          var releaseDate: String? = null,
                          var messageOfTheDay: String? = null,
                          var logoUrl: String? = null)

    @GetMapping("systemStatus")
    fun loginTest(request: HttpServletRequest): SystemData {
        return getSystemData()
    }

    companion object {
        fun getSystemData(): SystemStatusRest.SystemData {

            val systemStatus = SystemStatusRest.SystemData(appname = ProjectForgeVersion.APP_ID,
                    version = ProjectForgeVersion.VERSION_STRING,
                    releaseTimestamp = ProjectForgeVersion.RELEASE_TIMESTAMP,
                    releaseDate = ProjectForgeVersion.RELEASE_DATE,
                    logoUrl = LogoServiceRest.logoUrl)
            systemStatus.messageOfTheDay = GlobalConfiguration.getInstance()
                    .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY)
            return systemStatus
        }
    }
}
