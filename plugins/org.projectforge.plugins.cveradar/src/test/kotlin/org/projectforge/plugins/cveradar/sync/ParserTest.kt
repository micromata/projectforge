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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.CveRadarConfig

/** The responses of Tenable.sc and Graylog, synthetic examples in the formats of their APIs. */
class ParserTest {
    @Test
    fun `assets of Tenable_sc, usable and manageable, without duplicates`() {
        val json = """
            {"type":"regular","response":{
              "usable":[{"id":"11","name":"SN100_alice"},{"id":"12","name":"SN200_bob"}],
              "manageable":[{"id":"12","name":"SN200_bob"},{"id":"13","name":"Servers"}]
            },"error_code":0,"error_msg":""}
        """.trimIndent()
        val assets = TenableParser.parseAssets(json)
        assertEquals(listOf("11", "12", "13"), assets.map { it.id })
        assertEquals("SN100_alice", assets[0].name)
    }

    @Test
    fun `findings of Tenable_sc, numbers as strings`() {
        val json = """
            {"response":{"totalRecords":"2","returnedRecords":2,"results":[
              {"pluginID":"100001","pluginName":"Example Browser < 1.2 Multiple Vulnerabilities",
               "severity":{"id":"3","name":"High"},"cve":"CVE-2026-0001,CVE-2026-0002",
               "firstSeen":"1790000000","lastSeen":"1790100000","dnsName":"nb-alice.example.org"},
              {"pluginID":"100002","pluginName":"Untrusted root certificate","severity":{"id":"2"},"cve":"",
               "firstSeen":"0","lastSeen":"1790100000","netbiosName":"EXAMPLE\\NB-BOB"}
            ]},"error_code":0}
        """.trimIndent()
        val page = TenableParser.parseVulns(json)
        assertEquals(2, page.totalRecords)
        assertEquals(2, page.returned)
        assertNull(TenableParser.parseVulns("""{"response":{"results":[]}}""").totalRecords, "unknown, not guessed")
        val first = page.vulns[0]
        assertEquals("100001", first.pluginId)
        assertEquals(3, first.severity)
        assertEquals(listOf("CVE-2026-0001", "CVE-2026-0002"), first.cveIds)
        assertEquals(1790000000L * 1000, first.firstSeen!!.time)
        assertEquals("nb-alice.example.org", first.hostname)
        val second = page.vulns[1]
        assertTrue(second.cveIds.isEmpty())
        assertNull(second.firstSeen, "0 is no date")
        assertEquals("NB-BOB", second.hostname)
    }

    @Test
    fun `an error of Tenable_sc is thrown`() {
        assertThrows(IllegalStateException::class.java) {
            TenableParser.parseVulns("""{"response":null,"error_code":74,"error_msg":"Invalid login"}""")
        }
    }

    @Test
    fun `the query of Tenable_sc filters by asset and severity`() {
        val query = TenableParser.vulnQuery("11", "2,3,4", 0, 1000)
        assertTrue(query.contains(""""filterName":"asset","operator":"=","value":{"id":"11"}"""), query)
        assertTrue(query.contains(""""tool":"vulndetails""""), query)
    }

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
