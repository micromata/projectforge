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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.Constants
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.files.AttachmentsService
import org.projectforge.files.store.FileStore
import java.math.BigDecimal
import java.time.LocalDate

class EInvoiceExportServiceTest {

    companion object {
        /**
         * [EInvoiceExportService.validate] answers translated sentences, and this test runs without Spring:
         * without the bundle every key would resolve to itself, so the assertions below would compare keys
         * against keys and pass for a service that translates nothing.
         */
        @JvmStatic
        @BeforeAll
        fun registerBundle() {
            I18nHelper.addBundleName(Constants.RESOURCE_BUNDLE_NAME)
        }
    }

    private val invoiceServiceMock: InvoiceService = Mockito.mock(InvoiceService::class.java)
    private val attachmentsServiceMock: AttachmentsService = Mockito.mock(AttachmentsService::class.java)
    private val fileStoreMock: FileStore = Mockito.mock(FileStore::class.java)
    private val rechnungDaoMock: RechnungDao = Mockito.mock(RechnungDao::class.java)


    private fun createSellerConfig(): EInvoiceSellerConfig {
        return EInvoiceSellerConfig().apply {
            name = "Example Seller GmbH"
            street = "Musterstraße 1-5"
            zip = "12345"
            city = "Musterstadt"
            country = "DE"
            vatId = "DE123456789"
            taxNumber = "026/123/45678"
            email = "info@example.org"
            phone = "+49 123 456789-0"
            bankAccounts = mutableListOf(
                BankAccountConfig().apply {
                    name = "Commerzbank"
                    iban = "DE89370400440532013000"
                    bic = "COBADEFFXXX"
                },
                BankAccountConfig().apply {
                    name = "Sparkasse"
                    iban = "DE02500105170137075030"
                    bic = "INGDDEFFXXX"
                }
            )
        }
    }

    private fun createTestInvoice(): RechnungDO {
        val konto = KontoDO().apply {
            nummer = 10000
            bezeichnung = "Testkundin GmbH"
            contactPerson = "Max Mustermann"
            street = "Musterstraße 42"
            zipCode = "12345"
            city = "Berlin"
            country = "DE"
            vatId = "DE987654321"
            leitwegId = "04011000-1234512345-12"
            eInvoiceEmail = "rechnung@testkundin.de"
        }
        val kunde = KundeDO().apply {
            nummer = 1L
            name = "Testkundin GmbH"
            this.konto = konto
        }

        val pos1 = RechnungsPositionDO().apply {
            number = 1
            text = "Softwareentwicklung"
            menge = BigDecimal("10")
            einzelNetto = BigDecimal("150.00")
            vat = BigDecimal("0.19")
        }

        val pos2 = RechnungsPositionDO().apply {
            number = 2
            text = "Projektmanagement"
            menge = BigDecimal("5")
            einzelNetto = BigDecimal("120.00")
            vat = BigDecimal("0.19")
        }

        return RechnungDO().apply {
            nummer = 2024001
            datum = LocalDate.of(2024, 6, 15)
            faelligkeit = LocalDate.of(2024, 7, 15)
            typ = RechnungTyp.RECHNUNG
            this.kunde = kunde
            customerref1 = "PO-2024-42"
            periodOfPerformanceBegin = LocalDate.of(2024, 5, 1)
            periodOfPerformanceEnd = LocalDate.of(2024, 5, 31)
            sellerBankAccount = "DE89370400440532013000"
            positionen = mutableListOf(pos1, pos2)
        }
    }

    @Test
    fun exportAsXRechnung() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice()

        val xml = service.exportAsXRechnung(invoice)

        assertNotNull(xml)
        assertTrue(xml.isNotEmpty())

