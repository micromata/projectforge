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


package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker
import java.util.Locale

class InvoicingQuotaServiceTest {
    private fun service(
        info: Map<String, String> = emptyMap(),
        foreignUserGroups: List<ProjectForgeGroup> = emptyList(),
        isMember: Boolean = false,
    ): InvoicingQuotaService {
        val config = InvoicingQuotaConfiguration()
        config.info = info
        config.foreignUserGroups = foreignUserGroups
        val accessChecker = mock<AccessChecker> {
            on { isLoggedInUserMemberOfGroup(any<ProjectForgeGroup>()) } doReturn isMember
        }
        return InvoicingQuotaService(config, accessChecker)
    }

    @Test
    fun `own quota is always visible, foreign quota not by default`() {
        val service = service(isMember = true)
        assertTrue(service.mayViewQuotaOf(1L, 1L))
        assertFalse(service.mayViewQuotaOf(2L, 1L))
        assertFalse(service.mayViewQuotaOf(1L, null))
    }

    @Test
    fun `foreign quota is visible only for members of the configured groups`() {
        val groups = listOf(ProjectForgeGroup.CONTROLLING_GROUP)
        assertTrue(service(foreignUserGroups = groups, isMember = true).mayViewQuotaOf(2L, 1L))
        assertFalse(service(foreignUserGroups = groups, isMember = false).mayViewQuotaOf(2L, 1L))
    }

    @Test
    fun `info is taken in the user's language`() {
        val service = service(mapOf("de" to "Deutsch", "en" to "English"))
        assertEquals("Deutsch", service.getInfo(Locale.GERMANY))
        assertEquals("English", service.getInfo(Locale.ENGLISH))
    }

    @Test
    fun `info falls back to English for a language without own entry`() {
        assertEquals("English", service(mapOf("de" to "Deutsch", "en" to "English")).getInfo(Locale.FRENCH))
    }

    @Test
    fun `blank or missing info yields null`() {
        assertNull(service(mapOf("de" to " ")).getInfo(Locale.GERMAN))
        assertNull(service(mapOf("de" to "Deutsch")).getInfo(Locale.FRENCH))
        assertNull(service(emptyMap()).getInfo(Locale.ENGLISH))
    }
}
