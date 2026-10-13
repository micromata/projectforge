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


package org.projectforge.framework.configuration

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConfigurationParamCategoryTest {
    @Test
    fun everyParameterHasACategory() {
        ConfigurationParam.entries.forEach { param ->
            Assertions.assertNotNull(param.category, "$param")
        }
        Assertions.assertEquals(ConfigurationCategory.TIMESHEET, ConfigurationParam.TIMESHEET_VACATION_AUTO_BOOKING_DAYS.category)
    }

    @Test
    fun viewersAndEditorsAsShownInTheList() {
        Assertions.assertEquals("PF_Admin", ConfigurationEditors.ADMIN.viewersText)
        Assertions.assertEquals("PF_Admin, PF_Finance, PF_Controlling", ConfigurationEditors.FINANCE.viewersText)
        Assertions.assertEquals("PF_Finance, PF_Controlling", ConfigurationEditors.FINANCE.editorsText)
        Assertions.assertEquals("PF_Finance + PF_Controlling", ConfigurationEditors.CONTROLLING.editorsText)
        Assertions.assertEquals(
            ConfigurationEditors.FINANCE,
            ConfigurationParam.TIMESHEET_VACATION_KOST2.editors,
        )
    }
}
