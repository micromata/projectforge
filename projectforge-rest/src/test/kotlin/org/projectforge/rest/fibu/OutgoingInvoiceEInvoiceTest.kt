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
import org.projectforge.framework.i18n.translate
import org.projectforge.jcr.RepoService
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Rechnung
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The e-invoice of one outgoing invoice — the endpoints of [OutgoingInvoiceEntityRest] behind the form's
 * e-invoice section, which Wicket has as its `EInvoiceModalDialog` (`RechnungEditForm`).
 *
 * The XML itself is `EInvoiceExportService`'s and Mustang's business. What belongs here is what the endpoints
 * decide: that the validation is *readable before* the export, so the form can name what is missing instead of
 * showing a failed download (Wicket's error line does the same); that an invoice which isn't ready is refused
 * with that very list rather than with the `IllegalStateException` the service throws; that the section can
 * write the form it sits in, since everything else here works on the stored invoice; and that all of them
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

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

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
            validation.errors.contains(eInvoiceError("sellerNotConfigured")),
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
        assertTrue(validation.errors.contains(eInvoiceError("numberMissing")), "${validation.errors}")
        assertTrue(validation.errors.contains(eInvoiceError("bankAccountNotSelected")), "${validation.errors}")
        assertTrue(validation.errors.contains(eInvoiceError("customerAddressMissing")), "${validation.errors}")
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
            assertTrue(body.contains(eInvoiceError("numberMissing")), "The answer is what `validate` said: $body")
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
    fun `saving for the check writes the form and answers what the checklist is asked about afterwards`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("E-invoice save", planned = false)
        val dto = outgoingInvoiceEntityRest.transformFromDB(rechnungDao.find(id)!!, editMode = true)
        // What the user came here to correct — an address the e-invoice needs and the invoice lacked.
        dto.customerAddress = "Neue Kundenstraße 7"

        val response = post(dto)

        assertEquals(HttpStatus.OK, response.statusCode, "${response.body?.validationErrors}")
        // The point of the endpoint: the *stored* invoice carries it now, because that is the one every other
        // e-invoice endpoint works on.
        assertEquals("Neue Kundenstraße 7", rechnungDao.find(id)!!.customerAddress)
    }

    @Test
    fun `an invoice the save itself refuses comes back with the field errors, not with a saved invoice`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("E-invoice save refusal", planned = false)
        val dto = outgoingInvoiceEntityRest.transformFromDB(rechnungDao.find(id)!!, editMode = true)
        dto.betreff = "Not stored"
        // A rule of `OutgoingInvoiceEntityRest.validate`, i.e. one only the DTO validation catches — the check
        // this endpoint would skip if it ran `validate(dbObj)` alone.
        dto.periodOfPerformanceBegin = LocalDate.of(2026, 8, 18)
        dto.periodOfPerformanceEnd = LocalDate.of(2026, 8, 1)

        val response = post(dto)

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        val errors = response.body?.validationErrors
        assertNotNull(errors)
        assertTrue(
            errors!!.any { it.fieldId == "periodOfPerformanceEnd" },
            "Anchored at the field, so the form can show it there: $errors",
        )
        assertEquals("E-invoice save refusal", rechnungDao.find(id)!!.betreff, "Nothing was written.")
    }

    @Test
    fun `a user outside the finance and orga groups reaches none of the endpoints`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("E-invoice access", planned = true)
        val dto = outgoingInvoiceEntityRest.transformFromDB(rechnungDao.find(id)!!, editMode = true)

        logon(TEST_USER)
        assertThrows<AccessException> { outgoingInvoiceEntityRest.validateEInvoice(id) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.exportXRechnung(id) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.exportZugferd(id) }
        // The save is one of the e-invoice functions and guarded by the same groups, although it writes what
        // the regular save writes: the button that triggers it lives in the e-invoice section.
        assertThrows<AccessException> { post(dto) }
    }

    /**
     * The invoice posted to [OutgoingInvoiceEntityRest.saveAndCheckEInvoice].
     *
     * The CSRF token comes from [SessionCsrfService.createServerData] rather than from a literal: it is the
     * session's, and the endpoint validates it exactly as the regular save does.
     */
    private fun post(dto: Rechnung): ResponseEntity<ResponseAction> {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val postData = PostData(
            data = dto,
            watchFieldsTriggered = null,
            serverData = sessionCsrfService.createServerData(request),
        )
        return outgoingInvoiceEntityRest.saveAndCheckEInvoice(request, postData)
    }

    @Test
    fun `an unknown invoice is refused rather than validated as an empty one`() {
        logon(TEST_FINANCE_USER)

        // As on the PDF endpoints: these answer *about* an invoice, so "there is none" and "you may not see
        // it" are the same answer — see `checkEInvoiceReadAccess`.
        assertThrows<AccessException> { outgoingInvoiceEntityRest.validateEInvoice(-1L) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.exportXRechnung(-1L) }
    }

    /**
     * The sentence `EInvoiceExportService.validate` answers for one of its error keys.
     *
     * Compared to the translated text and not to an English substring: the list travels to the user in the
     * user's language, so an assertion on English prose would hold for an English account only.
     */
    private fun eInvoiceError(key: String): String = translate("fibu.rechnung.eInvoice.error.$key")

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
        // Required as soon as a position refers to it, which the position below does (it declares no period of
        // its own) — see `PeriodOfPerformanceValidator`. Without it the invoice could be inserted but not
        // posted back through `saveAndCheckEInvoice`.
        invoice.periodOfPerformanceBegin = LocalDate.of(2026, 2, 1)
        invoice.periodOfPerformanceEnd = LocalDate.of(2026, 2, 28)
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
