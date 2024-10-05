/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.test

import mu.KotlinLogging
import org.mockito.Mockito
import org.projectforge.Constants
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.framework.configuration.ConfigXmlTest
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.i18n.I18nHelper.addBundleName
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext.Companion.createTestInstance
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.DayOfWeek
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object TestSetup {
    /**
     * Puts a context user in ThreadLocaleUserContext and creates a minimal ConfigXml configuration.
     * This is use-full for tests needing the user's locale, timezone etc, but not the time consuming
     * database and Spring setup.
     * @return created user in ThreadLocalUserContext.
     */
    @JvmStatic
    fun init(): PFUserDO {
        addBundleName(Constants.RESOURCE_BUNDLE_NAME)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val user = PFUserDO()
        user.timeZone = TimeZone.getTimeZone("Europe/Berlin")
        user.excelDateFormat = "YYYY-MM-DD"
        user.dateFormat = "dd.MM.yyyy"
        user.locale = Locale("de", "DE")
        user.firstDayOfWeek = DayOfWeek.MONDAY
        ThreadLocalUserContext.userContext = createTestInstance(user)
        ConfigXmlTest.createTestConfiguration()
        ConfigurationServiceAccessor.internalInitJunitTestMode()
        /*val configurationService = Mockito.mock(ConfigurationService::class.java)
        val configList = mutableListOf<ConfigurationDO>()
        Mockito.`when`(configurationService.daoInternalLoadAll()).thenReturn(configList)
        Configuration.initializeForTestOnly(configurationService)*/
        return user
    }

    @JvmStatic
    fun mockConfigurationService() {
        val configurationService = Mockito.mock(ConfigurationService::class.java)
        val configList = mutableListOf<ConfigurationDO>()
        Mockito.`when`(configurationService.daoInternalLoadAll()).thenReturn(configList)
        Configuration.initializeForTestOnly(configurationService)
    }
}
