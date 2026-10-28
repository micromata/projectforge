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
import org.projectforge.framework.configuration.ConfigurationRenamedProperties.Companion.warnings
import org.springframework.mock.env.MockEnvironment

class ConfigurationRenamedPropertiesTest {
    @Test
    fun `nothing set, no warnings`() {
        Assertions.assertTrue(warnings { null }.isEmpty())
        Assertions.assertTrue(warnings { " " }.isEmpty())
    }

    @Test
    fun `former key set, still read`() {
        val result = warnings(properties("projectforge.jcr.maxDefaultFileSize" to "50MB"))
        Assertions.assertEquals(1, result.size)
        Assertions.assertSame(ConfigurationLogEvents.RENAMED_PROPERTY_SET, result[0].first)
        Assertions.assertEquals(
            "Property projectforge.jcr.maxDefaultFileSize='50MB' was renamed to projectforge.files.maxDefaultFileSize " +
                    "(still read). Please rename it in projectforge.properties.",
            result[0].second,
        )
    }

    @Test
    fun `both keys set, former one ignored`() {
        val result = warnings(
            properties(
                "projectforge.jcr.maxDefaultFileSize" to "50MB",
                "projectforge.files.maxDefaultFileSize" to "200MB",
            )
        )
        Assertions.assertEquals(1, result.size)
        Assertions.assertTrue(
            result[0].second.contains("(ignored, because projectforge.files.maxDefaultFileSize='200MB' is given)"),
            result[0].second,
        )
    }

    @Test
    fun `obsolete keys set`() {
        val result = warnings(properties("projectforge.files.store" to "db", "oak.datasource.url" to "jdbc:postgresql:oak"))
        Assertions.assertEquals(2, result.size)
        result.forEach { Assertions.assertSame(ConfigurationLogEvents.OBSOLETE_PROPERTY_SET, it.first) }
        Assertions.assertEquals(
            "Property projectforge.files.store has no effect any more. Please remove it from projectforge.properties.",
            result[0].second,
        )
        // Still read by the start-up guard, so not "no effect":
        Assertions.assertTrue(result[1].second.contains("LegacyJcrGuard"), result[1].second)
        Assertions.assertFalse(result[1].second.contains("no effect"), result[1].second)
    }

    @Test
    fun `new keys default to the former ones`() {
        // As in application.properties:
        val env = MockEnvironment()
            .withProperty("projectforge.files.maxDefaultFileSize", "\${projectforge.jcr.maxDefaultFileSize:100MB}")
            .withProperty("projectforge.files.cron.sanityCheck", "\${projectforge.jcr.cron.sanityCheck:-}")
        Assertions.assertEquals("100MB", env.getProperty("projectforge.files.maxDefaultFileSize"))
        Assertions.assertEquals("-", env.getProperty("projectforge.files.cron.sanityCheck"))
        env.withProperty("projectforge.jcr.maxDefaultFileSize", "50MB")
            .withProperty("projectforge.jcr.cron.sanityCheck", "0 0 3 * * *")
        Assertions.assertEquals("50MB", env.getProperty("projectforge.files.maxDefaultFileSize"))
        Assertions.assertEquals("0 0 3 * * *", env.getProperty("projectforge.files.cron.sanityCheck"))
        // The resolved new key equals the former one, so it is still read:
        Assertions.assertTrue(warnings { env.getProperty(it) }.all { it.second.contains("(still read)") })
    }

    private fun properties(vararg values: Pair<String, String>): (String) -> String? {
        val map = values.toMap()
        return { map[it] }
    }
}
