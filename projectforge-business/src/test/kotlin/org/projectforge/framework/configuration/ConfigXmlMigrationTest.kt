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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.kost.AccountingConfig
import org.projectforge.business.fibu.kost.AccountingConfigValidator
import org.projectforge.business.orga.ContractType
import org.projectforge.business.test.TestConfigurationParams
import org.projectforge.business.test.TestSetup
import org.projectforge.common.i18n.Priority
import org.projectforge.framework.calendar.HolidayDefinition
import org.projectforge.framework.calendar.HolidaysConfig
import org.projectforge.framework.configuration.ApplicationDirectories.Dir
import org.projectforge.framework.json.JsonUtils
import org.projectforge.jira.JiraConfig
import java.io.File
import java.math.BigDecimal

class ConfigXmlMigrationTest {
    @Test
    fun jsonValuesOfAllSections() {
        val values = ConfigXmlMigration.jsonValues(LegacyConfigXml.read(CONFIG_XML))
        Assertions.assertEquals(
            setOf(
                ConfigurationParam.HOLIDAYS,
                ConfigurationParam.JIRA,
                ConfigurationParam.CONTRACT_TYPES,
                ConfigurationParam.FIBU_ACCOUNTING,
            ),
            values.keys,
        )
        values.forEach { (param, json) ->
            // The values pass the validators of the configuration page.
            Assertions.assertEquals(emptyList<String>(), ConfigurationJsonValidators.validate(param.key, json), json)
        }

        HolidaysConfig.parse(values[ConfigurationParam.HOLIDAYS]).let { holidays ->
            Assertions.assertEquals(4, holidays.size)
            Assertions.assertEquals("Erster Mai", holidays[0].label)
            Assertions.assertEquals(5, holidays[0].month)
            Assertions.assertEquals(1, holidays[0].dayOfMonth)
            Assertions.assertEquals(HolidayDefinition.XMAS_EVE, holidays[1].id)
            Assertions.assertTrue(holidays[1].isWorkingDay)
            Assertions.assertEquals(0, BigDecimal("0.5").compareTo(holidays[1].workFraction))
            Assertions.assertEquals(HolidayDefinition.SHROVE_TUESDAY, holidays[2].id)
            Assertions.assertTrue(holidays[2].isIgnore)
            Assertions.assertEquals(HolidayDefinition.NEW_YEARS_EVE, holidays[3].id, "SYLVESTER renamed")
        }
        JiraConfig.parse(values[ConfigurationParam.JIRA]).let { jira ->
            Assertions.assertEquals("https://jira.acme.com/jira/browse/", jira.browseBaseUrl)
            Assertions.assertEquals(2, jira.servers!!.size)
            Assertions.assertEquals("https://customer.acme.com/jira/browse/", jira.servers!![0].baseUrl)
            Assertions.assertEquals("ACME; ADMIN, portal", jira.servers!![0].projectsAsString)
            Assertions.assertArrayEquals(arrayOf("ACME", "ADMIN", "PORTAL"), jira.servers!![0].projects)
            Assertions.assertFalse(values[ConfigurationParam.JIRA]!!.contains("configured"))
        }
        JsonUtils.fromJson(values[ConfigurationParam.CONTRACT_TYPES], Array<ContractType>::class.java)!!.let { types ->
            Assertions.assertEquals(listOf("NDA", "AV"), types.map { it.value })
            Assertions.assertEquals("Non-disclosure agreement", types[0].label)
        }
        AccountingConfig.parse(values[ConfigurationParam.FIBU_ACCOUNTING]).let { accounting ->
            Assertions.assertTrue(accounting.debitorsAccountNumberRanges.doesMatch(11000))
            Assertions.assertFalse(accounting.debitorsAccountNumberRanges.doesMatch(13000))
            Assertions.assertTrue(accounting.creditorsAccountNumberRanges.doesMatch(70000))
            val bwa = accounting.businessAssessmentConfig
            Assertions.assertEquals("BWA", bwa.heading)
            Assertions.assertEquals("gesamtleistung", bwa.overallPerformance)
            Assertions.assertEquals(3, bwa.rows.size)
            bwa.getRow("umsatzErloese").let { row ->
                Assertions.assertEquals("1020", row.no)
                Assertions.assertEquals(Priority.MIDDLE, row.priority)
                Assertions.assertEquals(4000, row.accountNumberRanges.ranges[0].minValue)
                Assertions.assertEquals(1, row.indent)
            }
            Assertions.assertEquals("=umsatzErloese", bwa.getRow("gesamtleistung").valueConfig)
            Assertions.assertTrue(bwa.getRow("erfolgsquote").valueConfig.contains("return betriebsErgebnis"))
        }
    }

