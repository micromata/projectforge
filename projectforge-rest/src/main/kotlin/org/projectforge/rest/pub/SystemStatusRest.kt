/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.pub

import org.projectforge.ProjectForgeVersion
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


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
                          var releaseYear: String? = null,
                          var messageOfTheDay: String? = null,
                          var logoUrl: String? = null,
                          var copyRightYears: String? = "")

    @GetMapping("systemStatus")
    fun loginTest(): SystemData {
        return getSystemData()
    }

    companion object {
        fun getSystemData(): SystemData {
            val systemStatus = SystemData(appname = ProjectForgeVersion.APP_ID,
                    version = ProjectForgeVersion.VERSION_STRING,
                    releaseTimestamp = ProjectForgeVersion.RELEASE_TIMESTAMP,
                    releaseDate = ProjectForgeVersion.RELEASE_DATE,
                    releaseYear = ProjectForgeVersion.YEAR,
                    logoUrl = LogoServiceRest.logoUrl,
                    copyRightYears = ProjectForgeVersion.COPYRIGHT_YEARS)
            systemStatus.messageOfTheDay = GlobalConfiguration.getInstance()
                    .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY)
            return systemStatus
        }
    }
}
