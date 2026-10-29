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

package org.projectforge.plugins.cveradar.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.CveRadarConfig

/** The responses of Graylog, synthetic examples in the format of its API. */
class ParserTest {
    @Test
    fun `messages of the client agents from Graylog by the configured fields`() {
        val config = CveRadarConfig.Graylog().also {
            it.userField = "user"
            it.hostField = "host"
            it.versionField = "agent_version"
            it.checkIdField = "check_id"
            it.checkResultField = "check_result"
        }
        assertEquals(listOf("timestamp", "user", "host", "agent_version", "check_id", "check_result"), GraylogParser.fields(config))
        val json = """
            {"schema":[
              {"column_type":"field","type":"date","field":"timestamp","name":"field: timestamp"},
              {"column_type":"field","type":"string","field":"user","name":"field: user"},
              {"column_type":"field","type":"string","field":"host","name":"field: host"},
              {"column_type":"field","type":"string","field":"agent_version","name":"field: agent_version"},
              {"column_type":"field","type":"string","field":"check_id","name":"field: check_id"},
              {"column_type":"field","type":"string","field":"check_result","name":"field: check_result"}],
             "datarows":[
              ["2026-10-26T10:00:00.000Z","alice","nb-alice","2.6.34",null,null],
              ["2026-10-26T10:05:00.000Z","alice","nb-alice",null,"INC-2026-1","passed"],
              ["2026-10-26T10:06:00.000Z","bob","nb-bob",null,"INC-2026-1","failed"],
              ["not a date","carol","nb-carol","2.6.30",null,null],
              ["2026-10-26T10:07:00.000Z",null,"nb-x","2.6.30",null,null]],
             "metadata":{}}
        """.trimIndent()
        val records = GraylogParser.parse(json, config)
        assertEquals(3, records.size, "without date or user a message is skipped")
        assertEquals("2.6.34", records[0].version)
        assertNull(records[0].checkPassed)
        assertEquals(true, records[1].checkPassed)
        assertEquals("INC-2026-1", records[1].checkId)
        assertEquals(false, records[2].checkPassed)
    }
}
