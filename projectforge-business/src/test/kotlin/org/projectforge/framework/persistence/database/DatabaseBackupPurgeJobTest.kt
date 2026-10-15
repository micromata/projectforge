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


package org.projectforge.framework.persistence.database

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DatabaseBackupPurgeJobTest {
    @Test
    fun `comma-separated prefixes are parsed, blank means all files`() {
        Assertions.assertEquals(listOf(null), DatabaseBackupPurgeJob.parsePrefixes(null))
        Assertions.assertEquals(listOf(null), DatabaseBackupPurgeJob.parsePrefixes(" , "))
        Assertions.assertEquals(listOf("projectforge_"), DatabaseBackupPurgeJob.parsePrefixes("projectforge_"))
        Assertions.assertEquals(
            listOf("projectforge_", "projectforge_files_"),
            DatabaseBackupPurgeJob.parsePrefixes(" projectforge_, projectforge_files_,projectforge_"),
        )
    }
}
