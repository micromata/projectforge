/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.AccessException
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.time.LocalDate

class EingangsrechnungDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Test
    fun checkAccess() {
        logon(TEST_FINANCE_USER)
        var eingangsrechnung = EingangsrechnungDO()
        eingangsrechnung.datum = LocalDate.now()
        eingangsrechnung.addPosition(EingangsrechnungsPositionDO())
        eingangsrechnung.faelligkeit = LocalDate.now()
        val id: Serializable = eingangsrechnungDao.insert(eingangsrechnung)
        eingangsrechnung = eingangsrechnungDao.find(id)!!

        logon(TEST_CONTROLLING_USER)
        eingangsrechnungDao.find(id)
        checkNoWriteAccess(id, eingangsrechnung, "Controlling")

        logon(TEST_USER)
        checkNoAccess(id, eingangsrechnung, "Other")

        logon(TEST_PROJECT_MANAGER_USER)
        checkNoAccess(id, eingangsrechnung, "Project manager")

        logon(TEST_ADMIN_USER)
        checkNoAccess(id, eingangsrechnung, "Admin ")
    }

    private fun checkNoAccess(id: Serializable, eingangsrechnung: EingangsrechnungDO, who: String) {
        try {
            val filter = RechnungFilter()
            suppressErrorLogs {
                eingangsrechnungDao.select(filter)
            }
            Assertions.fail { "AccessException expected: $who users should not have select list access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            suppressErrorLogs {
                eingangsrechnungDao.find(id)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        checkNoHistoryAccess(id, eingangsrechnung, who)
        checkNoWriteAccess(id, eingangsrechnung, who)
    }

    private fun checkNoHistoryAccess(id: Serializable, eingangsrechnung: EingangsrechnungDO, who: String) {
        Assertions.assertEquals(
            eingangsrechnungDao.hasLoggedInUserHistoryAccess(false), false,
            "$who users should not have select access to history of invoices."
        )
        try {
            suppressErrorLogs {
                eingangsrechnungDao.hasLoggedInUserHistoryAccess(true)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to history of invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        Assertions.assertEquals(
            eingangsrechnungDao.hasLoggedInUserHistoryAccess(eingangsrechnung, false), false,
            "$who users should not have select access to history of invoices."
        )
        try {
            suppressErrorLogs {
                eingangsrechnungDao.hasLoggedInUserHistoryAccess(eingangsrechnung, true)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to history of invoices." }
        } catch (ex: AccessException) {
            // OK
        }
    }

    private fun checkNoWriteAccess(id: Serializable, eingangsrechnung: EingangsrechnungDO, who: String) {
        try {
            val re = EingangsrechnungDO()
            re.datum = LocalDate.now()
            suppressErrorLogs {
                eingangsrechnungDao.insert(re)
            }
            Assertions.fail { "AccessException expected: $who users should not have save access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            eingangsrechnung.bemerkung = who
            suppressErrorLogs {
                eingangsrechnungDao.update(eingangsrechnung)
            }
            Assertions.fail { "AccessException expected: $who users should not have update access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
    }
}
