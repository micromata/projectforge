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


package org.projectforge.business.testdata

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.database.DatabaseInitTestDataService
import org.projectforge.framework.persistence.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import java.util.TimeZone

class TestDataSeederTest : AbstractTestBase() {
    @Autowired
    private lateinit var databaseService: DatabaseService

    @Autowired
    private lateinit var testDataSeeder: TestDataSeeder

    @Autowired
    private lateinit var userDao: UserDao

    /** The seeder needs an empty database, as the setup does. */
    override fun initDb() {
        init(false)
    }

    @Test
    fun `seed an empty database`() {
        Assertions.assertFalse(databaseService.databaseTablesWithEntriesExist())
        databaseService.insertGlobalAddressbook() // As the setup does before.
        val ctx = testDataSeeder.seedCore("Test-Data-2026!", TimeZone.getTimeZone("Europe/Berlin"))
        // The demo scripts don't fit into the test schema: hbm2ddl creates T_SCRIPT.SCRIPT with 255 bytes only
        // (a BLOB in the Flyway schema of a real database).
        Assertions.assertEquals(
            emptyList<String>(),
            ctx.failed - DatabaseInitTestDataService::class.simpleName,
            "Failed contributors (see log).",
        )

        Assertions.assertNotNull(userDao.getInternalByName(DatabaseService.DEFAULT_ADMIN_USER))
        Assertions.assertEquals("Laura", userDao.getInternalByName("laura")?.firstname)
        Assertions.assertTrue(
            userService.authenticateUser("laura", "Test-Data-2026!".toCharArray()) != null,
            "The sample users get the given password.",
        )
        Assertions.assertEquals(10L, count("PFUserDO"))
        Assertions.assertEquals(5L, count("KundeDO"))
        Assertions.assertEquals(6L, count("ProjektDO"))
        Assertions.assertEquals(11L, count("AuftragDO"))
        Assertions.assertEquals(9L, count("EmployeeDO"))
        Assertions.assertTrue(count("TimesheetDO") > 1000, "time sheets")
        Assertions.assertTrue(count("RechnungDO") > 50, "invoices")
        Assertions.assertTrue(count("EingangsrechnungDO") > 30, "incoming invoices")
        Assertions.assertEquals(9L, count("VacationDO"), "vacations (refused ones are logged)")
        Assertions.assertTrue(count("TeamEventDO") >= 10, "calendar events")
        Assertions.assertEquals(8L, count("AddressDO"))
        // Every invoice position refers to its order position.
        Assertions.assertEquals(
            0L,
            persistenceService.runReadOnly { context ->
                context.em.createQuery(
                    "select count(p) from RechnungsPositionDO p where p.auftragsPosition is null",
                    java.lang.Long::class.java,
                ).singleResult.toLong()
            },
        )
    }

    private fun count(entity: String): Long = persistenceService.runReadOnly { context ->
        context.em.createQuery("select count(e) from $entity e", java.lang.Long::class.java).singleResult.toLong()
    }
}
