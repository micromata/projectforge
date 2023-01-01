/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.caldav.config

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.caldav.controller.ProjectForgeCalDAVController
import org.projectforge.caldav.controller.ProjectForgeCardDAVController
import org.projectforge.rest.config.RestUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import javax.servlet.FilterRegistration
import javax.servlet.ServletContext

/**
 * -Dloader.path=${HOME}/ProjectForge/resources/milton
 */
@Component
open class PFMiltonInit {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    /**
     * You may call this method on start-up. It will do nothing, if no milton license files are available.
     */
    fun init(sc: ServletContext) {
        val miltonDir = File(configurationService.resourceDirName, "milton")
        val licenseFile = File(miltonDir, "milton.license.properties")
        if (!licenseFile.exists()) {
            log.info("Don't start webdav (CalDAV and CardDAV) server, OK. No license files given: ${licenseFile.absolutePath}")
            return
        }
        log.info("Milton license file found, try to start WebDAV functionality: ${licenseFile.absolutePath}")
        val miltonFilter: FilterRegistration = RestUtils.registerFilter(sc, "MiltonFilter", PFMiltonFilter::class.java, false, "/*")
        miltonFilter.setInitParameter("resource.factory.class", "io.milton.http.annotated.AnnotationResourceFactory")
        // FB: Don't work in Spring Boot fat jar
        // miltonFilter.setInitParameter("controllerPackagesToScan", "org.projectforge.caldav.controller");
        miltonFilter.setInitParameter("controllerClassNames",
                listOf(ProjectForgeCalDAVController::class.java, ProjectForgeCardDAVController::class.java).joinToString { it.name })
        miltonFilter.setInitParameter("enableDigestAuth", "false")
        miltonFilter.setInitParameter("milton.configurator", ProjectForgeMiltonConfigurator::class.java.name)
        configurationService.isDAVServicesAvailable = true
        available = true
    }

    companion object {
        private val log = LoggerFactory.getLogger(PFMiltonInit::class.java)

        internal var available = false
    }
}