        val xmlString = String(xml, Charsets.UTF_8)
        assertTrue(xmlString.contains("<?xml"), "Should be valid XML")
        assertTrue(xmlString.contains("2024001"), "Should contain invoice number")
        assertTrue(xmlString.contains("Example Seller GmbH"), "Should contain seller name")
        assertTrue(xmlString.contains("Testkundin GmbH"), "Should contain buyer name")
        assertTrue(xmlString.contains("Softwareentwicklung"), "Should contain position text")
        assertTrue(xmlString.contains("DE123456789"), "Should contain seller VAT ID")
        assertTrue(xmlString.contains("04011000-1234512345-12"), "Should contain Leitweg-ID")
    }

    @Test
    fun exportCreditNote() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply {
            typ = RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN
        }

        val xml = service.exportAsXRechnung(invoice)
        val xmlString = String(xml, Charsets.UTF_8)
        assertTrue(xmlString.contains("381"), "Credit note should have document type code 381")
    }

    /**
     * The cancellation of [createTestInvoice]: no number of its own, the original referenced, the unit prices
     * negated - as `OutgoingInvoiceEntityRest.prepareCancellation` builds it.
     */
    private fun createTestCancellation(): RechnungDO {
        val original = createTestInvoice().apply { id = 42L }
        return createTestInvoice().apply {
            nummer = null
            typ = RechnungTyp.CANCELLATION
            datum = LocalDate.of(2024, 7, 1)
            originalRechnung = original
            positionen!!.forEach { it.einzelNetto = it.einzelNetto!!.negate() }
        }
    }

    @Test
    fun exportBuyerReferencesOfTheCustomer() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply {
            customerOrderNumber = "4500123456"
            customerCostCenter = "KST-4711"
            customerContactPerson = "Erika Muster"
            customerContactDepartment = "Einkauf"
            customerEInvoiceEmail = "einkauf@testkundin.de"
        }

        val xmlString = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)

        // BT-13, independent of a Leitweg-ID: the customer reference stays what it is (BT-10 fallback).
        val order = xmlString.substringAfter("<ram:BuyerOrderReferencedDocument>").substringBefore("</ram:BuyerOrderReferencedDocument>")
        assertTrue(order.contains("<ram:IssuerAssignedID>4500123456</ram:IssuerAssignedID>"), "BT-13: $order")
        // BT-19 in the settlement header, as its last child.
        val settlement = xmlString.substringAfter("<ram:ApplicableHeaderTradeSettlement>")
            .substringBefore("</ram:ApplicableHeaderTradeSettlement>").replace(Regex("\\s+"), "")
        assertTrue(
            settlement.endsWith("<ram:ReceivableSpecifiedTradeAccountingAccount><ram:ID>KST-4711</ram:ID></ram:ReceivableSpecifiedTradeAccountingAccount>"),
            "BT-19: $settlement",
        )
        // BT-56 and BT-58 in the contact of the buyer.
        val buyer = xmlString.substringAfter("<ram:BuyerTradeParty>").substringBefore("</ram:BuyerTradeParty>")
        assertTrue(buyer.contains("<ram:PersonName>Erika Muster, Einkauf</ram:PersonName>"), "BT-56: $buyer")
        assertTrue(buyer.contains("<ram:URIID>einkauf@testkundin.de</ram:URIID>"), "BT-58: $buyer")
    }

    @Test
    fun exportWithoutBuyerReferences() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)

        // No customer reference either: with the fixture's Leitweg-ID it would be sent as BT-13.
        val invoice = createTestInvoice().apply { customerref1 = null }
        val xmlString = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)

        assertTrue(!xmlString.contains("BuyerOrderReferencedDocument"), "No order number, no BT-13")
        assertTrue(!xmlString.contains("ReceivableSpecifiedTradeAccountingAccount"), "No cost center, no BT-19")
    }

    @Test
    fun exportBuyerReferencesFallBackForAnInvoiceWithoutTheNewFields() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        // An invoice from before the fields: the cost center only on the account, the order number only as
        // the customer reference of a customer with a Leitweg-ID (the fixture's account has one).
        val invoice = createTestInvoice().apply {
            customerref1 = "PO-0815"
            kunde!!.konto!!.costCenter = "KST-0042"
        }

        val xmlString = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)

        val order = xmlString.substringAfter("<ram:BuyerOrderReferencedDocument>").substringBefore("</ram:BuyerOrderReferencedDocument>")
        assertTrue(order.contains("<ram:IssuerAssignedID>PO-0815</ram:IssuerAssignedID>"), "BT-13 from customerref1: $order")
        assertTrue(xmlString.contains("<ram:ID>KST-0042</ram:ID>"), "BT-19 from the account")

        // The order number wins over the customer reference.
        invoice.customerOrderNumber = "4500123456"
        val withOrderNumber = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)
            .substringAfter("<ram:BuyerOrderReferencedDocument>").substringBefore("</ram:BuyerOrderReferencedDocument>")
        assertTrue(withOrderNumber.contains("<ram:IssuerAssignedID>4500123456</ram:IssuerAssignedID>"), "BT-13: $withOrderNumber")

        // Without a Leitweg-ID the customer reference is BT-10 and no order reference.
        invoice.customerOrderNumber = null
        invoice.kunde!!.konto!!.leitwegId = null
        val withoutLeitwegId = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)
        assertTrue(!withoutLeitwegId.contains("BuyerOrderReferencedDocument"), "No BT-13 without a Leitweg-ID")
    }

    @Test
    fun exportCancellation() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestCancellation()
        assertEquals("2024001-S", invoice.belegNummer)
        assertTrue(service.validate(invoice).isEmpty(), "A complete cancellation is exportable: ${service.validate(invoice)}")

        val xmlString = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)
        assertTrue(xmlString.contains("<ram:TypeCode>381</ram:TypeCode>"), "Cancellation is exported as credit note (381)")
        assertTrue(!xmlString.contains("<ram:TypeCode>457</ram:TypeCode>"), "457 is no XRechnung type code")
        assertTrue(xmlString.contains("<ram:ID>2024001-S</ram:ID>"), "BT-1 is the derived number")
        val reference = xmlString.substringAfter("<ram:InvoiceReferencedDocument>", "")
            .substringBefore("</ram:InvoiceReferencedDocument>")
        assertTrue(reference.contains("<ram:IssuerAssignedID>2024001</ram:IssuerAssignedID>"), "BT-25 references the original")
        assertTrue(reference.contains("20240615"), "BT-26 is the date of the original")
        // 10 * 150 + 5 * 120, positive as a credit note states it.
        assertTrue(xmlString.contains(">2100.00<"), "Amounts are positive")
        assertTrue(!xmlString.contains(">-"), "No negative amount in the credit note")
        assertEquals("XRechnung_2024001-S.xml", service.getExportFilename(invoice))
    }

    @Test
    fun validateCancellation() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val withoutOriginal = createTestCancellation().apply { originalRechnung = null }
        val errors = service.validate(withoutOriginal)
        assertTrue(errors.contains(eInvoiceError("cancellationOriginalMissing")), "Should report missing original")
        assertTrue(!errors.contains(eInvoiceError("numberMissing")), "A cancellation has no number of its own")

        val positive = createTestCancellation().apply {
            positionen!!.forEach { it.einzelNetto = it.einzelNetto!!.negate() }
        }
        assertTrue(service.validate(positive).contains(eInvoiceError("cancellationNotNegative")), "Should report positive sum")
    }

    @Test
    fun validateMissingFields() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)

        val invoice = RechnungDO().apply {
            nummer = null
            datum = null
            positionen = null
            kunde = null
        }

        val errors = service.validate(invoice)
        // Compared to the translated texts and not to English substrings: the sentences are the user's
        // language, so a substring of the English bundle would only ever hold for an English account.
        assertTrue(errors.contains(eInvoiceError("numberMissing")), "Should report missing invoice number")
        assertTrue(errors.contains(eInvoiceError("dateMissing")), "Should report missing date")
        assertTrue(errors.contains(eInvoiceError("noPositions")), "Should report missing positions")
        assertTrue(errors.contains(eInvoiceError("customerNameMissing")), "Should report missing customer")
        // The accounts *are* configured here, the invoice just names none of them.
        assertTrue(errors.contains(eInvoiceError("bankAccountNotSelected")), "Should report missing bank account")
    }

    @Test
    fun validateIncompleteCustomerAddress() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)

        val invoice = createTestInvoice().apply {
            kunde = KundeDO().apply {
                nummer = 1L
                name = "Test GmbH"
                konto = KontoDO().apply {
                    nummer = 10001
                    bezeichnung = "Test"
                    street = null
                    zipCode = null
                    city = null
                }
            }
        }

        val errors = service.validate(invoice)
        assertTrue(errors.contains(eInvoiceError("customerAddressMissing")), "Should report incomplete address")
    }

    @Test
    fun validateUnconfiguredSeller() {
        val service = EInvoiceExportService(EInvoiceSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)

        val invoice = createTestInvoice().apply { sellerBankAccount = null }
        val errors = service.validate(invoice)
        assertTrue(errors.contains(eInvoiceError("sellerNotConfigured")), "Should report unconfigured seller")
    }

    /** The sentence [EInvoiceExportService.validate] answers for one of its error keys. */
    private fun eInvoiceError(key: String): String = translate("fibu.rechnung.eInvoice.error.$key")

    @Test
    fun getExportFilename() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice()

        assertEquals("XRechnung_2024001.xml", service.getExportFilename(invoice))
    }

    @Test
    fun getExportFilenameDraft() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply { nummer = null }

        assertEquals("XRechnung_draft.xml", service.getExportFilename(invoice))
    }

    @Test
    fun exportWithSkonto() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply {
            discountPercent = BigDecimal("2")
            discountMaturity = LocalDate.of(2024, 6, 25)
        }

        val xml = service.exportAsXRechnung(invoice)
        val xmlString = String(xml, Charsets.UTF_8)
        assertTrue(xmlString.contains("Skonto"), "Should contain Skonto payment terms")
    }

    @Test
    fun exportWithZeroVat() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply {
            positionen = mutableListOf(
                RechnungsPositionDO().apply {
                    number = 1
                    text = "Steuerfreie Leistung"
                    menge = BigDecimal.ONE
                    einzelNetto = BigDecimal("1000.00")
                    vat = BigDecimal.ZERO
                }
            )
        }

        val xml = service.exportAsXRechnung(invoice)
        assertNotNull(xml)
        assertTrue(xml.isNotEmpty())
    }

    /**
     * A deleted position is no line of the e-invoice: it is not part of any sum of the invoice
     * ([RechnungCalculator] skips it), so a line for it would state an amount the totals don't contain.
     */
    @Test
    fun exportSkipsDeletedPositions() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice()
        invoice.positionen!![1].deleted = true

        val xmlString = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)
        assertTrue(xmlString.contains("Softwareentwicklung"), "The remaining position is a line of the e-invoice")
        assertFalse(xmlString.contains("Projektmanagement"), "The deleted position is not")
    }

    /** An invoice whose only position was deleted has nothing to state, and says so instead of exporting. */
    @Test
    fun validateAllPositionsDeleted() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice()
        invoice.positionen!!.forEach { it.deleted = true }

        assertTrue(service.validate(invoice).contains(eInvoiceError("noPositions")), "Should report missing positions")
    }

    /**
     * An e-invoice whose amount due doesn't match its lines is read anyway, and the mismatch is reported: mustang
     * rejects such an invoice by default, so the checker showed "no e-invoice data" instead of the actual error.
     */
    @Test
    fun readInvoiceWithWrongTotal() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val xmlString = String(service.exportAsXRechnung(createTestInvoice()), Charsets.UTF_8)
        val readService = EInvoiceReadService()

        val valid = readService.parseFile("invoice.xml", xmlString.toByteArray()).invoiceData
        assertEquals("2024001", valid.invoiceNumber)
        assertTrue(valid.validationErrors.none { it.startsWith("[BR-CO-16]") }, "Totals match: ${valid.validationErrors}")

        val duePayable = Regex("<ram:DuePayableAmount>([^<]*)</ram:DuePayableAmount>")
        val amount = duePayable.find(xmlString)!!.groupValues[1]
        val wrong = BigDecimal(amount).add(BigDecimal("0.15")).toPlainString()
        val tampered = xmlString.replace(duePayable, "<ram:DuePayableAmount>$wrong</ram:DuePayableAmount>")
        val invalid = readService.parseFile("invoice.xml", tampered.toByteArray()).invoiceData
        assertEquals("2024001", invalid.invoiceNumber, "The invoice is read despite the wrong total")
        assertTrue(
            invalid.validationErrors.any { it.startsWith("[BR-CO-16]") && it.contains(wrong) && it.contains(amount) },
            "Wrong total is reported: ${invalid.validationErrors}",
        )
    }

    /**
     * The positions of an invoice whose PDF stated another gross sum than its XML: the vat rounded per position
     * sums up to 2085.21, the vat of the net sum is 2085.20. The XML and [RechnungCalculator] (PDF, UI) agree.
     */
    @Test
    fun `sums of the e-invoice are the sums of the invoice`() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply {
            positionen = mutableListOf(
                position(1, "2.50", "168.35", "0.19"),
                position(2, "15.25", "149.70", "0.19"),
                position(3, "33.75", "149.70", "0.19"),
                position(4, "9.75", "149.70", "0.19"),
                position(5, "11.75", "149.70", "0.19"),
            )
        }
        val xml = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)
        assertEquals("10974.75", xmlAmount(xml, "TaxBasisTotalAmount"))
        assertEquals("2085.20", xmlAmount(xml, "TaxTotalAmount"))
        assertEquals("13059.95", xmlAmount(xml, "GrandTotalAmount"))
        val info = RechnungCalculator.calculate(invoice, useCaches = false)
        assertEquals(BigDecimal("10974.75"), info.netSum)
        assertEquals(BigDecimal("2085.20"), info.vatAmount)
        assertEquals(BigDecimal("13059.95"), info.grossSum)
    }

    /**
     * Fractional quantities and unit prices, two vat rates and a cancellation: the export checks its sums against
     * [RechnungCalculator] and refuses the invoice if they differ, so it succeeding is the assertion.
     */
    @Test
    fun `sums of the e-invoice match for several vat rates and a cancellation`() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, fileStoreMock, rechnungDaoMock)
        val invoice = createTestInvoice().apply {
            positionen = mutableListOf(
                position(1, "2.125", "149.70", "0.19"),
                position(2, "0.333", "99.99", "0.19"),
                position(3, "7", "12.345", "0.07"),
                position(4, "1.5", "33.33", "0.07"),
            )
        }
        val xml = String(service.exportAsXRechnung(invoice), Charsets.UTF_8)
        val info = RechnungCalculator.calculate(invoice, useCaches = false)
        assertEquals(info.grossSum.toPlainString(), xmlAmount(xml, "GrandTotalAmount"))
        assertEquals(info.netSum + info.vatAmount, info.grossSum)

        val cancellation = createTestCancellation().apply {
            positionen = invoice.positionen!!.map { pos ->
                position(pos.number.toInt(), pos.menge.toString(), pos.einzelNetto!!.negate().toString(), pos.vat.toString())
            }.toMutableList()
        }
        val cancellationXml = String(service.exportAsXRechnung(cancellation), Charsets.UTF_8)
        assertEquals(info.grossSum.toPlainString(), xmlAmount(cancellationXml, "GrandTotalAmount"))
    }

    private fun position(number: Int, menge: String, einzelNetto: String, vat: String) = RechnungsPositionDO().also {
        it.number = number.toShort()
        it.text = "Position $number"
        it.menge = BigDecimal(menge)
        it.einzelNetto = BigDecimal(einzelNetto)
        it.vat = BigDecimal(vat)
    }

    private fun xmlAmount(xml: String, element: String): String? {
        return Regex("<(?:\\w+:)?$element(?:\\s[^>]*)?>([^<]+)<").find(xml)?.groupValues?.get(1)
    }
}