    @Test
    fun priorityCaseInsensitive() {
        val accounting = AccountingConfig.parse(
            """{"businessAssessment":{"rows":[{"no":"1","id":"a","priority":"high"}]}}"""
        )
        Assertions.assertEquals(Priority.HIGH, accounting.businessAssessmentConfig.getRow("a").priority)
    }

    @Test
    fun emptyConfigXml() {
        val legacy = LegacyConfigXml.read("<config><holidays></holidays><accountingConfig/></config>")
        Assertions.assertTrue(ConfigXmlMigration.jsonValues(legacy).isEmpty())
        Assertions.assertTrue(ConfigXmlMigration.directoryValues(legacy).isEmpty())
    }

    @Test
    fun directoryValuesDifferingFromDefaults() {
        val legacy = LegacyConfigXml.read(
            "<config><jcrDirectory>jcr</jcrDirectory><workingDirectory>/var/pf/work</workingDirectory>" +
                    "<backupDirectory> </backupDirectory><databaseDirectory>db</databaseDirectory></config>"
        )
        Assertions.assertEquals(mapOf(Dir.WORK to "/var/pf/work"), ConfigXmlMigration.directoryValues(legacy))
    }

    @Test
    fun validatorRejectsUnknownProperties() {
        val errors = ConfigurationJsonValidators.validate(ConfigurationParam.HOLIDAYS.key, """[{"lable":"Typo"}]""")
        Assertions.assertEquals(1, errors.size, errors.toString())
        Assertions.assertEquals(
            emptyList<String>(),
            ConfigurationJsonValidators.validate(ConfigurationParam.HOLIDAYS.key, ConfigurationParam.HOLIDAYS.defaultStringValue),
        )
    }

    @Test
    fun holidaysValidation() {
        fun errors(json: String) = ConfigurationJsonValidators.validate(ConfigurationParam.HOLIDAYS.key, json)
        Assertions.assertEquals(emptyList<String>(), errors(TestConfigurationParams.HOLIDAYS_JSON))
        Assertions.assertEquals(1, errors("""[{"label":"","month":5,"dayOfMonth":1}]""").size, "Label missing.")
        Assertions.assertEquals(1, errors("""[{"label":"A","month":2,"dayOfMonth":30}]""").size, "No 30th of February.")
        Assertions.assertEquals(0, errors("""[{"label":"A","month":2,"dayOfMonth":29}]""").size, "Leap day.")
        Assertions.assertEquals(1, errors("""[{"label":"A","year":2023,"month":2,"dayOfMonth":29}]""").size)
        Assertions.assertEquals(1, errors("""[{"id":"XMAS_EVE"},{"id":"XMAS_EVE","ignore":true}]""").size, "Twice.")
        Assertions.assertEquals(1, errors("""[{"id":"XMAS_EVE","workFraction":1.5}]""").size)
    }

