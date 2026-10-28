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


package org.projectforge.business.testdata

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsPositionsArt
import org.projectforge.business.fibu.AuftragsPositionsPaymentType
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.PaymentType
import org.projectforge.business.fibu.PeriodOfPerformanceType
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Orders of the projects in all states, the invoices of the commissioned ones (fixed price at the end, time and
 * materials monthly) and monthly incoming invoices of the company's suppliers.
 */
@Service
open class OrderInvoiceTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    override val order = 50

    private class Pos(
        val title: String,
        val task: String,
        val net: Int,
        val personDays: Int?,
        val type: AuftragsPositionsPaymentType,
        val art: AuftragsPositionsArt,
        val begin: LocalDate,
        val end: LocalDate,
    )

    private class Order(
        val projekt: String,
        val titel: String,
        val status: AuftragsStatus,
        val offer: LocalDate,
        val decision: LocalDate?,
        val probability: Int?,
        val contact: String,
        val positions: List<Pos>,
    )

    override fun seed(ctx: TestDataContext) {
        orders(ctx).forEach { o -> insertOrder(ctx, o) }
        insertIncomingInvoices(ctx)
    }

    private fun insertOrder(ctx: TestDataContext, o: Order) {
        val projekt = ctx.projekt(o.projekt)
        val commissioned = o.status == AuftragsStatus.BEAUFTRAGT || o.status == AuftragsStatus.ABGESCHLOSSEN
        val auftrag = AuftragDO().also {
            it.nummer = auftragDao.getNextNumber(null)
            it.titel = o.titel
            it.status = o.status
            it.kunde = projekt.kunde
            it.projekt = projekt
            it.angebotsDatum = o.offer
            it.erfassungsDatum = o.offer
            it.entscheidungsDatum = o.decision
            it.beauftragungsDatum = if (commissioned) o.decision else null
            it.probabilityOfOccurrence = o.probability
            it.contactPerson = ctx.user(o.contact)
            it.periodOfPerformanceBegin = o.positions.minOf { p -> p.begin }
            it.periodOfPerformanceEnd = o.positions.maxOf { p -> p.end }
            it.positionen = mutableListOf()
        }
        o.positions.forEachIndexed { i, p ->
            auftrag.positionen!!.add(AuftragsPositionDO().also {
                it.number = (i + 1).toShort()
                it.auftrag = auftrag
                it.titel = p.title
                it.task = ctx.task(o.projekt, p.task)
                it.art = p.art
                it.paymentType = p.type
                it.status = o.status
                // Maintenance is given per month.
                it.nettoSumme = bd(if (p.art == AuftragsPositionsArt.WARTUNG) p.net * 12 else p.net)
                it.personDays = p.personDays?.let { pd -> bd(pd) }
                it.periodOfPerformanceType = PeriodOfPerformanceType.OWN
                it.periodOfPerformanceBegin = p.begin
                it.periodOfPerformanceEnd = p.end
                it.vollstaendigFakturiert = o.status == AuftragsStatus.ABGESCHLOSSEN
            })
        }
        auftragDao.insert(auftrag, checkAccess = false)
        if (!commissioned) {
            return
        }
        val invoicedUntil = ctx.month(2026, 9, 30)
        auftrag.positionen!!.forEach { pos ->
            val invoices = mutableListOf<Pair<LocalDate, BigDecimal>>()
            val begin = pos.periodOfPerformanceBegin!!
            val end = pos.periodOfPerformanceEnd!!
            if (pos.paymentType == AuftragsPositionsPaymentType.FESTPREISPAKET) {
                if (end.isBefore(ctx.today)) {
                    invoices.add(end.plusDays(3) to pos.nettoSumme!!)
                }
            } else {
                val monthCount = ChronoUnit.MONTHS.between(YearMonth.from(begin), YearMonth.from(end)) + 1
                val share = pos.nettoSumme!!.divide(bd(monthCount), 0, RoundingMode.HALF_UP)
                var ym = YearMonth.from(begin)
                while (!ym.atDay(1).isAfter(minOf(end, invoicedUntil))) {
                    val factor = bd(listOf(0.85, 0.95, 1.0, 1.05, 1.1)[(ym.monthValue + pos.number) % 5])
                    invoices.add(ym.plusMonths(1).atDay(3) to share.multiply(factor).setScale(2, RoundingMode.HALF_UP))
                    ym = ym.plusMonths(1)
                }
            }
            invoices.filter { it.first.isBefore(ctx.today) }.forEach { (date, amount) ->
                val period = YearMonth.from(date.minusMonths(1))
                val monthName = period.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                val paid = date.plusDays(40).isBefore(ctx.today)
                val rechnung = RechnungDO().also {
                    it.nummer = rechnungDao.getNextNumber(null)
                    it.datum = date
                    it.faelligkeit = date.plusDays(30)
                    it.zahlungsZielInTagen = 30
                    it.kunde = projekt.kunde
                    it.projekt = projekt
                    it.typ = RechnungTyp.RECHNUNG
                    it.betreff = "${auftrag.titel}: ${pos.titel} – $monthName ${period.year}"
                    it.status = if (paid) RechnungStatus.BEZAHLT else RechnungStatus.GESTELLT
                    if (paid) {
                        it.bezahlDatum = date.plusDays((18 + date.dayOfYear % 15).toLong())
                        it.zahlBetrag = amount.multiply(VAT_FACTOR).setScale(2, RoundingMode.HALF_UP)
                    }
                    it.periodOfPerformanceBegin = period.atDay(1)
                    it.periodOfPerformanceEnd = period.atEndOfMonth()
                    it.positionen = mutableListOf()
                }
                rechnung.positionen!!.add(RechnungsPositionDO().also {
                    it.number = 1
                    it.rechnung = rechnung
                    it.text = pos.titel
                    it.auftragsPosition = pos
                    it.menge = BigDecimal.ONE
                    it.einzelNetto = amount
                    it.vat = VAT
                })
                rechnungDao.insert(rechnung, checkAccess = false)
            }
        }
    }

    /** The last twelve months: rent, hosting, a freelancer and some single purchases. */
    private fun insertIncomingInvoices(ctx: TestDataContext) {
        val thisMonth = YearMonth.from(ctx.today)
        for (m in 12 downTo 1) {
            val ym = thisMonth.minusMonths(m.toLong())
            addIncomingInvoice(ctx, "Kassel Office Properties GmbH", "Office rent", ym.atDay(1), 8400, PaymentType.BANK_TRANSFER)
            addIncomingInvoice(ctx, "Nimbus Cloud Services", "Cloud hosting", ym.atDay(5), 2350 + 40 * (m % 4), PaymentType.CREDIT_CARD)
            addIncomingInvoice(ctx, "Rhine Telecom AG", "Phone and internet", ym.atDay(8), 480, PaymentType.DEBIT)
            if (m % 2 == 0) {
                addIncomingInvoice(ctx, "Freelance UX Studio", "UX support Customer App", ym.atEndOfMonth(), 6200 + 300 * (m % 3), PaymentType.BANK_TRANSFER)
            }
        }
        addIncomingInvoice(ctx, "TechStore GmbH", "Laptops for new colleagues", ctx.today.minusDays(12), 12400, PaymentType.BANK_TRANSFER)
        addIncomingInvoice(ctx, "Conference Events Ltd.", "Conference tickets", ctx.today.minusDays(4), 1890, PaymentType.CREDIT_CARD)
    }

    private fun addIncomingInvoice(
        ctx: TestDataContext, kreditor: String, betreff: String, date: LocalDate, net: Int, paymentType: PaymentType,
    ) {
        val paid = date.plusDays(20).isBefore(ctx.today)
        val invoice = EingangsrechnungDO().also {
            it.kreditor = kreditor
            it.referenz = "${kreditor.take(3).uppercase()}-${date.year}-${"%03d".format(date.dayOfYear)}"
            it.betreff = betreff
            it.datum = date
            it.faelligkeit = date.plusDays(14)
            it.paymentType = paymentType
            if (paid) {
                it.bezahlDatum = date.plusDays(10)
                it.zahlBetrag = bd(net).multiply(VAT_FACTOR).setScale(2, RoundingMode.HALF_UP)
            }
            it.positionen = mutableListOf()
        }
        invoice.positionen!!.add(EingangsrechnungsPositionDO().also {
            it.number = 1
            it.eingangsrechnung = invoice
            it.text = betreff
            it.menge = BigDecimal.ONE
            it.einzelNetto = bd(net)
            it.vat = VAT
        })
        eingangsrechnungDao.insert(invoice, checkAccess = false)
    }

    private fun orders(ctx: TestDataContext): List<Order> {
        fun d(year: Int, month: Int, day: Int = 1) = ctx.month(year, month, day)
        val tm = AuftragsPositionsPaymentType.TIME_AND_MATERIALS
        val fp = AuftragsPositionsPaymentType.FESTPREISPAKET
        val neu = AuftragsPositionsArt.NEUENTWICKLUNG
        val wart = AuftragsPositionsArt.WARTUNG
        val cr = AuftragsPositionsArt.CHANGE_REQUEST
        return listOf(
            Order(
                "Fleet Tracking Platform", "Fleet Tracking Platform – Release 1", AuftragsStatus.ABGESCHLOSSEN,
                d(2024, 9, 12), d(2024, 10, 1), null, "joe", listOf(
                    Pos("Specification and design", "Specification", 48000, 40, fp, neu, d(2024, 10, 1), d(2024, 12, 20)),
                    Pos("Implementation", "Development", 186000, 155, tm, neu, d(2025, 1, 6), d(2025, 9, 30)),
                ),
            ),
            Order(
                "Fleet Tracking Platform", "Fleet Tracking Platform – Release 2", AuftragsStatus.BEAUFTRAGT,
                d(2025, 8, 20), d(2025, 9, 15), null, "joe", listOf(
                    Pos("New features: route planning", "Development", 168000, 140, tm, neu, d(2025, 10, 1), d(2026, 12, 18)),
                    Pos("Go-live and hypercare", "Go-live", 24000, 20, fp, neu, d(2026, 11, 2), d(2026, 12, 18)),
                ),
            ),
            Order(
                "Claims Portal", "Claims Portal – new customer portal", AuftragsStatus.BEAUFTRAGT,
                d(2025, 2, 3), d(2025, 3, 10), null, "laura", listOf(
                    Pos("Concept and specification", "Specification", 36000, 30, fp, neu, d(2025, 4, 1), d(2025, 6, 30)),
                    Pos("Implementation", "Development", 252000, 210, tm, neu, d(2025, 7, 1), d(2026, 12, 31)),
                    Pos("Test support", "Testing", 42000, 35, tm, neu, d(2026, 3, 1), d(2026, 12, 31)),
                ),
            ),
            Order(
                "Customer App", "Customer App iOS/Android", AuftragsStatus.LOI,
                d(2026, 7, 14), d(2026, 9, 1), 90, "laura", listOf(
                    Pos("UX and design", "Design", 54000, 45, fp, neu, d(2026, 9, 1), d(2026, 12, 18)),
                    Pos("App development", "Development", 144000, 120, tm, neu, d(2027, 1, 4), d(2027, 6, 30)),
                ),
            ),
            Order(
                "Smart Meter Analytics", "Smart Meter Analytics – pilot", AuftragsStatus.BEAUFTRAGT,
                d(2025, 11, 4), d(2025, 12, 1), null, "joe", listOf(
                    Pos("Pilot implementation", "Development", 132000, 110, tm, neu, d(2026, 1, 5), d(2026, 12, 18)),
                    Pos("Data analysis workshops", "Specification", 18000, 15, fp, neu, d(2026, 1, 5), d(2026, 3, 31)),
                ),
            ),
            Order(
                "Smart Meter Analytics", "Smart Meter Analytics – rollout", AuftragsStatus.GELEGT,
                d(2026, 9, 22), null, 60, "joe", listOf(
                    Pos("Rollout to all regions", "Development", 216000, 180, tm, neu, d(2027, 1, 4), d(2027, 12, 17)),
                ),
            ),
            Order(
                "Patient Scheduling", "Patient Scheduling Platform", AuftragsStatus.BEAUFTRAGT,
                d(2025, 5, 6), d(2025, 6, 2), null, "julia", listOf(
                    Pos("Implementation", "Development", 198000, 165, tm, neu, d(2025, 7, 1), d(2026, 11, 30)),
                    Pos("Maintenance and support", "Go-live", 3500, null, tm, wart, d(2026, 1, 1), d(2026, 12, 31)),
                ),
            ),
            Order(
                "Patient Scheduling", "Patient Scheduling – reporting module", AuftragsStatus.POTENZIAL,
                d(2026, 10, 5), null, 30, "julia", listOf(
                    Pos("Reporting module", "Development", 72000, 60, tm, cr, d(2027, 2, 1), d(2027, 6, 30)),
                ),
            ),
            Order(
                "Shop Relaunch", "Shop Relaunch 2026", AuftragsStatus.BEAUFTRAGT,
                d(2025, 12, 2), d(2026, 1, 12), null, "laura", listOf(
                    Pos("Design and frontend", "Design", 66000, 55, fp, neu, d(2026, 2, 2), d(2026, 6, 30)),
                    Pos("Shop implementation", "Development", 156000, 130, tm, neu, d(2026, 4, 1), d(2026, 12, 18)),
                ),
            ),
            Order(
                "Shop Relaunch", "Shop Relaunch – payment change request", AuftragsStatus.BEAUFTRAGT,
                d(2026, 8, 18), d(2026, 9, 1), null, "laura", listOf(
                    Pos("Additional payment providers", "Development", 28800, 24, fp, cr, d(2026, 9, 1), d(2026, 11, 30)),
                ),
            ),
            Order(
                "Claims Portal", "Claims Portal – document archive", AuftragsStatus.GELEGT,
                d(2026, 9, 30), null, 50, "laura", listOf(
                    Pos("Document archive integration", "Development", 64800, 54, tm, cr, d(2027, 1, 4), d(2027, 4, 30)),
                ),
            ),
        )
    }

    companion object {
        private val VAT = BigDecimal("0.19")

        private val VAT_FACTOR = BigDecimal("1.19")

        private fun bd(value: Number) = BigDecimal(value.toString())
    }
}
