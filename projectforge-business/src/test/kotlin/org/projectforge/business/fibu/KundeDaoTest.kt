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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable

class KundeDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var kundeDao: KundeDao

    @Test
    fun checkAccess() {
        logon(TEST_FINANCE_USER)
        var kunde = KundeDO()
        kunde.name = "ACME"
        kunde.id = 42L
        val id: Serializable = kundeDao.insert(kunde)
        kunde = kundeDao.find(id)!!
        kunde.description = "Test"
        kundeDao.update(kunde)

        logon(TEST_CONTROLLING_USER)
        kundeDao.find(id)
        checkNoWriteAccess(kunde, "Controlling")

        logon(TEST_USER)
        checkNoAccess(id, kunde, "Other")

        logon(TEST_PROJECT_MANAGER_USER)
        checkNoWriteAccess(kunde, "Project manager")
        checkNoHistoryAccess(kunde, "Project manager")

        logon(TEST_ADMIN_USER)
        checkNoAccess(id, kunde, "Admin ")
    }

    private fun checkNoAccess(id: Serializable, kunde: KundeDO, who: String) {
        try {
            val filter = BaseSearchFilter()
            suppressErrorLogs {
                kundeDao.select(filter)
            }
            Assertions.fail { "AccessException expected: $who users should not have select list access to customers." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            suppressErrorLogs {
                kundeDao.find(id)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to customers." }
        } catch (ex: AccessException) {
            // OK
        }
        checkNoHistoryAccess(kunde, who)
        checkNoWriteAccess(kunde, who)
    }

    private fun checkNoHistoryAccess(kunde: KundeDO, who: String) {
        Assertions.assertEquals(
            kundeDao.hasLoggedInUserHistoryAccess(false), false,
            "$who users should not have select access to history of customers."
        )
        try {
            suppressErrorLogs {
                kundeDao.hasLoggedInUserHistoryAccess(true)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to history of customers." }
        } catch (ex: AccessException) {
            // OK
        }
        Assertions.assertEquals(
            kundeDao.hasLoggedInUserHistoryAccess(kunde, false), false,
            "$who users should not have select access to history of customers."
        )
        try {
            suppressErrorLogs {
                kundeDao.hasLoggedInUserHistoryAccess(kunde, true)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to history of invoices." }
        } catch (ex: AccessException) {
            // OK
        }
    }

    private fun checkNoWriteAccess(kunde: KundeDO, who: String) {
        try {
            val ku = KundeDO()
            ku.id = 42L
            kunde.name = "ACME 2"
            suppressErrorLogs {
                kundeDao.insert(ku)
            }
            Assertions.fail { "AccessException expected: $who users should not have save access to customers." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            kunde.description = who
            suppressErrorLogs {
                kundeDao.update(kunde)
            }
            Assertions.fail { "AccessException expected: $who users should not have update access to customers." }
        } catch (ex: AccessException) {
            // OK
        }
    }
}