    @Test
    fun accountingValidation() {
        fun errors(json: String) = ConfigurationJsonValidators.validate(ConfigurationParam.FIBU_ACCOUNTING.key, json)
        val valid = ConfigXmlMigration.jsonValues(LegacyConfigXml.read(CONFIG_XML))[ConfigurationParam.FIBU_ACCOUNTING]!!
        Assertions.assertEquals(emptyList<String>(), errors(valid))
        Assertions.assertEquals(1, errors("""{"debitorsAccountNumberRanges":"10000-abc"}""").size)
        val rows = """{"businessAssessment":{"overallPerformance":"%s","rows":[
            |{"no":"1","id":"a","accountRange":"4000-4799"},{"no":"2","id":"%s","value":"%s"}]}}""".trimMargin()
        Assertions.assertEquals(emptyList<String>(), errors(rows.format("a", "b", "=a*2")))
        Assertions.assertEquals(1, errors(rows.format("x", "b", "=a*2")).size, "Unknown row.")
        Assertions.assertEquals(1, errors(rows.format("a", "a", "=a*2")).size, "Duplicate id.")
        Assertions.assertEquals(1, errors(rows.format("a", "1b", "=a*2")).size, "Invalid id.")
        Assertions.assertEquals(1, errors(rows.format("a", "b", "=a*(")).size, "Not compilable.")
    }

    @Test
    fun contractTypesValidation() {
        fun errors(json: String) = ConfigurationJsonValidators.validate(ConfigurationParam.CONTRACT_TYPES.key, json)
        Assertions.assertEquals(emptyList<String>(), errors("""[{"value":"NDA","label":"NDA"},{"value":"AV","label":"AV"}]"""))
        Assertions.assertEquals(1, errors("""[{"value":" ","label":"NDA"}]""").size, "Value missing.")
        Assertions.assertEquals(1, errors("""[{"value":"NDA","label":""}]""").size, "Label missing.")
        Assertions.assertEquals(1, errors("""[{"value":"NDA","label":"a"},{"value":"NDA ","label":"b"}]""").size)
    }

    @Test
    fun jiraValidation() {
        fun errors(json: String) = ConfigurationJsonValidators.validate(ConfigurationParam.JIRA.key, json)
        Assertions.assertEquals(
            emptyList<String>(),
            errors("""{"browseBaseUrl":"https://jira.acme.com/browse/","servers":[{"baseUrl":"https://a.acme.com/browse/","projects":"ACME, adm"}]}"""),
        )
        Assertions.assertEquals(emptyList<String>(), errors("""{"browseBaseUrl":" ","servers":[]}"""))
        Assertions.assertEquals(1, errors("""{"browseBaseUrl":"jira.acme.com"}""").size, "No http url.")
        Assertions.assertEquals(1, errors("""{"servers":[{"baseUrl":"","projects":"ACME"}]}""").size, "Url missing.")
        Assertions.assertEquals(1, errors("""{"servers":[{"baseUrl":"https://a.acme.com/"}]}""").size, "Projects missing.")
        Assertions.assertEquals(1, errors("""{"servers":[{"baseUrl":"https://a.acme.com/","projects":"AC-ME"}]}""").size)
        Assertions.assertEquals(
            1,
            errors("""{"servers":[{"baseUrl":"https://a.acme.com/","projects":"ACME"},{"baseUrl":"https://b.acme.com/","projects":"acme"}]}""").size,
            "Project on two servers.",
        )
    }

