/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.framework.persistence.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class SystemStatus {
    private val log = org.slf4j.LoggerFactory.getLogger(SystemStatus::class.java)

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var databaseService: DatabaseService

    val appname = ProjectForgeVersion.APP_ID
    val version = ProjectForgeVersion.VERSION_STRING
    val buildTimestamp = ProjectForgeVersion.BUILD_TIMESTAMP
    val buildDate = ProjectForgeVersion.BUILD_DATE
    val releaseYear = ProjectForgeVersion.YEAR
    val scmIdFull = ProjectForgeVersion.SCM_ID_FULL
    val scmId = ProjectForgeVersion.SCM_ID
    var messageOfTheDay: String? = null
        private set
    var logoFile: String? = null
        private set
    val copyRightYears = ProjectForgeVersion.COPYRIGHT_YEARS
    @Value("\${projectforge.development.mode}")
    var developmentMode: Boolean = false
        private set
    var setupRequiredFirst: Boolean? = null
    var updateRequiredFirst: Boolean? = null
    /**
     * This flag is set to true if ProjectForge's start is completed.
     */
    var upAndRunning: Boolean = false
    val startTimeMillis: Long = System.currentTimeMillis()

    @PostConstruct
    private fun postConstruct() {
        messageOfTheDay = GlobalConfiguration.getInstance()
                .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY)
        logoFile = configurationService.syntheticLogoName
        if (!databaseService.databaseTablesWithEntriesExists())
            setupRequiredFirst = true
        devMode = developmentMode
    }

    companion object {
        private var devMode: Boolean? = null

        @JvmStatic
        fun isDevelopmentMode(): Boolean {
            return devMode == true
        }
    }
}

