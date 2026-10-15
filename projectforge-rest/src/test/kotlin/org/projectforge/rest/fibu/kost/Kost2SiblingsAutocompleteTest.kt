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

package org.projectforge.rest.fibu.kost

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest

/**
 * `cost2/autosearch?siblingsOf=`: re-opening the cost 2 picker of a cost assignment offers the units of the
 * same `x.xxx.xx` prefix instead of the first units in number order.
 */
class Kost2SiblingsAutocompleteTest : AbstractTestBase() {
    @Autowired
    private lateinit var kost2EntityRest: Kost2EntityRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Test
    fun `an empty term with siblingsOf answers the units of the same prefix`() {
        logon(TEST_FINANCE_USER)
        val project = insertProject(31)
        val other = insertProject(32)
        val (k1, k2, k3) = listOf(61L, 62L, 63L).map { insertKost2(project, it) }
        val ended = insertKost2(project, 64L, KostentraegerStatus.ENDED)
        insertKost2(other, 61L)

        assertEquals(listOf(k1, k2, k3).map { it.id }, search(null, k2.id))
        // The current value stays in the list even when it is no longer active.
        assertEquals(listOf(k1, k2, k3, ended).map { it.id }, search(null, ended.id))
    }

    @Test
    fun `a typed term or an unknown id searches as usual`() {
        logon(TEST_FINANCE_USER)
        val project = insertProject(33)
        val other = insertProject(34)
        val own = insertKost2(project, 61L)
        val foreign = insertKost2(other, 62L)

        assertTrue(search("Kost2SiblingsAutocompleteTest 34", own.id).contains(foreign.id))
        assertTrue(search(null, -1L).isNotEmpty(), "unknown id: the first units of all")
    }

    private fun search(term: String?, siblingsOf: Long?): List<Long?> {
        val request = MockHttpServletRequest().also { req ->
            siblingsOf?.let { req.setParameter("siblingsOf", it.toString()) }
        }
        return kost2EntityRest.getAutoCompleteObjects(request, term, null).map { it.id as Long? }
    }

    private fun insertProject(nummer: Int): ProjektDO = ProjektDO().also {
        it.name = "Project Kost2SiblingsAutocompleteTest $nummer"
        it.internKost2_4 = 103
        it.nummer = nummer
        projektDao.insert(it, checkAccess = false)
    }

    private fun insertKost2(
        project: ProjektDO,
        artId: Long,
        status: KostentraegerStatus? = null,
    ): Kost2DO {
        val art = kost2ArtDao.find(artId, checkAccess = false) ?: Kost2ArtDO().also {
            it.id = artId
            it.name = "Kost2Art $artId of Kost2SiblingsAutocompleteTest"
            kost2ArtDao.insert(it, checkAccess = false)
        }
        return Kost2DO().also {
            it.projekt = project
            it.nummernkreis = project.nummernkreis
            it.bereich = project.bereich!!
            it.teilbereich = project.nummer
            it.kost2Art = art
            it.kostentraegerStatus = status
            kost2Dao.insert(it, checkAccess = false)
        }
    }
}