    @Test
    fun takeOverAndRename(@TempDir homeDir: File) {
        val configXml = File(homeDir, LegacyConfigXml.CONFIG_XML_FILE)
        configXml.writeText(CONFIG_XML)
        File(homeDir, "projectforge.properties").writeText("projectforge.dir.tmp=/tmp/pf\n")
        try {
            ConfigXmlMigration.init(homeDir.absolutePath)
            Assertions.assertNotNull(ConfigXmlMigration.getJsonValue(ConfigurationParam.HOLIDAYS))
            Assertions.assertNull(ConfigXmlMigration.getJsonValue(ConfigurationParam.TIMESHEET_TAGS))

            val dirs = ConfigXmlMigration.takeOverDirectories(
                homeDir.absolutePath,
                mapOf(Dir.JCR to null, Dir.WORK to null, Dir.TMP to "/tmp/pf", Dir.BACKUP to ""),
            )
            // The working directory of config.xml (property not given), the temp dir of the property:
            Assertions.assertEquals("myWork", dirs[Dir.WORK])
            Assertions.assertEquals("/tmp/pf", dirs[Dir.TMP])
            Assertions.assertTrue(dirs[Dir.JCR].isNullOrBlank())
            val properties = File(homeDir, "projectforge.properties").readText()
            Assertions.assertTrue(properties.contains("projectforge.dir.work=myWork"), properties)
            Assertions.assertTrue(properties.contains("projectforge.dir.tmp=/tmp/pf"), properties)

            ConfigXmlMigration.markMigrated()
            Assertions.assertFalse(configXml.exists())
            Assertions.assertTrue(File(homeDir, "config.xml.migrated").exists())
            Assertions.assertNull(ConfigXmlMigration.getJsonValue(ConfigurationParam.HOLIDAYS))

            // Nothing to do on the next start:
            ConfigXmlMigration.init(homeDir.absolutePath)
            Assertions.assertNull(ConfigXmlMigration.getJsonValue(ConfigurationParam.HOLIDAYS))
        } finally {
            ConfigXmlMigration.init(File(homeDir, "nonExisting").absolutePath)
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestSetup.init()
            ConfigurationJsonValidators.registerType(
                ConfigurationParam.HOLIDAYS, HolidaysConfig.typeReference, HolidaysConfig::validate)
            ConfigurationJsonValidators.registerType(
                ConfigurationParam.JIRA, object : com.fasterxml.jackson.core.type.TypeReference<JiraConfig>() {}, JiraConfig::validate)
            ConfigurationJsonValidators.registerType(
                ConfigurationParam.CONTRACT_TYPES,
                object : com.fasterxml.jackson.core.type.TypeReference<List<ContractType>>() {},
                ConfigurationService::validateContractTypes,
            )
            ConfigurationJsonValidators.registerType(
                ConfigurationParam.FIBU_ACCOUNTING,
                object : com.fasterxml.jackson.core.type.TypeReference<AccountingConfig>() {},
                AccountingConfigValidator::validate,
            )
        }

        private val CONFIG_XML = """<?xml version="1.0" encoding="UTF-8"?>
<config>
  <jiraBrowseBaseUrl>https://jira.acme.com/jira/browse/</jiraBrowseBaseUrl>
  <jiraServers>
    <jiraServer browse-base-url="https://customer.acme.com/jira/browse/" projects="ACME; ADMIN, portal" />
    <jiraServer browse-base-url="https://customer2.acme.com/browse/" projects="SUPPORT" />
  </jiraServers>
  <holidays>
    <holiday label="Erster Mai" month="5" dayOfMonth="1" workingDay="false" />
    <holiday id="XMAS_EVE" workingDay="true" workFraction="0.5" />
    <holiday id="SHROVE_TUESDAY" ignore="true" />
    <holiday id="SYLVESTER" workingDay="true" workFraction="0.5" />
  </holidays>
  <workingDirectory>myWork</workingDirectory>
  <tempDirectory>myTmp</tempDirectory>
  <contractTypes>
    <contractType value="NDA" label="Non-disclosure agreement" />
    <contractType value="AV" label="Arbeitsvertrag" />
  </contractTypes>
  <accountingConfig>
    <debitorsAccountNumberRanges>10000-12999</debitorsAccountNumberRanges>
    <creditorsAccountNumberRanges>70000-79999</creditorsAccountNumberRanges>
    <businessAssessment heading="BWA" overallPerformance="gesamtleistung">
      <rows>
        <row no="1020" id="umsatzErloese" accountRange="4000-4799" priority="middle" title="Umsatzerlöse" indent="1" />
        <row no="1051" id="gesamtleistung" value="=umsatzErloese" priority="high" title="Gesamtleistung" />
        <row no="" id="erfolgsquote" priority="high" title="Erfolgsquote">
          <value>
            if (gesamtleistung != 0.0) {
              return betriebsErgebnis * 100 / gesamtleistung;
            } else {
              return 0.0;
            }
          </value>
        </row>
      </rows>
    </businessAssessment>
  </accountingConfig>
  <portletMode>false</portletMode>
</config>
"""
    }
}
