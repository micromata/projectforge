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

package org.projectforge.business.fibu.kost

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.springframework.beans.factory.annotation.Autowired

/**
 * Creating, activating and deactivating a project's cost 2 units by type ([ProjektKost2Service]), as the project
 * edit form and mass update do.
 */
class ProjektKost2ServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var projektKost2Service: ProjektKost2Service

    @Test
    fun `activate creates and reactivates, deactivate only hits active units`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val id = insertProjekt(internKost2_4 = 977, nummer = 11)
            Assertions.assertTrue(projektKost2Service.activate(id, listOf(1L)))
            Assertions.assertEquals(setOf(1L), activeArtIds(id))
            Assertions.assertEquals(setOf(id), kostCache.getProjektIdsWithActiveKost2Arts(listOf(1L, 2L)).filter { it == id }.toSet())
            // Already active: nothing to do.
            Assertions.assertFalse(projektKost2Service.activate(id, listOf(1L)))

            // No unit of type 2: deactivating it is a no-op.
            Assertions.assertFalse(projektKost2Service.deactivate(id, listOf(2L)))
            // Missing type 2: not among the projects having all of 1 and 2 (the gap filter's complement).
            Assertions.assertTrue(id in kostCache.getProjektIdsWithAllActiveKost2Arts(listOf(1L)))
            Assertions.assertFalse(id in kostCache.getProjektIdsWithAllActiveKost2Arts(listOf(1L, 2L)))
            Assertions.assertTrue(projektKost2Service.deactivate(id, listOf(1L, 2L)))
            // A deactivated unit counts as missing.
            Assertions.assertFalse(id in kostCache.getProjektIdsWithAllActiveKost2Arts(listOf(1L)))
            Assertions.assertEquals(emptySet<Long>(), activeArtIds(id))
            // Non-active already, so no longer found by the active type filter.
            Assertions.assertFalse(projektKost2Service.deactivate(id, listOf(1L)))
            Assertions.assertFalse(id in kostCache.getProjektIdsWithActiveKost2Arts(listOf(1L)))

            // Activated again (no second unit), plus type 2 newly created.
            Assertions.assertTrue(projektKost2Service.activate(id, listOf(1L, 2L)))
            Assertions.assertEquals(setOf(1L, 2L), activeArtIds(id))
            Assertions.assertEquals(2, kostCache.getKost2ForProjekt(id, includeDeleted = true).size)
            Assertions.assertTrue(id in kostCache.getProjektIdsWithAllActiveKost2Arts(listOf(1L, 2L)))

            // A deleted unit is undeleted rather than inserted again (which would collide with its number).
            val kost2 = kostCache.getKost2ForProjekt(id).first { it.kost2Art?.id == 2L }
            kost2Dao.markAsDeleted(kost2Dao.find(kost2.id)!!)
            Assertions.assertEquals(setOf(1L), activeArtIds(id))
            Assertions.assertTrue(projektKost2Service.activate(id, listOf(2L)))
            Assertions.assertEquals(setOf(1L, 2L), activeArtIds(id))
            Assertions.assertEquals(2, kostCache.getKost2ForProjekt(id, includeDeleted = true).size)
            null
        }
    }

    @Test
    fun `activate refuses a project without range`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val id = insertProjekt(internKost2_4 = null, nummer = 12)
            val ex = assertThrows<UserException> { projektKost2Service.activate(id, listOf(1L)) }
            Assertions.assertEquals("validation.error.fieldRequired", ex.i18nKey)
            // Deactivating has nothing to create, so it stays a no-op.
            Assertions.assertFalse(projektKost2Service.deactivate(id, listOf(1L)))
            null
        }
    }

    private fun insertProjekt(internKost2_4: Int?, nummer: Int): Long {
        return projektDao.insert(ProjektDO().also {
            it.name = "ProjektKost2ServiceTest $nummer"
            it.internKost2_4 = internKost2_4
            it.nummer = nummer
        })
    }

    private fun activeArtIds(projektId: Long): Set<Long> {
        return kostCache.getKost2ForProjekt(projektId)
            .filter { projektKost2Service.isActive(it) }
            .mapNotNull { it.kost2Art?.id }
            .toSet()
    }
}
