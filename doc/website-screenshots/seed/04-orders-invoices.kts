import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.business.user.*
import org.projectforge.business.fibu.*
import org.projectforge.business.task.*
import java.math.BigDecimal
import java.time.*

fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val out = StringBuilder()
val xUserDao = bean(UserDao::class.java)
val xProjektDao = bean(ProjektDao::class.java)
val xTaskDao = bean(TaskDao::class.java)
val xAuftragDao = bean(AuftragDao::class.java)
val xRechnungDao = bean(RechnungDao::class.java)
val today = LocalDate.now()
// The data was designed for October 2026; all dates are shifted by the months since then.
val shiftMonths = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.of(2026, 10), YearMonth.from(today))
val projekte = xProjektDao.select(QueryFilter(), checkAccess = false).filter { (it.kunde?.nummer ?: 0) >= 300 }.associateBy { it.name }
val allTasks = xTaskDao.select(QueryFilter(), checkAccess = false)
fun sub(projekt: String, title: String) = allTasks.first { it.title == title && it.parentTask?.id == projekte[projekt]!!.task!!.id }
fun bd(v: Number) = BigDecimal(v.toString())

data class Pos(val title: String, val task: String, val net: Int, val pd: Int?, val type: AuftragsPositionsPaymentType, val art: AuftragsPositionsArt, val begin: LocalDate, val end: LocalDate)
data class O(val projekt: String, val titel: String, val status: AuftragsStatus, val offer: LocalDate, val decision: LocalDate?, val prob: Int?, val positions: List<Pos>, val contact: String)
val TM = AuftragsPositionsPaymentType.TIME_AND_MATERIALS
val FP = AuftragsPositionsPaymentType.FESTPREISPAKET
val NEU = AuftragsPositionsArt.NEUENTWICKLUNG
val WART = AuftragsPositionsArt.WARTUNG
val CR = AuftragsPositionsArt.CHANGE_REQUEST
fun d(y: Int, m: Int, day: Int = 1) = LocalDate.of(y, m, day).plusMonths(shiftMonths)
val orders = listOf(
    O("Fleet Tracking Platform", "Fleet Tracking Platform – Release 1", AuftragsStatus.ABGESCHLOSSEN, d(2024, 9, 12), d(2024, 10, 1), null, listOf(
        Pos("Specification and design", "Specification", 48000, 40, FP, NEU, d(2024, 10, 1), d(2024, 12, 20)),
        Pos("Implementation", "Development", 186000, 155, TM, NEU, d(2025, 1, 6), d(2025, 9, 30))), "joe"),
    O("Fleet Tracking Platform", "Fleet Tracking Platform – Release 2", AuftragsStatus.BEAUFTRAGT, d(2025, 8, 20), d(2025, 9, 15), null, listOf(
        Pos("New features: route planning", "Development", 168000, 140, TM, NEU, d(2025, 10, 1), d(2026, 12, 18)),
        Pos("Go-live and hypercare", "Go-live", 24000, 20, FP, NEU, d(2026, 11, 2), d(2026, 12, 18))), "joe"),
    O("Claims Portal", "Claims Portal – new customer portal", AuftragsStatus.BEAUFTRAGT, d(2025, 2, 3), d(2025, 3, 10), null, listOf(
        Pos("Concept and specification", "Specification", 36000, 30, FP, NEU, d(2025, 4, 1), d(2025, 6, 30)),
        Pos("Implementation", "Development", 252000, 210, TM, NEU, d(2025, 7, 1), d(2026, 12, 31)),
        Pos("Test support", "Testing", 42000, 35, TM, NEU, d(2026, 3, 1), d(2026, 12, 31))), "laura"),
    O("Customer App", "Customer App iOS/Android", AuftragsStatus.LOI, d(2026, 7, 14), d(2026, 9, 1), 90, listOf(
        Pos("UX and design", "Design", 54000, 45, FP, NEU, d(2026, 9, 1), d(2026, 12, 18)),
        Pos("App development", "Development", 144000, 120, TM, NEU, d(2027, 1, 4), d(2027, 6, 30))), "laura"),
    O("Smart Meter Analytics", "Smart Meter Analytics – pilot", AuftragsStatus.BEAUFTRAGT, d(2025, 11, 4), d(2025, 12, 1), null, listOf(
        Pos("Pilot implementation", "Development", 132000, 110, TM, NEU, d(2026, 1, 5), d(2026, 12, 18)),
        Pos("Data analysis workshops", "Specification", 18000, 15, FP, NEU, d(2026, 1, 5), d(2026, 3, 31))), "joe"),
    O("Smart Meter Analytics", "Smart Meter Analytics – rollout", AuftragsStatus.GELEGT, d(2026, 9, 22), null, 60, listOf(
        Pos("Rollout to all regions", "Development", 216000, 180, TM, NEU, d(2027, 1, 4), d(2027, 12, 17))), "joe"),
    O("Patient Scheduling", "Patient Scheduling Platform", AuftragsStatus.BEAUFTRAGT, d(2025, 5, 6), d(2025, 6, 2), null, listOf(
        Pos("Implementation", "Development", 198000, 165, TM, NEU, d(2025, 7, 1), d(2026, 11, 30)),
        Pos("Maintenance and support", "Go-live", 3500, null, TM, WART, d(2026, 1, 1), d(2026, 12, 31))), "julia"),
    O("Patient Scheduling", "Patient Scheduling – reporting module", AuftragsStatus.POTENZIAL, d(2026, 10, 5), null, 30, listOf(
        Pos("Reporting module", "Development", 72000, 60, TM, CR, d(2027, 2, 1), d(2027, 6, 30))), "julia"),
    O("Shop Relaunch", "Shop Relaunch 2026", AuftragsStatus.BEAUFTRAGT, d(2025, 12, 2), d(2026, 1, 12), null, listOf(
        Pos("Design and frontend", "Design", 66000, 55, FP, NEU, d(2026, 2, 2), d(2026, 6, 30)),
        Pos("Shop implementation", "Development", 156000, 130, TM, NEU, d(2026, 4, 1), d(2026, 12, 18))), "laura"),
    O("Shop Relaunch", "Shop Relaunch – payment change request", AuftragsStatus.BEAUFTRAGT, d(2026, 8, 18), d(2026, 9, 1), null, listOf(
        Pos("Additional payment providers", "Development", 28800, 24, FP, CR, d(2026, 9, 1), d(2026, 11, 30))), "laura"),
    O("Claims Portal", "Claims Portal – document archive", AuftragsStatus.GELEGT, d(2026, 9, 30), null, 50, listOf(
        Pos("Document archive integration", "Development", 64800, 54, TM, CR, d(2027, 1, 4), d(2027, 4, 30))), "laura"),
)
var orderCount = 0
var invoiceCount = 0
orders.forEach { o ->
    val projekt = projekte[o.projekt]!!
    val auftrag = AuftragDO().also {
        it.nummer = xAuftragDao.getNextNumber(null)
        it.titel = o.titel; it.status = o.status; it.kunde = projekt.kunde; it.projekt = projekt
        it.angebotsDatum = o.offer; it.erfassungsDatum = o.offer; it.entscheidungsDatum = o.decision
        it.beauftragungsDatum = if (o.status == AuftragsStatus.BEAUFTRAGT || o.status == AuftragsStatus.ABGESCHLOSSEN) o.decision else null
        it.probabilityOfOccurrence = o.prob
        it.contactPerson = xUserDao.getInternalByName(o.contact)
        it.periodOfPerformanceBegin = o.positions.minOf { p -> p.begin }; it.periodOfPerformanceEnd = o.positions.maxOf { p -> p.end }
        it.positionen = mutableListOf()
    }
    o.positions.forEachIndexed { i, p ->
        val pos = AuftragsPositionDO().also {
            it.number = (i + 1).toShort(); it.auftrag = auftrag; it.titel = p.title; it.task = sub(o.projekt, p.task)
            it.art = p.art; it.paymentType = p.type; it.status = o.status
            it.nettoSumme = bd(if (p.art == WART) p.net * 12 else p.net); it.personDays = p.pd?.let { pd -> bd(pd) }
            it.periodOfPerformanceType = PeriodOfPerformanceType.OWN; it.periodOfPerformanceBegin = p.begin; it.periodOfPerformanceEnd = p.end
            it.vollstaendigFakturiert = o.status == AuftragsStatus.ABGESCHLOSSEN
        }
        auftrag.positionen!!.add(pos)
    }
    xAuftragDao.insert(auftrag, checkAccess = false)
    orderCount++
    if (o.status != AuftragsStatus.BEAUFTRAGT && o.status != AuftragsStatus.ABGESCHLOSSEN) return@forEach
    val saved = xAuftragDao.find(auftrag.id, checkAccess = false)!!
    // Invoices: fixed price at the end, time and materials monthly.
    saved.positionen!!.forEach { pos ->
        val months = mutableListOf<Pair<LocalDate, BigDecimal>>()
        val begin = pos.periodOfPerformanceBegin!!
        val end = minOf(pos.periodOfPerformanceEnd!!, d(2026, 9, 30))
        if (pos.paymentType == FP) {
            if (pos.periodOfPerformanceEnd!!.isBefore(today)) months.add(pos.periodOfPerformanceEnd!!.plusDays(3) to pos.nettoSumme!!)
        } else {
            val monthCount = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(begin), YearMonth.from(pos.periodOfPerformanceEnd!!)) + 1
            var ym = YearMonth.from(begin)
            while (!ym.atDay(1).isAfter(end)) {
                val share = pos.nettoSumme!!.divide(bd(monthCount), 0, java.math.RoundingMode.HALF_UP)
                val factor = bd(listOf(0.85, 0.95, 1.0, 1.05, 1.1)[(ym.monthValue + pos.number) % 5])
                months.add(ym.plusMonths(1).atDay(3) to share.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP))
                ym = ym.plusMonths(1)
            }
        }
        months.filter { it.first.isBefore(today) }.forEach { (date, amount) ->
            val r = RechnungDO().also {
                it.nummer = xRechnungDao.getNextNumber(null)
                it.datum = date; it.faelligkeit = date.plusDays(30); it.zahlungsZielInTagen = 30
                it.kunde = projekt.kunde; it.projekt = projekt; it.typ = RechnungTyp.RECHNUNG
                it.betreff = "${saved.titel}: ${pos.titel} – ${date.minusMonths(1).month.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ${date.minusMonths(1).year}"
                val paid = date.plusDays(40).isBefore(today)
                it.status = if (paid) RechnungStatus.BEZAHLT else RechnungStatus.GESTELLT
                if (paid) { it.bezahlDatum = date.plusDays((18 + date.dayOfYear % 15).toLong()); it.zahlBetrag = amount.multiply(bd(1.19)).setScale(2, java.math.RoundingMode.HALF_UP) }
                it.periodOfPerformanceBegin = YearMonth.from(date.minusMonths(1)).atDay(1); it.periodOfPerformanceEnd = YearMonth.from(date.minusMonths(1)).atEndOfMonth()
                it.positionen = mutableListOf()
            }
            val rp = RechnungsPositionDO().also {
                it.number = 1; it.rechnung = r; it.text = pos.titel; it.auftragsPosition = pos
                it.menge = BigDecimal.ONE; it.einzelNetto = amount; it.vat = bd(0.19)
            }
            r.positionen!!.add(rp)
            try { xRechnungDao.insert(r, checkAccess = false); invoiceCount++ } catch (ex: Exception) { if (invoiceCount < 2) out.appendLine("invoice: ${ex::class.simpleName} ${ex.message}") }
        }
    }
}
out.appendLine("orders=$orderCount invoices=$invoiceCount")
out.toString()
