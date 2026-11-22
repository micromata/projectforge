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

package org.projectforge.framework.configuration.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.business.availability.AvailabilityConfig
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.EInvoiceSellerConfig
import org.projectforge.business.fibu.InvoiceConfiguration
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.io.File

class ProjectForgePropertiesReloaderTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `the parameters of reloadable beans take effect, all others keep their startup value`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        val environment = StandardEnvironment()
        // As Spring Boot names the source of --spring.config.additional-location.
        val name = "Config resource 'file [${file.absolutePath}]' via location 'file:${file.absolutePath}'"
        environment.propertySources.addLast(
            MapPropertySource(name, mapOf("server.port" to "8080", "projectforge.availability.types" to "a;b"))
        )
        val availabilityConfig = AvailabilityConfig()
        val invoiceConfig = InvoiceConfiguration()
        val sellerConfig = EInvoiceSellerConfig()
        val reloader = ProjectForgePropertiesReloader(environment, listOf(availabilityConfig, invoiceConfig, sellerConfig))
        assertTrue(reloader.isLive("projectforge.availability.types"))
        assertTrue(reloader.isLive("projectforge.einvoice.seller.bankAccounts[0].iban"))
        assertFalse(reloader.isLive("server.port"))
        assertFalse(reloader.isLive("projectforge.availability.typesX"), "An exact key, no prefix.")

        file.writeText(
            """
            server.port=9090
            projectforge.availability.types=parentalLeave ; childSick
            projectforge.fibu.invoices.accountRequired=true
            projectforge.einvoice.seller.name=Seller
            projectforge.einvoice.seller.bankAccounts[0].iban=DE00
            """.trimIndent()
        )
        assertTrue(reloader.reload(file))
        assertEquals(listOf("parentalLeave", "childSick"), availabilityConfig.getTypes())
        assertTrue(invoiceConfig.accountRequired)
        assertEquals("Seller", sellerConfig.name)
        assertEquals("DE00", sellerConfig.bankAccounts.single().iban)
        assertEquals("parentalLeave ; childSick", reloader.activeValue("projectforge.availability.types"))
        assertEquals("9090", environment.getProperty("server.port"))
        assertEquals("8080", reloader.activeValue("server.port"), "Needs a restart: the startup value.")

        // A removed parameter gets its default again; the startup value is kept over several reloads.
        file.writeText("server.port=9191\n")
        assertTrue(reloader.reload(file))
        assertFalse(invoiceConfig.accountRequired)
        assertEquals("", sellerConfig.name)
        assertTrue(sellerConfig.bankAccounts.isEmpty())
        assertEquals(
            listOf("parentalLeave", "sickness", "absence", "compensatoryTimeOff", "mobileWork", "workation"),
            availabilityConfig.getTypes(),
        )
        assertEquals("8080", reloader.activeValue("server.port"))
    }

    @Test
    fun `a changed logo and its height take effect, the resolved file isn't kept`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        val environment = StandardEnvironment()
        environment.propertySources.addLast(MapPropertySource("Config resource 'file [${file.absolutePath}]'", emptyMap()))
        val configurationService = ConfigurationService()
        val reloader = ProjectForgePropertiesReloader(environment, listOf(configurationService))
        val first = File(dir, "first.png").also { it.writeText("png") }
        file.writeText("projectforge.logoFile=${first.absolutePath}\nprojectforge.logoHeight=30\n")
        assertTrue(reloader.reload(file))
        assertEquals(first, configurationService.logoFileObject)
        assertEquals(30, configurationService.logoHeight)

        val second = File(dir, "second.jpg").also { it.writeText("jpg") }
        file.writeText("projectforge.logoFile=${second.absolutePath}\n")
        assertTrue(reloader.reload(file))
        assertEquals(second, configurationService.logoFileObject)
        assertEquals("logo.jpg", configurationService.syntheticLogoName)
        assertEquals(null, configurationService.logoHeight, "Removed: the client's default.")
    }

    @Test
    fun `nothing is reloaded without a property source of the file`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        file.writeText("projectforge.availability.types=x\n")
        val availabilityConfig = AvailabilityConfig()
        val reloader = ProjectForgePropertiesReloader(StandardEnvironment(), listOf(availabilityConfig))
        assertFalse(reloader.reload(file))
        assertEquals(null, reloader.activeValue("projectforge.availability.types"))
    }

    @Test
    fun `regional settings take effect without a restart`() {
        val file = File(dir, ProjectForgePropertiesFile.FILENAME)
        val environment = StandardEnvironment()
        val name = "Config resource 'file [${file.absolutePath}]' via location 'file:${file.absolutePath}'"
        environment.propertySources.addLast(
            MapPropertySource(
                name,
                mapOf(
                    "projectforge.currencySymbol" to "$",
                    "projectforge.currency" to "USD",
                    "projectforge.defaultLocale" to "de",
                    "projectforge.defaultTimeNotation" to "H12",
                    "projectforge.defaultFirstDayOfWeek" to "SUNDAY",
                )
            )
        )
        val configurationService = ConfigurationService()
        val reloader = ProjectForgePropertiesReloader(environment, listOf(configurationService))
        listOf(
            "projectforge.currencySymbol", "projectforge.currency", "projectforge.defaultLocale",
            "projectforge.defaultTimeNotation", "projectforge.defaultFirstDayOfWeek",
        ).forEach { assertTrue(reloader.isLive(it), it) }

        file.writeText(
            "projectforge.currencySymbol=CHF\nprojectforge.currency=CHF\nprojectforge.defaultLocale=en\n" +
                "projectforge.defaultTimeNotation=H24\nprojectforge.defaultFirstDayOfWeek=MONDAY\n"
        )
        assertTrue(reloader.reload(file))
        assertEquals("CHF", configurationService.currencySymbol)
        assertEquals("CHF", configurationService.currency)
        assertEquals(java.util.Locale.ENGLISH, configurationService.defaultLocale)
        assertEquals(org.projectforge.framework.time.TimeNotation.H24, configurationService.defaultTimeNotation)
        assertEquals(java.time.DayOfWeek.MONDAY, configurationService.defaultFirstDayOfWeek)
    }
}
