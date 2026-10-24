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
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import java.math.BigDecimal
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RechnungCalculatorTest : AbstractTestBase() {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    @Test
    fun `test calculation of RechnungDO`() {
        RechnungDO().also { invoice ->
            invoice.addPosition(createPositionInfo("2", "10", null, "5", "10"))
            invoice.addPosition(createPositionInfo("1", "90", "0.19", "5", "10", "5"))
            var info = calculateAndAssert(invoice, net = "110.00", gross = "127.10")
            assertEquals(BigDecimal("35.00"), info.kostZuweisungenNetSum, "kostZuweisungenNetSum")
            assertEquals(BigDecimal("75.00"), info.kostZuweisungenFehlbetrag, "kostZuweisungenFehlbetrag")

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
            assertEquals(BigDecimal("127.10"), info.grossSum, "grossSumWithDiscount")
            assertEquals(BigDecimal("124.56"), info.grossSumWithDiscount, "grossSumWithDiscount")
            assertEquals(future5Days, info.faelligkeitOrDiscountMaturity, "faelligkeitOrDiscountMaturity")
        }
    }

    @Test
    fun `a creditor invoice is marked as overdue 2 working days before its due date`() {
        val today = PFDay.today()
        fun creditorInvoice(dueDate: LocalDate) = EingangsrechnungDO().also {
            it.addPosition(EingangsrechnungsPositionDO().also { pos ->
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("100")
            })
            it.faelligkeit = dueDate
        }
        val inTwoWorkingDays = PFDayUtils.addWorkingDays(today, 2).localDate
        val inThreeWorkingDays = PFDayUtils.addWorkingDays(today, 3).localDate
        RechnungCalculator.calculate(creditorInvoice(inTwoWorkingDays), useCaches = false).also {
            assertTrue(it.isMarkedOverdue, "due in 2 working days")
            assertFalse(it.isUeberfaellig, "not yet overdue: filters and sums unchanged")
        }
        assertFalse(RechnungCalculator.calculate(creditorInvoice(inThreeWorkingDays), useCaches = false).isMarkedOverdue)
        assertTrue(RechnungCalculator.calculate(creditorInvoice(today.localDate.minusDays(1)), useCaches = false).isMarkedOverdue)
        creditorInvoice(inTwoWorkingDays).also {
            it.bezahlDatum = today.localDate
            it.zahlBetrag = BigDecimal("100")
            assertFalse(RechnungCalculator.calculate(it, useCaches = false).isMarkedOverdue, "paid")
        }
        // An outgoing invoice is marked only when overdue.
        RechnungDO().also {
            it.addPosition(createPositionInfo("1", "100"))
            it.faelligkeit = inTwoWorkingDays
            assertFalse(RechnungCalculator.calculate(it, useCaches = false).isMarkedOverdue)
        }
    }

    @Test
    fun `due within working days skips weekends and holidays`() {
        fun info(dueDate: LocalDate?) = RechnungInfo(EingangsrechnungDO().also { it.faelligkeit = dueDate })
        // Monday, March 8th 2032: 2 working days before is Thursday.
        val monday = LocalDate.of(2032, 3, 8)
        assertTrue(RechnungCalculator.isDueWithinWorkingDays(info(monday), 2, LocalDate.of(2032, 3, 4)))
        assertFalse(RechnungCalculator.isDueWithinWorkingDays(info(monday), 2, LocalDate.of(2032, 3, 3)))
        assertTrue(RechnungCalculator.isDueWithinWorkingDays(info(monday), 2, LocalDate.of(2032, 3, 20)), "overdue")
        assertFalse(RechnungCalculator.isDueWithinWorkingDays(info(monday), 0, LocalDate.of(2032, 3, 5)))
        assertTrue(RechnungCalculator.isDueWithinWorkingDays(info(monday), 0, monday))
        assertTrue(RechnungCalculator.isDueWithinWorkingDays(info(monday), 5, LocalDate.of(2032, 3, 1)))
        // Tuesday after Easter 2032: Easter Monday and Good Friday are holidays, so 2 working days before is Wednesday.
        val afterEaster = LocalDate.of(2032, 3, 30)
        assertTrue(RechnungCalculator.isDueWithinWorkingDays(info(afterEaster), 2, LocalDate.of(2032, 3, 24)))
        assertFalse(RechnungCalculator.isDueWithinWorkingDays(info(afterEaster), 2, LocalDate.of(2032, 3, 23)))
        assertFalse(RechnungCalculator.isDueWithinWorkingDays(info(null), 2, monday), "no due date")
    }

    @Test
    fun `a cancelled invoice and its cancellation count as paid and in no sum`() {
        // Set as RechnungDao.buildStatistik does.
        AbstractRechnungsStatistik.configurationService = configurationService
        AbstractRechnungsStatistik.currencyConversionService = currencyConversionService
        RechnungDO().also { invoice ->
            invoice.addPosition(createPositionInfo("1", "100", "0.19"))
            invoice.faelligkeit = LocalDate.now().minusDays(1)
            invoice.status = RechnungStatus.STORNIERT
            val info = RechnungCalculator.calculate(invoice)
            assertTrue(info.isBezahlt, "cancelled")
            assertFalse(info.isUeberfaellig, "cancelled")
            val statistics = RechnungsStatistik()
            statistics.add(info)
            assertEquals(0, BigDecimal.ZERO.compareTo(statistics.brutto), "brutto")
            assertEquals(0, BigDecimal.ZERO.compareTo(statistics.netto), "netto")
            assertEquals(0, statistics.counter, "counter")
            assertEquals(0, BigDecimal.ZERO.compareTo(statistics.offen), "offen")
            assertEquals(0, BigDecimal.ZERO.compareTo(statistics.ueberfaellig), "ueberfaellig")
        }
        RechnungDO().also { invoice ->
            invoice.typ = RechnungTyp.CANCELLATION
            invoice.status = RechnungStatus.GESTELLT
            invoice.addPosition(createPositionInfo("-1", "100", "0.19"))
            val info = RechnungCalculator.calculate(invoice)
            assertTrue(info.isBezahlt, "cancellation")
            val statistics = RechnungsStatistik()
            statistics.add(info)
            assertEquals(0, BigDecimal.ZERO.compareTo(statistics.brutto), "brutto")
            assertEquals(0, BigDecimal.ZERO.compareTo(statistics.offen), "offen")
        }
    }

    @Test
    fun `test rounding`() {
        RechnungDO().also { invoice ->
            invoice.addPosition(createPositionInfo("0.75", "118.75", "0.19", "79.06")) // -10.00
            invoice.addPosition(createPositionInfo("2.5", "118.75", "0.19", "296.88"))
            invoice.addPosition(createPositionInfo("2.5", "118.75", "0.19", "296.88"))
            invoice.addPosition(createPositionInfo("3.5", "118.75", "0.19", "415.62")) // -0.01
            invoice.addPosition(createPositionInfo("1", "118.75", "0.19", "118.75"))
            val info = calculateAndAssert(invoice, net = "1217.20", gross = "1448.47")
            assertEquals(BigDecimal("1207.19"), info.kostZuweisungenNetSum, "kostZuweisungenNetSum")
            assertEquals(BigDecimal("10.01"), info.kostZuweisungenFehlbetrag, "kostZuweisungenFehlbetrag")
        }
    }

    @Test
    fun `test calculation of RechnungPositionDO`() {
        RechnungsPositionDO().also { position ->
            calculateAndAssert(position, "0.00", gross = "0.00")
            position.apply {
                menge = BigDecimal("0.75")
                einzelNetto = BigDecimal("17.85")
                vat = BigDecimal("0.19")
            }
            calculateAndAssert(position, net = "13.39", gross = "15.93")
        }
        createPositionInfo("2", "10", null).also {
            calculateAndAssert(it, net = "20.00", gross = "20.00")
        }
        createPositionInfo("2", "10", null, "5", "10").also {
            val info = calculateAndAssert(it, net = "20.00", gross = "20.00")
            assertEquals(BigDecimal("15.00"), info.kostZuweisungNetSum)
            assertEquals(BigDecimal("15.00"), info.kostZuweisungGrossSum)
            assertEquals(BigDecimal("-5.00"), info.kostZuweisungNetFehlbetrag)
        }
        createPositionInfo("2", "10.00", "0.19", "5", "10").also {
            val info = calculateAndAssert(it, net = "20.00", gross = "23.80")
            assertEquals(BigDecimal("15.00"), info.kostZuweisungNetSum, "kostZuweisungNetSum")
            assertEquals(BigDecimal("17.85"), info.kostZuweisungGrossSum, "kostZuweisungGrossSum")
            assertEquals(BigDecimal("-5.00"), info.kostZuweisungNetFehlbetrag, "kostZuweisungNetFehlbetrag")
        }
    }

    /**
     * The assigned order position is named by its id even when [AuftragsCache] cannot answer anything about it.
     * That is the situation of every position while `RechnungCache.refresh` runs: it is called from the refresh of
     * `AuftragsCache` (through `AuftragsRechnungCache`), so that cache answers nothing at all as long as its own
     * refresh is underway - a nested refresh returns immediately. The id stayed null in the cached
     * [RechnungPosInfo] then, and the invoice list, which resolves the orders of an invoice from it
     * (`Rechnung.copyFrom4ListRow`), showed no order for any invoice.
     */
    @Test
    fun `an assigned order position is named although the cache knows nothing about it`() {
        val position = createPositionInfo("1", "100").also {
            // An id no order position of the test database has, so AuftragsCache answers null for it - as it does
            // for every position while it is refreshing itself.
            it.auftragsPosition = AuftragsPositionDO().also { orderPos -> orderPos.id = 4711424242L }
        }
        val posInfo = calculateAndAssert(position, net = "100.00", gross = "100.00")
        assertEquals(4711424242L, posInfo.auftragsPositionId, "auftragsPositionId")
        // The order behind it is the cache's answer and stays open until it has been read.
        assertNull(posInfo.auftragsId, "auftragsId")
        assertNull(posInfo.auftragsPositionNummer, "auftragsPositionNummer")
    }

    private fun calculateAndAssert(
        invoice: AbstractRechnungDO,
        net: String,
        gross: String
    ): RechnungInfo {
        val info = RechnungCalculator.calculate(invoice)
        assertEquals(BigDecimal(net), info.netSum, "netSum")
        assertEquals(BigDecimal(gross), info.grossSum, "grossSum")
        return info
    }

    private fun calculateAndAssert(
        position: AbstractRechnungsPositionDO,
        net: String,
        gross: String
    ): RechnungPosInfo {
        val rechnung = RechnungDO()
        val info = RechnungInfo(rechnung)
        val posInfo = RechnungPosInfo(info, position)
        RechnungCalculator.calculate(posInfo, position)
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
