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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

/** The customer data sheet of an account is for those who may read accounts (Finance), not for everybody. */
class KontoCustomerDataSheetAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var kontoEntityRest: KontoEntityRest

    @Autowired
    private lateinit var kontoDao: KontoDao

    /** The number is unique, and the test database keeps the accounts of the other test. */
    private var nextNumber = 19000 + (System.nanoTime() % 900).toInt()

    private fun createKonto(): Long {
        logon(TEST_FINANCE_USER)
        val konto = KontoDO().also {
            it.nummer = nextNumber++
            it.bezeichnung = "Data sheet test"
        }
        return kontoDao.insert(konto, checkAccess = true)
    }

    @Test
    fun `an ordinary user is refused`() {
        val id = createKonto()
        logon(TEST_USER)
        assertThrows<AccessException> { kontoEntityRest.getCustomerDataSheet(id) }
    }

    @Test
    fun `the finance user gets the pdf`() {
        val id = createKonto()
        logon(TEST_FINANCE_USER)
        val response = kontoEntityRest.getCustomerDataSheet(id)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(true, response.headers.getFirst("Content-Disposition")?.contains(".pdf"))
    }
}
