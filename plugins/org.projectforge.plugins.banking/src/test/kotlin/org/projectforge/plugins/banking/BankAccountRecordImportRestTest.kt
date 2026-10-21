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

package org.projectforge.plugins.banking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.rest.importer.ImportView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile

/**
 * The access rule of the import of bank account transactions: only the finance group may use it, and only into an
 * account the user has write access to; the account is always the stored one of the given id.
 */
class BankAccountRecordImportRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var bankAccountDao: BankAccountDao

    @Autowired
    private lateinit var bankAccountRecordImportRest: BankAccountRecordImportRest

    @Test
    fun `the import is refused for an ordinary user`() {
        val accountId = createAccount("Import refused", getUserId(TEST_FINANCE_USER))
        logon(TEST_USER)
        assertThrows<AccessException> { upload(accountId) }
    }

    @Test
    fun `the import is allowed into an account with write access only`() {
        val financeUserId = getUserId(TEST_FINANCE_USER)
        val writableId = createAccount("Import writable", financeUserId)
        val foreignId = createAccount("Import foreign", getUserId(TEST_USER2))
        logon(TEST_FINANCE_USER)

        val response = upload(writableId)
        assertEquals(200, response.statusCode.value(), "${response.body}")
        val view = response.body as ImportView<*>
        assertEquals(writableId, view.meta?.get("bankAccountId"))
        assertEquals(listOf(ImportEntry.Status.NEW, ImportEntry.Status.NEW), view.entries.map { it.status })

        // Another account of the finance group, but without write access of the user.
        assertEquals(400, upload(foreignId).statusCode.value())
        // An unknown account.
        assertEquals(400, upload(-4711).statusCode.value())
        // No account at all.
        assertEquals(400, upload(null).statusCode.value())
    }

    @Test
    fun `records missing a required value or of another account are faulty, each error once`() {
        val accountId = createAccount("Import faulty", getUserId(TEST_FINANCE_USER))
        logon(TEST_FINANCE_USER)
        val csv = """
            Date;Value date;Amount;Subject;Account
            ;01.12.2099;7.00;No date;$IBAN
            01.12.2099;;12.34;No value date;$IBAN
            01.12.2099;01.12.2099;;No amount;$IBAN
            01.12.2099;01.12.2099;1.00;Other account;DE99999999999999999999
            02.12.2099;02.12.2099;2.00;Fine;$IBAN
            03.12.2099;03.12.2099;3.00;Fine too;$IBAN
        """.trimIndent()
        val response = upload(accountId, csv)
        assertEquals(200, response.statusCode.value(), "${response.body}")
        val entries = (response.body as ImportView<*>).entries.associateBy { (it.read as BankAccountRecord).subject }
        // Without a date it can't be paired with a stored record, but is shown, not dropped.
        assertEquals(ImportEntry.Status.FAULTY, entries["No date"]?.status)
        assertEquals(ImportEntry.Status.FAULTY, entries["No value date"]?.status)
        assertEquals(ImportEntry.Status.FAULTY, entries["No amount"]?.status)
        assertEquals(ImportEntry.Status.FAULTY, entries["Other account"]?.status)
        // Checked once after the reconciliation, not once per later day of the file.
        assertEquals(1, entries["Other account"]?.getErrors()?.size)
        assertEquals(ImportEntry.Status.NEW, entries["Fine"]?.status)
        assertEquals(ImportEntry.Status.NEW, entries["Fine too"]?.status)
    }

    @Test
    fun `a file of undated records only is shown as faulty`() {
        val accountId = createAccount("Import undated", getUserId(TEST_FINANCE_USER))
        logon(TEST_FINANCE_USER)
        val csv = """
            Date;Value date;Amount;Subject;Account
            ;01.12.2099;7.00;No date;$IBAN
        """.trimIndent()
        val response = upload(accountId, csv)
        assertEquals(200, response.statusCode.value(), "${response.body}")
        val view = response.body as ImportView<*>
        assertEquals(listOf(ImportEntry.Status.FAULTY), view.entries.map { it.status })
    }

    private fun createAccount(name: String, fullAccessUserId: Long): Long {
        val account = BankAccountDO()
        account.name = name
        account.bank = "Test bank"
        account.iban = IBAN
        account.fullAccessUserIds = "$fullAccessUserId"
        account.importSettings = SETTINGS
        return bankAccountDao.insert(account, checkAccess = false)
    }

    private fun upload(accountId: Long?, csv: String = CSV): ResponseEntity<*> {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        accountId?.let { request.setParameter("bankAccountId", it.toString()) }
        val file = MockMultipartFile("file", "transactions.csv", "text/csv", csv.toByteArray())
        return bankAccountRecordImportRest.upload(request, file)
    }

    companion object {
        private const val IBAN = "DE02120300000000202051"

        private val SETTINGS = """
            date=Date|:dd.MM.yyyy
            valueDate=Value date|:dd.MM.yyyy
            amount=Amount
            subject=Subject
            account=Account
        """.trimIndent()

        // Far in the future, so no transaction of the test data is in the period of the file.
        private val CSV = """
            Date;Value date;Amount;Subject;Account
            01.12.2099;01.12.2099;12.34;First;$IBAN
            02.12.2099;02.12.2099;-5.00;Second;$IBAN
        """.trimIndent()
    }
}
