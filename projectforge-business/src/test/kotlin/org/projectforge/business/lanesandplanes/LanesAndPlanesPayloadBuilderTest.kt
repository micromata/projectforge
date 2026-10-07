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

package org.projectforge.business.lanesandplanes

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.util.zip.GZIPInputStream

class LanesAndPlanesPayloadBuilderTest : AbstractTestBase() {
    @Autowired
    private lateinit var payloadBuilder: LanesAndPlanesPayloadBuilder

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Test
    fun bookableKost2() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val kost2a = addKost2(1)
            val kost2b = addKost2(2)
            val kost2c = addKost2(3)
            val kost2Nonactive = addKost2(4, KostentraegerStatus.NONACTIVE)
            val open = initTestDB.addTask("lpOpen", "root")
            open.kost2BlackWhiteList = "${kost2a.formattedNumber}, ${kost2b.formattedNumber}, ${kost2Nonactive.formattedNumber}"
            taskDao.update(open)
            val closed = initTestDB.addTask("lpClosed", "root")
            closed.kost2BlackWhiteList = kost2c.formattedNumber
            closed.status = TaskStatus.C
            taskDao.update(closed)
            val noBooking = initTestDB.addTask("lpNoBooking", "root")
            noBooking.kost2BlackWhiteList = kost2c.formattedNumber
            noBooking.timesheetBookingStatus = TimesheetBookingStatus.NO_BOOKING
            taskDao.update(noBooking)
            val withAccess = initTestDB.addUser("lpUser1")
            val withoutAccess = initTestDB.addUser("lpUser2")
            val group = initTestDB.addGroup("lpGroup", "lpUser1")
            listOf(open, closed, noBooking).forEach {
                initTestDB.createGroupTaskAccess(group, it, AccessType.OWN_TIMESHEETS, true, true, true, true)
            }

            val result = payloadBuilder.getBookableKost2(listOf(withAccess, withoutAccess), emptyList())
            Assertions.assertEquals(
                listOf(kost2a.formattedNumber, kost2b.formattedNumber),
                result[withAccess.id]?.map { it.formattedNumber }?.sorted(),
                "Only the active Kost2 of the open task.",
            )
            Assertions.assertNull(result[withoutAccess.id], "No task access, no Kost2.")
            Assertions.assertEquals(
                listOf(kost2b.formattedNumber),
                payloadBuilder.getBookableKost2(listOf(withAccess), listOf(2L))[withAccess.id]?.map { it.formattedNumber },
                "Filtered by Kost2Art.",
            )

            val employee = EmployeeDO().also { it.staffNumber = " 4711 " }
            withAccess.firstname = "Kai"
            withAccess.lastname = "Tester"
            val user = payloadBuilder.createUser(employee, withAccess, "k.tester@example.org", listOf(kost2b, kost2a), listOf(123L))
            Assertions.assertEquals("4711", user.personnelNumber)
            Assertions.assertNull(user.referenceCostCenter)
            Assertions.assertEquals(listOf(kost2a.formattedNumber, kost2b.formattedNumber), user.costUnits.map { it.ident })
            null
        }
    }

    @Test
    fun json() {
        val user = LanesAndPlanesUser(
            ident = "42",
            firstName = "Kai",
            lastName = "Tester",
            email = "k.tester@example.org",
            costUnits = listOf(LanesAndPlanesCostObject("5.123.45.01", "5.123.45.01: Development - Project")),
            accountingInvoiceProfileIds = listOf(123L),
        )
        val json = JsonUtils.toJson(LanesAndPlanesUsersRequest(listOf(user)), ignoreNullableProps = true)
        Assertions.assertEquals(
            """{"users":[{"ident":"42","first_name":"Kai","last_name":"Tester","email":"k.tester@example.org",""" +
                    """"cost_units":[{"ident":"5.123.45.01","name":"5.123.45.01: Development - Project"}],""" +
                    """"accounting_invoice_profile_ids":[123]}]}""",
            json,
        )
        val unzipped = GZIPInputStream(LanesAndPlanesSyncService.gzip(json).inputStream()).readBytes().toString(Charsets.UTF_8)
        Assertions.assertEquals(json, unzipped)
    }

    private fun addKost2(kost2ArtId: Long, status: KostentraegerStatus? = null): Kost2DO {
        val kost2 = Kost2DO()
        kost2.nummernkreis = 1
        kost2.bereich = 731
        kost2.teilbereich = 5
        kost2.kost2Art = Kost2ArtDO().withId(kost2ArtId)
        kost2.kostentraegerStatus = status
        return kost2Dao.find(kost2Dao.insert(kost2))!!
    }
}
