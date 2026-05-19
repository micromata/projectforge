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
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.RepoService
import java.math.BigDecimal
import java.time.LocalDate

class EInvoiceExportServiceTest {

    private val invoiceServiceMock: InvoiceService = Mockito.mock(InvoiceService::class.java)
    private val attachmentsServiceMock: AttachmentsService = Mockito.mock(AttachmentsService::class.java)
    private val repoServiceMock: RepoService = Mockito.mock(RepoService::class.java)


    private fun createSellerConfig(): EInvoiceSellerConfig {
        return EInvoiceSellerConfig().apply {
            name = "Micromata GmbH"
            street = "Marie-Calm-Straße 1-5"
            zip = "34131"
            city = "Kassel"
            country = "DE"
            vatId = "DE123456789"
            taxNumber = "026/123/45678"
            email = "info@micromata.de"
            phone = "+49 561 316 85 0"
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
            bezeichnung = "Testkundin"
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
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)
        val invoice = createTestInvoice()

        val xml = service.exportAsXRechnung(invoice)

        assertNotNull(xml)
        assertTrue(xml.isNotEmpty())

        val xmlString = String(xml, Charsets.UTF_8)
        assertTrue(xmlString.contains("<?xml"), "Should be valid XML")
        assertTrue(xmlString.contains("2024001"), "Should contain invoice number")
        assertTrue(xmlString.contains("Micromata GmbH"), "Should contain seller name")
        assertTrue(xmlString.contains("Testkundin GmbH"), "Should contain buyer name")
        assertTrue(xmlString.contains("Softwareentwicklung"), "Should contain position text")
        assertTrue(xmlString.contains("DE123456789"), "Should contain seller VAT ID")
        assertTrue(xmlString.contains("04011000-1234512345-12"), "Should contain Leitweg-ID")
    }

    @Test
    fun exportCreditNote() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)
        val invoice = createTestInvoice().apply {
            typ = RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN
        }

        val xml = service.exportAsXRechnung(invoice)
        val xmlString = String(xml, Charsets.UTF_8)
        assertTrue(xmlString.contains("381"), "Credit note should have document type code 381")
    }

    @Test
    fun validateMissingFields() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)

        val invoice = RechnungDO().apply {
            nummer = null
            datum = null
            positionen = null
            kunde = null
        }

        val errors = service.validate(invoice)
        assertTrue(errors.any { it.contains("number") }, "Should report missing invoice number")
        assertTrue(errors.any { it.contains("date") }, "Should report missing date")
        assertTrue(errors.any { it.contains("positions") }, "Should report missing positions")
        assertTrue(errors.any { it.contains("customer") }, "Should report missing customer")
        assertTrue(errors.any { it.contains("bank account") }, "Should report missing bank account")
    }

    @Test
    fun validateIncompleteCustomerAddress() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)

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
        assertTrue(errors.any { it.contains("address") }, "Should report incomplete address")
    }

    @Test
    fun validateUnconfiguredSeller() {
        val service = EInvoiceExportService(EInvoiceSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)

        val invoice = createTestInvoice().apply { sellerBankAccount = null }
        val errors = service.validate(invoice)
        assertTrue(errors.any { it.contains("Seller") }, "Should report unconfigured seller")
    }

    @Test
    fun getExportFilename() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)
        val invoice = createTestInvoice()

        assertEquals("XRechnung_2024001.xml", service.getExportFilename(invoice))
    }

    @Test
    fun getExportFilenameDraft() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)
        val invoice = createTestInvoice().apply { nummer = null }

        assertEquals("XRechnung_draft.xml", service.getExportFilename(invoice))
    }

    @Test
    fun exportWithSkonto() {
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)
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
        val service = EInvoiceExportService(createSellerConfig(), invoiceServiceMock, attachmentsServiceMock, repoServiceMock)
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
}
