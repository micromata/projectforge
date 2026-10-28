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

package org.projectforge.business.scripting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

class ScriptDaoBackupTest {
    @Test
    fun `reads the timestamp of current and old backup file names as UTC`() {
        val expected = Instant.parse("2022-03-05T22:06:16Z").toEpochMilli()
        Assertions.assertEquals(
            expected,
            ScriptDao.parseBackupTimestamp("script-42_DB_neu_2022-03-05_22_06_16.kts"),
        )
        Assertions.assertEquals(
            expected,
            ScriptDao.parseBackupTimestamp("Basisfunktionen_BUs_und_Projekte_2022-03-05_22_06_16.groovy"),
        )
    }

    @Test
    fun `a name without a valid timestamp has none`() {
        Assertions.assertNull(ScriptDao.parseBackupTimestamp("script-42_Report.kts"))
        Assertions.assertNull(ScriptDao.parseBackupTimestamp("script-42_Report_2022-13-05_22_06_16.kts"))
        Assertions.assertNull(ScriptDao.parseBackupTimestamp("script-42_Report_2022-03-05_22_06_16"))
    }
}
