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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.numberrange.NumberRangeViolation
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.springframework.beans.factory.annotation.Autowired

/**
 * Project numbers by the number ranges of application-test.properties (4 internal, 5 customers 0-999, 1 customers
 * 10000-19999), see [ProjektDao.assignProjektnummer].
 */
class ProjektDaoNumberRangeTest : AbstractTestBase() {
    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kostFormatter: KostFormatter

    @Test
    fun `project numbers are computed from customer and local number`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val legacyCustomer = insertKunde(777)
            val newCustomer = insertKunde(10007)
            val legacy = insertProjekt(legacyCustomer, nummer = 45)
            assertEquals(577745, legacy.projektnummer)
            assertEquals("5.777.45", kostFormatter.formatProjekt(legacy))
            assertEquals("5.777", kostFormatter.formatKunde(legacyCustomer))

            val new = insertProjekt(newCustomer, nummer = 3)
            assertEquals(100073, new.projektnummer)
            assertEquals("1.0007.3", kostFormatter.formatProjekt(new))
            assertEquals("100073", kostFormatter.formatProjekt(new, KostFormatter.FormatType.NUMBER))
            assertEquals("1.0007", kostFormatter.formatKunde(newCustomer))
            assertEquals("10007", kostFormatter.formatKunde(newCustomer, KostFormatter.FormatType.NUMBER))

            // Only 10 projects per customer of range 1:
            val ex = assertThrows<UserException> { insertProjekt(newCustomer, nummer = 10) }
            assertEquals(ProjektDao.LOCAL_NUMBER_OUT_OF_RANGE_I18N_KEY, ex.i18nKey)

            // Kost2 of the new range:
            val art = kost2ArtDao.find(2L, checkAccess = false) ?: Kost2ArtDO().also {
                it.id = 2L
                it.name = "Test"
                kost2ArtDao.insert(it, checkAccess = false)
            }
            val kost2 = Kost2DO().also { it.kost2Art = art }
            kost2Dao.setProjekt(kost2, new.id!!)
            kost2Dao.insert(kost2, checkAccess = false)
            assertEquals("1.0007.3.02", kost2.formattedNumber)
            assertEquals("10007302", kost2.rawNumberString)
            assertEquals(kost2.id, kost2Dao.getKost2("1.0007.3.02")?.id)
            null
        }
    }

    @Test
    fun `projects without customer and free numbers`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            // Internal project, legacy positional input (4.321.09):
            val internal = projektDao.find(projektDao.insert(ProjektDO().also {
                it.name = "internal legacy input"
                it.internKost2_4 = 321
                it.nummer = 9
            }))!!
            assertEquals(432109, internal.projektnummer)
            assertEquals(32109, internal.nummer)
            assertNull(internal.internKost2_4)
            assertEquals("4.321.09", kostFormatter.formatProjekt(internal))

            // Internal project by its project number:
            val internal2 = projektDao.find(projektDao.insert(ProjektDO().also {
                it.name = "internal"
                it.projektnummer = 432110
            }))!!
            assertEquals(32110, internal2.nummer)

            // Free customer (allow-free-numbers=true in the tests) with a free project number:
            val freeCustomer = insertKunde(4711)
            assertEquals("4711", kostFormatter.formatKunde(freeCustomer))
            val free = insertProjekt(freeCustomer, nummer = 1, projektnummer = 47110001)
            assertEquals("47110001", kostFormatter.formatProjekt(free))
            assertNull(free.nummernkreis, "No Kost2 for free project numbers.")
            assertEquals(47110001, free.nummer, "The free number is the local one, too.")
            // A free number must not lie in a range:
            val ex = assertThrows<UserException> { insertProjekt(freeCustomer, nummer = 2, projektnummer = 512345) }
            assertEquals(NumberRangeViolation.FREE_NUMBER_IN_RANGE.i18nKey, ex.i18nKey)
            // Not given: the next free one.
            val next = insertProjekt(freeCustomer, nummer = 3)
            assertEquals(47110002, next.projektnummer)
            // Project numbers are unique:
            val ex2 = assertThrows<UserException> { insertProjekt(freeCustomer, nummer = 4, projektnummer = 47110001) }
            assertEquals(ProjektDao.PROJEKTNUMMER_EXISTS_I18N_KEY, ex2.i18nKey)
            null
        }
    }

    @Test
    fun `local number of a project saved as free project is derived from its project number`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val customer = insertKunde(778)
            val projekt = insertProjekt(customer, nummer = 12)
            assertEquals(577812, projekt.projektnummer)
            // Saved without number ranges, the local number was the project number:
            projekt.nummer = 577812
            assertEquals(12, projektDao.localNummer(projekt))
            projektDao.update(projekt)
            assertEquals(12, projektDao.find(projekt.id)!!.nummer)
            null
        }
    }

    private fun insertKunde(nummer: Long): KundeDO {
        return kundeDao.find(nummer, checkAccess = false) ?: KundeDO().also {
            it.nummer = nummer
            it.name = "Customer $nummer"
            kundeDao.insert(it, checkAccess = false)
        }
    }

    private fun insertProjekt(kunde: KundeDO, nummer: Int, projektnummer: Int? = null): ProjektDO {
        val id = projektDao.insert(ProjektDO().also {
            it.name = "Project ${kunde.nummer}.$nummer"
            it.kunde = kunde
            it.nummer = nummer
            it.projektnummer = projektnummer
        })
        return projektDao.find(id)!!
    }
}
