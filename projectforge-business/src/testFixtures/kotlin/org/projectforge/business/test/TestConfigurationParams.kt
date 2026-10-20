/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.business.test

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.framework.configuration.ConfigurationJsonValues
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.jira.JiraUtilsTest

/**
 * The configuration parameters of the test cases (former test config.xml): the jira servers of [JiraUtilsTest] and some
 * holidays (1st of May and 3rd of October added, Shrove Tuesday ignored, Christmas Eve and New Year's Eve half
 * working days).
 */
object TestConfigurationParams {
    const val HOLIDAYS_JSON = """[
  {"label":"Erster Mai","month":5,"dayOfMonth":1},
  {"label":"Dritter Oktober","month":10,"dayOfMonth":3},
  {"id":"XMAS_EVE","workingDay":true,"workFraction":0.5},
  {"id":"SHROVE_TUESDAY","ignore":true},
  {"id":"NEW_YEARS_EVE","workingDay":true,"workFraction":0.5}
]"""

    val JIRA_JSON = """{
  "browseBaseUrl":"${JiraUtilsTest.JIRA_BASE_URL}",
  "servers":[
    {"baseUrl":"${JiraUtilsTest.JIRA_ACME_BASE_URL}","projects":"ACME; ADMIN, portal"},
    {"baseUrl":"https://customer2.acme.com/browse/","projects":"SUPPORT"}
  ]
}"""

    @JvmStatic
    fun init() {
        ConfigurationServiceAccessor.internalInitJunitTestMode()
        ConfigurationJsonValues.internalSetForTest(ConfigurationParam.HOLIDAYS, HOLIDAYS_JSON)
        ConfigurationJsonValues.internalSetForTest(ConfigurationParam.JIRA, JIRA_JSON)
    }
}
