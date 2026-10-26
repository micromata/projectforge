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
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.numberrange.NumberRangeService
import org.projectforge.business.fibu.numberrange.NumberRangeViolation
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.configuration.ConfigurationJsonValidators
import org.projectforge.framework.configuration.ConfigurationJsonValues
import org.projectforge.framework.configuration.ConfigurationParam
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

    @Autowired
    private lateinit var numberRangeService: NumberRangeService

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
            val art = kost2Art()
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

    @Test
    fun `Kost2 of projects only in a number range, those of 4 and 5 tell the former ranges in use`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val art = kost2Art()
            // A free project number isn't made of Kost2, even if digits are given (5.123.45 is no project of 4713). Below
            // the free numbers of the other tests, which expect their maximum + 1 as next free number:
            val free = insertProjekt(insertKunde(4713), nummer = 1, projektnummer = 600001)
            val ex = assertThrows<UserException> {
                kost2Dao.insert(Kost2DO().also {
                    it.projekt = free
                    it.nummernkreis = 5
                    it.bereich = 123
                    it.teilbereich = 45
                    it.kost2Art = art
                }, checkAccess = false)
            }
            assertEquals(Kost2Dao.PROJEKT_WITHOUT_NUMBER_RANGE_I18N_KEY, ex.i18nKey)

            // Kost2 of a project of range 1 aren't those of the former ranges, Kost2 of a project of range 5 are:
            val before = numberRangeService.countLegacyKost2()
            val new = insertProjekt(insertKunde(10008), nummer = 4)
            kost2Dao.insert(Kost2DO().also { kost2Dao.setProjekt(it, new.id!!); it.kost2Art = art }, checkAccess = false)
            assertEquals(before, numberRangeService.countLegacyKost2())
            val legacy = insertProjekt(insertKunde(779), nummer = 7)
            kost2Dao.insert(Kost2DO().also { kost2Dao.setProjekt(it, legacy.id!!); it.kost2Art = art }, checkAccess = false)
            assertEquals(before + 1, numberRangeService.countLegacyKost2())
            // Configured ranges win anyway:
            assertEquals(NumberRangeService.Mode.CONFIGURED, numberRangeService.mode)
            null
        }
    }

    @Test
    fun `a free project number skips the local numbers of the customer's projects without project number`() {
        persistenceService.runInTransaction { context ->
            logon(TEST_FINANCE_USER)
            val customer = insertKunde(4712)
            val first = insertProjekt(customer, nummer = 1)
            val taken = first.projektnummer!!
            // As left by the migration (or its rollback): the local number stays, the project number is gone.
            context.executeUpdate(
                "UPDATE ProjektDO p SET p.projektnummer = NULL WHERE p.id = :id",
                Pair("id", first.id),
            )
            context.em.clear()
            val next = insertProjekt(customer, nummer = 2)
            assertEquals(taken + 1, next.projektnummer)
            assertEquals(taken + 1, next.nummer)
            // Given by hand, the number is rejected as local number of the customer instead of failing on flush:
            val ex = assertThrows<UserException> { insertProjekt(customer, nummer = 3, projektnummer = taken) }
            assertEquals(ProjektDao.NUMBER_EXISTS_FOR_CUSTOMER_I18N_KEY, ex.i18nKey)
            null
        }
    }

    @Test
    fun `next free local number of a customer's range and of the internal range`() {
        persistenceService.runInTransaction { context ->
            logon(TEST_FINANCE_USER)
            val customer = insertKunde(10008)
            assertEquals(0, projektDao.nextFreeLocalNumber(customer, null))
            val first = insertProjekt(customer, nummer = 0)
            // A deleted project keeps its number:
            projektDao.markAsDeleted(insertProjekt(customer, nummer = 1))
            // As left by the migration: no project number, but the local number is taken.
            val noNumber = insertProjekt(customer, nummer = 2)
            context.executeUpdate(
                "UPDATE ProjektDO p SET p.projektnummer = NULL WHERE p.id = :id",
                Pair("id", noNumber.id),
            )
            context.em.clear()
            assertEquals(3, projektDao.nextFreeLocalNumber(customer, null))
            // The project's own number isn't taken for itself:
            assertEquals(0, projektDao.nextFreeLocalNumber(customer, first.id))
            // Customer of no range:
            assertNull(projektDao.nextFreeLocalNumber(insertKunde(4713), null))

            // Internal range (without customers):
            val internalNext = projektDao.nextFreeLocalNumber(null, null)!!
            projektDao.insert(ProjektDO().also {
                it.name = "internal next free"
                it.projektnummer = 400000 + internalNext
            })
            val afterwards = projektDao.nextFreeLocalNumber(null, null)!!
            assertTrue(afterwards > internalNext, "$afterwards must be above $internalNext")
            null
        }
    }

    @Test
    fun `next free local number starts at the configured first project number`() {
        ConfigurationJsonValues.internalSetForTest(ConfigurationParam.FIBU_ACCOUNTING, """{"firstProjectNumbers":{"5":1}}""")
        try {
            persistenceService.runInTransaction { _ ->
                logon(TEST_FINANCE_USER)
                val customer = insertKunde(778)
                assertEquals(1, projektDao.nextFreeLocalNumber(customer, null))
                assertEquals(577801, insertProjekt(customer, nummer = 1).projektnummer)
                assertEquals(2, projektDao.nextFreeLocalNumber(customer, null))
                // Lower numbers can still be given by hand:
                assertEquals(577800, insertProjekt(customer, nummer = 0).projektnummer)
                // Other ranges start at 0:
                assertEquals(0, projektDao.nextFreeLocalNumber(insertKunde(10009), null))
                null
            }
        } finally {
            ConfigurationJsonValues.internalSetForTest(ConfigurationParam.FIBU_ACCOUNTING, null)
        }
    }

    @Test
    fun `first project numbers are validated against the number ranges`() {
        fun errors(json: String) = ConfigurationJsonValidators.validate(ConfigurationParam.FIBU_ACCOUNTING.key, json)
        assertEquals(emptyList<String>(), errors("""{"firstProjectNumbers":{"5":1,"1":9}}"""))
        assertEquals(1, errors("""{"firstProjectNumbers":{"5":100}}""").size, "Only 2 project digits.")
        assertEquals(1, errors("""{"firstProjectNumbers":{"1":10}}""").size, "Only 1 project digit.")
        assertEquals(1, errors("""{"firstProjectNumbers":{"7":1}}""").size, "No range 7.")
    }

    @Test
    fun `a collision on the unique project number is told by the constraint in any cause`() {
        // Two projects saved at the same time pass the check of assignProjektnummer, the second fails on flush:
        val hsqldb = RuntimeException(
            "could not execute statement",
            java.sql.SQLIntegrityConstraintViolationException(
                "integrity constraint violation: unique constraint or index violation ; UNIQUE_T_FIBU_PROJEKT_PROJEKTNUMMER table: T_FIBU_PROJEKT"
            ),
        )
        assertTrue(ProjektDao.isProjektnummerCollision(hsqldb))
        assertTrue(
            ProjektDao.isProjektnummerCollision(
                IllegalStateException("duplicate key value violates unique constraint \"unique_t_fibu_projekt_projektnummer\"")
            )
        )
        assertFalse(ProjektDao.isProjektnummerCollision(RuntimeException("other", RuntimeException("unique_t_fibu_kunde"))))
    }

    private fun kost2Art(): Kost2ArtDO = kost2ArtDao.find(2L, checkAccess = false) ?: Kost2ArtDO().also {
        it.id = 2L
        it.name = "Test"
        kost2ArtDao.insert(it, checkAccess = false)
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
