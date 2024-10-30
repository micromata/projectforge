package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.KostZuweisungDO
import java.math.BigDecimal
import java.time.LocalDate

class RechnungCalculatorTest {
    @Test
    fun `test calculation of RechnungDO`() {
        RechnungDO().also { invoice ->
            invoice.addPosition(createPositionInfo("2", "10", null, "5", "10"))
            invoice.addPosition(createPositionInfo("1", "90", "0.19", "5", "10", "5"))
            var info = calculateAndAssert(invoice, net = "110", gross = "127.10")
            assertEquals(BigDecimal("35"), info.kostZuweisungenNetSum, "kostZuweisungenNetSum")
            assertEquals(BigDecimal("75"), info.kostZuweisungenFehlbetrag, "kostZuweisungenFehlbetrag")

            val future5Days = LocalDate.now().plusDays(5)
            val yesterday = LocalDate.now().minusDays(1)
            invoice.faelligkeit = LocalDate.now().plusDays(5)
            info = RechnungCalculator.calculate(invoice)
            assertFalse(info.isBezahlt)
            assertFalse(info.isUeberfaellig)
            assertEquals(future5Days, info.faelligkeitOrDiscountMaturity, "faelligkeitOrDiscountMaturity")
            invoice.faelligkeit = yesterday
            info = RechnungCalculator.calculate(invoice)
            assertFalse(info.isBezahlt)
            assertTrue(info.isUeberfaellig)
            assertEquals(yesterday, info.faelligkeitOrDiscountMaturity, "faelligkeitOrDiscountMaturity")

            invoice.bezahlDatum = LocalDate.now()
            info = RechnungCalculator.calculate(invoice)
            assertFalse(info.isBezahlt, "paid date given, but no amount paid")
            assertTrue(info.isUeberfaellig, "paid date given, but no amount paid")
            invoice.zahlBetrag = BigDecimal("120.00")
            info = RechnungCalculator.calculate(invoice)
            assertFalse(info.isBezahlt, "paid date and amount given, but not status paid")
            assertTrue(info.isUeberfaellig, "paid date and amount given, but not status paid")
            invoice.status = RechnungStatus.BEZAHLT
            info = RechnungCalculator.calculate(invoice)
            assertTrue(info.isBezahlt, "paid")
            assertFalse(info.isUeberfaellig, "paid, can't be overdued.")
            invoice.status = RechnungStatus.BEZAHLT

            invoice.bezahlDatum = null
            invoice.status = RechnungStatus.GESTELLT
            invoice.zahlBetrag = null
            invoice.discountPercent = BigDecimal("2")
            invoice.discountMaturity = future5Days
            info = RechnungCalculator.calculate(invoice)
            assertEquals(BigDecimal("124.56"), info.grossSumWithDiscount, "grossSumWithDiscount")
            assertEquals(future5Days, info.faelligkeitOrDiscountMaturity, "faelligkeitOrDiscountMaturity")
        }
    }

    @Test
    fun `test calculation of RechnungPositionDO`() {
        RechnungsPositionDO().also { position ->
            calculateAndAssert(position, "0", gross = "0")
            position.apply {
                menge = BigDecimal.TEN
                einzelNetto = BigDecimal("12")
                vat = BigDecimal("0.19")
            }
            calculateAndAssert(position, net = "120", gross = "142.80")
        }
        createPositionInfo("2", "10", null).also {
            calculateAndAssert(it, net = "20", gross = "20")
        }
        createPositionInfo("2", "10", null, "5", "10").also {
            val info = calculateAndAssert(it, net = "20", gross = "20")
            assertEquals(BigDecimal("15"), info.kostZuweisungNetSum)
            assertEquals(BigDecimal("15"), info.kostZuweisungGrossSum)
            assertEquals(BigDecimal("5"), info.kostZuweisungNetFehlbetrag)
        }
        createPositionInfo("2", "10", "0.19", "5", "10").also {
            val info = calculateAndAssert(it, net = "20", gross = "23.80")
            assertEquals(BigDecimal("15"), info.kostZuweisungNetSum, "kostZuweisungNetSum")
            assertEquals(BigDecimal("17.85"), info.kostZuweisungGrossSum, "kostZuweisungGrossSum")
            assertEquals(BigDecimal("5"), info.kostZuweisungNetFehlbetrag, "kostZuweisungNetFehlbetrag")
        }
    }

    private fun calculateAndAssert(
        invoice: AbstractRechnungDO,
        net: String,
        gross: String
    ): RechnungInfo {
        val info = RechnungCalculator.calculate(invoice)
        assertEquals(BigDecimal(net), info.netSum)
        assertEquals(BigDecimal(gross), info.grossSum)
        return info
    }

    private fun calculateAndAssert(
        position: AbstractRechnungsPositionDO,
        net: String,
        gross: String
    ): RechnungPosInfo {
        val posInfo = RechnungCalculator.calculate(position)
        assertEquals(BigDecimal(net), posInfo.netSum, "netSum")
        assertEquals(BigDecimal(gross), posInfo.grossSum, "grossSum")
        return posInfo
    }

    private fun createPositionInfo(
        menge: String,
        einzelNetto: String,
        vat: String? = null,
        vararg kostNets: String
    ): RechnungsPositionDO {
        return RechnungsPositionDO().also { pos ->
            pos.menge = BigDecimal(menge)
            pos.einzelNetto = BigDecimal(einzelNetto)
            if (vat != null)
                pos.vat = BigDecimal(vat)
            kostNets.forEach { net ->
                pos.addKostZuweisung(KostZuweisungDO().also {
                    it.netto = BigDecimal(net)
                })
            }
        }
    }

    private fun createInvoice(
        date: LocalDate,
        paidData: LocalDate? = null,
        dueDate: LocalDate? = null,
        discountPercent: String? = null,
        discountDate: LocalDate? = null
    ): RechnungDO {
        return RechnungDO().also {
            it.datum = date
            it.bezahlDatum = paidData
            it.faelligkeit = dueDate
            if (discountDate != null)
                it.discountPercent = BigDecimal(discountPercent)
            it.discountMaturity = discountDate
        }
    }
}
