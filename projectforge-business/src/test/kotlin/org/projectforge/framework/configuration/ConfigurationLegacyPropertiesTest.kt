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
import org.projectforge.framework.configuration.ConfigurationLegacyProperties.Companion.legacyPropertyWarning
import org.projectforge.framework.configuration.entities.ConfigurationDO

class ConfigurationLegacyPropertiesTest {
    @Test
    fun warnsAsLongAsTheLegacyPropertyIsSet() {
        val taskId = ConfigurationParam.TIMESHEET_VACATION_TASK_ID
        val entry = ConfigurationDO().also {
            it.configurationType = ConfigurationType.LONG
            it.longValue = 4711L
        }
        Assertions.assertNull(legacyPropertyWarning(taskId, null, entry))
        Assertions.assertNull(legacyPropertyWarning(taskId, " ", entry))
        // No legacy property at all:
        Assertions.assertNull(legacyPropertyWarning(ConfigurationParam.TIMESHEET_TAGS, "a,b", null))

        legacyPropertyWarning(taskId, " 4711 ", entry)!!.let {
            Assertions.assertTrue(it.contains("projectforge.timesheet.vacation.taskId='4711' is ignored"), it)
            Assertions.assertTrue(it.contains("was taken over"), it)
        }
        legacyPropertyWarning(taskId, "42", entry)!!.let {
            Assertions.assertTrue(it.contains("(currently '4711')"), it)
        }
        entry.longValue = null // Cleared on the configuration page.
        legacyPropertyWarning(taskId, "4711", entry)!!.let {
            Assertions.assertTrue(it.contains("(currently '')"), it)
        }
        val kost2 = ConfigurationDO().also {
            it.configurationType = ConfigurationType.STRING
            it.stringValue = "5.000.00.03"
        }
        legacyPropertyWarning(ConfigurationParam.TIMESHEET_VACATION_KOST2, "5.000.00.03", kost2)!!.let {
            Assertions.assertTrue(it.contains("was taken over"), it)
        }
    }
}
