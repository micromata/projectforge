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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.BankAccountConfig
import org.projectforge.business.fibu.EInvoiceExportService
import org.projectforge.business.fibu.EInvoiceSellerConfig
import org.projectforge.business.fibu.InvoiceService
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.files.AttachmentsService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.files.store.FileStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The endpoints of the e-invoice checker page. What the parser makes of a file is `EInvoiceReadService`'s
 * business (see `EInvoiceExportServiceTest`); what belongs here is what the endpoints decide: that a check is
 * answered and kept for the session, that a file which can't be checked at all is a 400 with the reason, and
 * who may use the page.
 */
class EInvoiceCheckerRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var eInvoiceCheckerRest: EInvoiceCheckerRest

    @Autowired
    private lateinit var groupDao: GroupDao

    @Test
    fun `an uploaded XRechnung is answered and kept as the session's last result`() {
        logon(TEST_FINANCE_USER)
        val request = newRequest()
        assertEquals(HttpStatus.NO_CONTENT, eInvoiceCheckerRest.getResult(request).statusCode, "Nothing checked yet.")

        val response = eInvoiceCheckerRest.upload(request, xmlFile("XRechnung_2024001.xml", createXRechnung()))
        assertEquals(HttpStatus.OK, response.statusCode)
        val result = response.body as EInvoiceCheckerRest.CheckResult
        assertEquals("XRechnung_2024001.xml", result.filename)
        val data = result.invoiceData
        assertEquals("2024001", data.invoiceNumber)
        assertEquals("Example Seller GmbH", data.seller?.name)
        assertEquals("Testkundin GmbH", data.buyer?.name)
        assertEquals(2, data.lineItems.size)
        assertEquals(0, BigDecimal("2100.00").compareTo(data.totalNetAmount), "Net: ${data.totalNetAmount}")

        val last = eInvoiceCheckerRest.getResult(request)
        assertEquals(HttpStatus.OK, last.statusCode, "A reload shows the check again.")
        assertEquals("2024001", last.body!!.invoiceData.invoiceNumber)
        assertEquals(
            HttpStatus.NOT_FOUND,
            eInvoiceCheckerRest.downloadAttachment(4711, request).statusCode,
            "No such attachment.",
        )
    }

    @Test
    fun `a file that can't be checked is refused with the reason`() {
        logon(TEST_FINANCE_USER)
        val request = newRequest()
        val empty = eInvoiceCheckerRest.upload(request, xmlFile("empty.xml", ByteArray(0)))
        assertEquals(HttpStatus.BAD_REQUEST, empty.statusCode)
        val wrongType = eInvoiceCheckerRest.upload(
            request,
            MockMultipartFile("file", "invoice.txt", "text/plain", "not an e-invoice".toByteArray()),
        )
        assertEquals(HttpStatus.BAD_REQUEST, wrongType.statusCode)
        assertNotNull(wrongType.body as? String)
        assertTrue((wrongType.body as String).isNotBlank(), "The reason is the body.")
        assertEquals(HttpStatus.NO_CONTENT, eInvoiceCheckerRest.getResult(request).statusCode, "Nothing kept.")
    }

    @Test
    fun `a user outside the finance and orga groups without order book right reaches none of the endpoints`() {
        logon(TEST_USER)
        val request = newRequest()
        assertThrows<AccessException> { eInvoiceCheckerRest.getResult(request) }
        assertThrows<AccessException> { eInvoiceCheckerRest.upload(request, xmlFile("x.xml", createXRechnung())) }
        assertThrows<AccessException> { eInvoiceCheckerRest.downloadAttachment(1, request) }
    }

    @Test
    fun `a project manager with the order book right reaches the endpoints`() {
        logon(TEST_ADMIN_USER)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "EInvoiceCheckerRestTest.orderBook"
            it.addRight(UserRightDO(AuftragDao.USER_RIGHT_ID, UserRightValue.PARTLYREADWRITE))
        })
        val group = getGroup(PROJECT_MANAGER)
        group.addUser(user)
        groupDao.update(group)
        logon(user)
        val request = newRequest()
        assertEquals(HttpStatus.NO_CONTENT, eInvoiceCheckerRest.getResult(request).statusCode)
        assertEquals(
            HttpStatus.OK,
            eInvoiceCheckerRest.upload(request, xmlFile("XRechnung_2024002.xml", createXRechnung())).statusCode,
        )
    }

    private fun newRequest() = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }

    private fun xmlFile(name: String, content: ByteArray) = MockMultipartFile("file", name, "application/xml", content)

    /** A real XRechnung, as the export writes it — built by hand, since a test installation configures no seller. */
    private fun createXRechnung(): ByteArray {
        val sellerConfig = EInvoiceSellerConfig().apply {
            name = "Example Seller GmbH"
            street = "Musterstraße 1-5"
            zip = "12345"
            city = "Musterstadt"
            country = "DE"
            vatId = "DE123456789"
            email = "info@example.org"
            bankAccounts = mutableListOf(BankAccountConfig().apply { iban = "DE89370400440532013000" })
        }
        val service = EInvoiceExportService(
            sellerConfig,
            Mockito.mock(InvoiceService::class.java),
            Mockito.mock(AttachmentsService::class.java),
            Mockito.mock(FileStore::class.java),
            Mockito.mock(RechnungDao::class.java),
        )
        val konto = KontoDO().apply {
            nummer = 10000
            bezeichnung = "Testkundin GmbH"
            street = "Musterstraße 42"
            zipCode = "12345"
            city = "Berlin"
            country = "DE"
            vatId = "DE987654321"
            leitwegId = "04011000-1234512345-12"
            eInvoiceEmail = "rechnung@testkundin.de"
        }
        val invoice = RechnungDO().apply {
            nummer = 2024001
            datum = LocalDate.of(2024, 6, 15)
            faelligkeit = LocalDate.of(2024, 7, 15)
            typ = RechnungTyp.RECHNUNG
            kunde = KundeDO().apply {
                nummer = 1L
                name = "Testkundin GmbH"
                this.konto = konto
            }
            sellerBankAccount = "DE89370400440532013000"
            positionen = mutableListOf(
                position(1, "Softwareentwicklung", "10", "150.00"),
                position(2, "Projektmanagement", "5", "120.00"),
            )
        }
        return service.exportAsXRechnung(invoice)
    }

    private fun position(number: Short, text: String, menge: String, einzelNetto: String) =
        RechnungsPositionDO().apply {
            this.number = number
            this.text = text
            this.menge = BigDecimal(menge)
            this.einzelNetto = BigDecimal(einzelNetto)
            vat = BigDecimal("0.19")
        }
}
