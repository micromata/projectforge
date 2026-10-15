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

package org.projectforge.business.lanesandplanes

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired

class LanesAndPlanesSettingsServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var settingsService: LanesAndPlanesSettingsService

    @Test
    fun addUserEmails() {
        configurationDao.checkAndUpdateDatabaseEntries()
        val entry = configurationDao.getEntry(ConfigurationParam.LANES_AND_PLANES)!!
        entry.stringValue = JsonUtils.toJson(
            LanesAndPlanesSettings(
                accountingInvoiceProfileIds = listOf(1L),
                userEmails = listOf(
                    LanesAndPlanesSettings.UserEmail(1, "edited@example.org"),
                    LanesAndPlanesSettings.UserEmail(),
                ),
            )
        )
        configurationDao.update(entry, checkAccess = false)
        val lastUpdate = configurationDao.getEntry(ConfigurationParam.LANES_AND_PLANES)!!.lastUpdate

        settingsService.addUserEmails(emptyMap())
        Assertions.assertEquals(
            lastUpdate, configurationDao.getEntry(ConfigurationParam.LANES_AND_PLANES)!!.lastUpdate,
            "Nothing new: nothing written.",
        )

        settingsService.addUserEmails(mapOf(3L to "c@example.org", 1L to "sent@example.org", 2L to "b@example.org"))
        val settings = settingsService.settings()
        Assertions.assertEquals(
            mapOf(1L to "edited@example.org", 2L to "b@example.org", 3L to "c@example.org"),
            settings.emailsByUserId,
            "An existing entry (e.g. edited meanwhile) isn't overwritten.",
        )
        Assertions.assertEquals(listOf(1L, 2L, 3L), settings.userEmails.map { it.userId }, "Empty rows are dropped.")
        Assertions.assertEquals(listOf(1L), settings.invoiceProfileIds, "The other settings are kept.")
    }
}
