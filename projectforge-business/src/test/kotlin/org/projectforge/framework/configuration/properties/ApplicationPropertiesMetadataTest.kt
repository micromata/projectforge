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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the format of the bundled application.properties, which is the source of the properties editor (see its
 * header): groups, unique keys, English (ASCII) comments.
 */
class ApplicationPropertiesMetadataTest {
    private val content = readResource("/application.properties")

    @Test
    fun `every key is in a group and occurs only once`() {
        val lines = PropertiesFileParser.parse(content)
        val firstGroup = lines.indexOfFirst { it is PropertiesFileLine.Comment && it.groupTitle != null }
        assertTrue(lines.take(firstGroup).none { it is PropertiesFileLine.Entry }, "Keys in front of the first group.")
        val keys = lines.filterIsInstance<PropertiesFileLine.Entry>().map { it.key }
        val duplicates = keys.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "Keys given more than once (move alternatives to the description): $duplicates")
        val metadata = PropertiesMetadata.instance
        assertEquals(keys.size, metadata.groups.sumOf { it.params.size })
        assertEquals(metadata.groups.size, metadata.groups.map { it.id }.toSet().size, "Group ids must be unique.")
    }

    @Test
    fun `comments are plain ASCII`() {
        content.lines().forEachIndexed { index, line ->
            assertFalse(line.any { it.code > 0x7e }, "Line ${index + 1}: use unicode escapes, comments in English: $line")
        }
    }

    @Test
    fun `keys of the initial projectforge properties are known`() {
        val initial = readResource("/initialBaseDirFiles/initialProjectForge.properties")
        val metadata = PropertiesMetadata.instance
        val unknown = PropertiesFileParser.parse(initial).filterIsInstance<PropertiesFileLine.Entry>()
            .map { it.key }.filter { !metadata.contains(it) }.distinct()
        assertTrue(unknown.isEmpty(), "Keys of initialProjectForge.properties missing in application.properties: $unknown")
    }

    @Test
    fun `blocks, descriptions, optional parameters and secrets`() {
        val metadata = PropertiesMetadata.instance
        val visibility = metadata.groups.single { it.id == "menu-visibility" }.blocks.first()
        assertTrue(visibility.description!!.startsWith("Accessibility / visibility of menu entries"))
        assertEquals("The functionality is available through the MyAccount-page:",
            visibility.params.single { it.key == "projectforge.menu.visibility.changePassword" }.description)
        assertNull(visibility.params.first().description)

        val domain = metadata.getParam("projectforge.domain")!!
        assertTrue(domain.description!!.startsWith("The external url"))
        assertEquals("http://localhost:8080", domain.defaultValue)

        val plugins = metadata.getParam("projectforge.plugins.active")!!
        assertTrue(plugins.optional)
        assertEquals("all", plugins.example)
        assertNotNull(plugins.description)

        assertEquals(PropertiesMetadata.Type.BOOLEAN, metadata.getParam("projectforge.ldap.storePasswords")!!.type)
        listOf(
            "spring.datasource.password", "projectforge.security.passwordPepper", "projectforge.keycloak.clientSecret",
            "projectforge.security.authenticationTokenEncryptionKey", "projectforge.lanesandplanes.apiKey",
            "mail.session.pfmailsession.smtp.password", "projectforge.phoneLookupKey", "projectforge.keystorePassphrase",
        ).forEach { assertTrue(metadata.getParam(it)!!.secret, it) }
        listOf("projectforge.ldap.storePasswords", "projectforge.idp.syncPasswords", "projectforge.domain")
            .forEach { assertFalse(metadata.getParam(it)!!.secret, it) }
        // Also keys unknown to application.properties, e.g. own ones of the admin.
        listOf("projectforge.receiveSmsKey", "projectforge.sms.publicRestCallAuthKey", "plugins.licensemanagement.key", "my.own-key")
            .forEach { assertTrue(PropertiesMetadata.isSecret(it), it) }
        listOf("projectforge.keystoreFile", "projectforge.sipgate.tokenId", "projectforge.ldap.storePasswords")
            .forEach { assertFalse(PropertiesMetadata.isSecret(it), it) }
        assertTrue(metadata.groups.single { it.title == "Internal" }.expert)
        assertEquals("projectforge.einvoice.seller.bankAccounts[1].iban",
            metadata.findRelatedKey("projectforge.einvoice.seller.bankAccounts[5].iban"))
        assertEquals("e-invoice", metadata.getGroup(metadata.findRelatedKey("projectforge.einvoice.seller.foo")!!)!!.id)
        assertNull(metadata.findRelatedKey("my.own.key"))
    }

    @Test
    fun `status of undocumented keys`() {
        val metadata = PropertiesMetadata.instance
        assertEquals(PropertiesMetadata.KeyStatus.KNOWN, metadata.statusOf("projectforge.domain"))
        listOf(
            "projectforge.einvoice.seller.bankAccounts[2].iban", "projectforge.idp.userAttributes.gender",
            "projectforge.idp.groupAttributes.organization", "logging.level.org.projectforge.idp",
            "projectforge.invoicing-quota.info.fr", "projectforge.integration.clients.sms.responseTimeoutMs",
        ).forEach { assertEquals(PropertiesMetadata.KeyStatus.VARIANT, metadata.statusOf(it), it) }
        listOf("pf.config.compileCss", "projectforge.menu.visibility.myPreferences", "projectforge.receiveSmsKey")
            .forEach { assertEquals(PropertiesMetadata.KeyStatus.OBSOLETE, metadata.statusOf(it), it) }
        assertEquals(PropertiesMetadata.KeyStatus.UNKNOWN, metadata.statusOf("springdoc.api-docs.enabled"))
        assertEquals("logging.level.org.hibernate", metadata.findRelatedKey("logging.level.org.projectforge.idp"))
        // The marker isn't part of the description.
        assertFalse(metadata.groups.flatMap { it.blocks }.any { block ->
            (listOfNotNull(block.description) + block.params.mapNotNull { it.description }).any { it.contains("@map") }
        })
    }

    private fun readResource(path: String): String =
        String(javaClass.getResourceAsStream(path)!!.readBytes(), Charsets.ISO_8859_1)
}
