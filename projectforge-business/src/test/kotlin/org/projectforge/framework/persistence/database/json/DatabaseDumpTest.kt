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

package org.projectforge.framework.persistence.database.json

import org.apache.commons.io.output.ByteArrayOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class DatabaseDumpTest : AbstractTestBase() {
    @Autowired
    private lateinit var databaseDump: DatabaseDump

    @Test
    fun dumpTest() {
        logon(ADMIN)
        val out = ByteArrayOutputStream()
        databaseDump.dump( out)
        val result = out.toString("UTF-8")
        log.info("$result")
        assertEquals("WIP", "WIP")
    }
}
