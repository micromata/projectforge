/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.SystemStatus
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*


/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping(Rest.PUBLIC_URL)
class SystemStatusRest {
    data class SystemData(var appname: String,
                          var version: String,
                          var buildTimestamp: String,
                          var buildDate: String,
                          var releaseYear: String,
                          val scmId: String,
                          val scmIdFull: String,
                          var messageOfTheDay: String? = null,
                          var copyRightYears: String,
                          var logoUrl: String? = null,
                          /**
                           * If given, the client should redirect to this url.
                           */
                          var setupRedirectUrl: String? = null,
                          var startTimeUTC: Date? = null)

    private var _systemData: SystemData? = null

    val systemData: SystemData
        get() =
            if (_systemData == null) {
                // Must be initialized on demand, LogServiceRest is not available on @PostConstruct in test cases.
                _systemData = SystemData(appname = systemStatus.appname,
                        version = systemStatus.version,
                        buildTimestamp = systemStatus.buildTimestamp,
                        buildDate = systemStatus.buildDate,
                        releaseYear = systemStatus.releaseYear,
                        scmId = systemStatus.scmId,
                        scmIdFull = systemStatus.scmIdFull,
                        messageOfTheDay = systemStatus.messageOfTheDay,
                        copyRightYears = systemStatus.copyRightYears,
                        logoUrl = LogoServiceRest.logoUrl,
                        setupRedirectUrl = if (systemStatus.setupRequiredFirst == true) "/wa/setup" else null,
                        startTimeUTC = Date(systemStatus.startTimeMillis))
                _systemData!!
            } else {
                _systemData!!
            }

    @Autowired
    private lateinit var systemStatus: SystemStatus

    @GetMapping("systemStatus")
    fun getSystemStatus(): SystemData {
        if (systemData.setupRedirectUrl != null
                && systemStatus.setupRequiredFirst != true
                && systemStatus.updateRequiredFirst != true) {
            // Setup was already done:
            systemData.setupRedirectUrl = null
        }
        return systemData
    }
}
