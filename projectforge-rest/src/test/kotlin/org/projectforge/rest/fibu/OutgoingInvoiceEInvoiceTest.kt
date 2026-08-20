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

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.BankAccountConfig
import org.projectforge.business.fibu.EInvoiceSellerConfig
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The e-invoice of one outgoing invoice — the three endpoints of [OutgoingInvoiceEntityRest] Wicket has as its
 * `EInvoiceModalDialog` (`RechnungEditForm`).
 *
 * The XML itself is `EInvoiceExportService`'s and Mustang's business. What belongs here is what the endpoints
 * decide: that the validation is *readable before* the export, so the form can name what is missing instead of
 * showing a failed download (Wicket's error line does the same); that an invoice which isn't ready is refused
 * with that very list rather than with the `IllegalStateException` the service throws; and that both of them
 * require the groups of `EInvoiceCheckerPageRest` — an e-invoice is the document that goes to the customer.
 *
 * A test installation configures no seller (`projectforge.einvoice.seller.*`), which is exactly the state
 * `configured = false` is for. The exporting cases therefore configure one on the bean for their duration and
 * put it back afterwards; without that, "a complete invoice exports" could not be tested at all.
 */
class OutgoingInvoiceEInvoiceTest : AbstractTestBase() {

    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var sellerConfig: EInvoiceSellerConfig

    @Autowired
    private lateinit var repoService: RepoService

    @PostConstruct
    private fun postConstruct() {
        // Both exports read the JCR — XRechnung embeds the regular attachments into the XML, ZUGFeRD into the
        // PDF — so a repository has to exist even for an invoice that has none.
        initJCRTestRepo(MODULE_NAME, "outgoingInvoiceEInvoiceTestRepo")
    }

    /** Releases the repository directory again, as `OutgoingInvoicePdfTest` does. */
    override fun afterAll() {
        repoService.shutdown()
    }

    /** The configuration is a singleton bean of the shared context, so no case may leave its seller behind. */
    @AfterEach
    fun resetSellerConfig() {
        sellerConfig.name = ""
        sellerConfig.street = ""
        sellerConfig.zip = ""
        sellerConfig.city = ""
        sellerConfig.vatId = ""
        sellerConfig.taxNumber = ""
        sellerConfig.bankAccounts = mutableListOf()
    }

    @Test
    fun `an unconfigured installation says so instead of listing it as a problem of the invoice`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("E-invoice unconfigured", planned = true)

        val validation = outgoingInvoiceEntityRest.validateEInvoice(id)

        // Nothing an editing user can fix, which is why it is a flag of its own: the client explains the
        // setting rather than putting it among the invoice's own missing fields.
        assertFalse(validation.configured)
        assertTrue(
            validation.errors.any { it.contains("Seller configuration") },
            "`validate` names it as well, since Wicket's dialog has only the list: ${validation.errors}",
        )
    }

    @Test
    fun `a planned invoice is not exportable, and the validation names every reason`() {
        logon(TEST_FINANCE_USER)
        configureSeller()
        val id = insertInvoice("E-invoice incomplete", planned = true)

        val validation = outgoingInvoiceEntityRest.validateEInvoice(id)

        assertTrue(validation.configured)
        // A planned invoice spends no invoice number, and this one selected no bank account and carries no
        // address — the three the form has to point at. Named individually rather than counted, since
        // `validate` is free to grow another rule.
        assertTrue(validation.errors.any { it.contains("Invoice number") }, "${validation.errors}")
        assertTrue(validation.errors.any { it.contains("bank account") }, "${validation.errors}")
        assertTrue(validation.errors.any { it.contains("address") }, "${validation.errors}")
    }

    @Test
    fun `an invoice that is not exportable is refused with the list rather than with a stack trace`() {
        logon(TEST_FINANCE_USER)
        configureSeller()
        val id = insertInvoice("E-invoice refusal", planned = true)

        // Both exports, because the check sits in front of both: the service answers a refusal by throwing
        // `IllegalStateException`, i.e. a 500, and an export URL can be called without asking `validate` first.
        listOf(
            outgoingInvoiceEntityRest.exportXRechnung(id),
            outgoingInvoiceEntityRest.exportZugferd(id),
        ).forEach { response ->
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            val body = response.body as String
            assertTrue(body.contains("Invoice number"), "The answer is what `validate` said: $body")
        }
    }

    @Test
    fun `a complete invoice is answered as XRechnung XML`() {
        logon(TEST_FINANCE_USER)
        configureSeller()
        val id = insertInvoice("E-invoice complete", planned = false)

        val response = outgoingInvoiceEntityRest.exportXRechnung(id)

        assertEquals(HttpStatus.OK, response.statusCode)
        val xml = (response.body as Resource).inputStream.readBytes().decodeToString()
        // The document the customer receives: an invoice with the number and the net amount of the position in
        // it. The net amount is the part `RechnungCalculator.calculate` has to have filled — without it the
        // sums live in an unset `lateinit` and the export throws (see `exportEInvoice`).
        assertTrue(xml.contains("CrossIndustryInvoice"), "A CII document: ${xml.take(200)}")
        assertTrue(xml.contains("1000.00"), "The net amount of the single position is stated.")
        val disposition = response.headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
        assertNotNull(disposition)
        assertTrue(disposition!!.endsWith(".xml"), "Named by `getExportFilename`: $disposition")
    }

    @Test
    fun `a user outside the finance and orga groups reaches none of the three endpoints`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("E-invoice access", planned = true)

        logon(TEST_USER)
        assertThrows<AccessException> { outgoingInvoiceEntityRest.validateEInvoice(id) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.exportXRechnung(id) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.exportZugferd(id) }
    }

    @Test
    fun `an unknown invoice is refused rather than validated as an empty one`() {
        logon(TEST_FINANCE_USER)

        // As on the PDF endpoints: these answer *about* an invoice, so "there is none" and "you may not see
        // it" are the same answer — see `checkEInvoiceReadAccess`.
        assertThrows<AccessException> { outgoingInvoiceEntityRest.validateEInvoice(-1L) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.exportXRechnung(-1L) }
    }

    /** A seller complete enough for [EInvoiceSellerConfig.isConfigured], with one bank account to select. */
    private fun configureSeller() {
        sellerConfig.name = "ProjectForge Test GmbH"
        sellerConfig.street = "Teststraße 1"
        sellerConfig.zip = "34117"
        sellerConfig.city = "Kassel"
        sellerConfig.vatId = "DE123456789"
        sellerConfig.bankAccounts = mutableListOf(
            BankAccountConfig().also {
                it.name = "Test account"
                it.iban = IBAN
                it.bic = "TESTDEFFXXX"
            },
        )
    }

    /**
     * An invoice with one position.
     *
     * @param planned `GEPLANT`, which spends no invoice number and is therefore the case where an e-invoice
     * cannot be built. A non-planned one gets the next number and everything an export needs: the bank account
     * configured above, and an address for the buyer.
     */
    private fun insertInvoice(subject: String, planned: Boolean): Long {
        val invoice = RechnungDO()
        invoice.status = if (planned) RechnungStatus.GEPLANT else RechnungStatus.GESTELLT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.datum = LocalDate.of(2026, 3, 2)
        invoice.betreff = subject
        invoice.kundeText = "$subject customer"
        if (!planned) {
            invoice.nummer = rechnungDao.nextNumber
            invoice.sellerBankAccount = IBAN
            invoice.customerAddress = "Kundenstraße 2"
            invoice.customerZipCode = "34117"
            invoice.customerCity = "Kassel"
        }
        invoice.positionen = mutableListOf(
            RechnungsPositionDO().also { pos ->
                pos.rechnung = invoice
                pos.number = 1.toShort()
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("1000.00")
                pos.vat = BigDecimal("0.19")
            },
        )
        return rechnungDao.insert(invoice)!!
    }

    companion object {
        /** Module directory this test runs in — `initJCRTestRepo` insists on it (see `TestUtils`). */
        private const val MODULE_NAME = "projectforge-rest"
        private const val IBAN = "DE02120300000000202051"
    }
}
