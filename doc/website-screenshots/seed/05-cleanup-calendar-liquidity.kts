import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.business.user.*
import org.projectforge.business.fibu.*
import org.projectforge.business.task.*
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.plugins.liquidityplanning.*
import java.math.BigDecimal
import java.time.*
import java.util.*

fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val out = StringBuilder()
val xUserDao = bean(UserDao::class.java)
val xAuftragDao = bean(AuftragDao::class.java)
val xKundeDao = bean(KundeDao::class.java)
val xTaskDao = bean(TaskDao::class.java)
val xTeamCalDao = bean(TeamCalDao::class.java)
val xTeamEventDao = bean(TeamEventDao::class.java)
val xLiquidityEntryDao = bean(LiquidityEntryDao::class.java)
val zone = ZoneId.of("Europe/Berlin")
val shiftMonths = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.of(2026, 10), YearMonth.now())
fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day).plusMonths(shiftMonths)
fun run(name: String, block: () -> Unit) { try { block() } catch (ex: Exception) { out.appendLine("$name: ${ex::class.simpleName} ${ex.message}") } }

// Cleanup of the built-in test data that shows real or confusing names.
run("rename kai") { xUserDao.getInternalByName("kai")?.let { u -> u.firstname = "Kim"; u.lastname = "Richter"; xUserDao.update(u, checkAccess = false) } }
xAuftragDao.select(QueryFilter(), checkAccess = false).filter { (it.kunde?.nummer ?: 0L) < 300L }.forEach { a -> run("order ${a.id}") { xAuftragDao.markAsDeleted(a, checkAccess = false) } }
run("customer 200") { xKundeDao.find(200L, checkAccess = false)?.let { k -> k.name = "Example Consulting (internal)"; k.identifier = "Internal"; xKundeDao.update(k, checkAccess = false) } }
xTaskDao.select(QueryFilter(), checkAccess = false).filter { it.title == "Yellow Logistics" || it.title == "ProjectForge" }.forEach { t ->
    run("task ${t.title}") { xTaskDao.markAsDeleted(t, checkAccess = false) }
}
xTaskDao.select(QueryFilter(), checkAccess = false).firstOrNull { it.title == "My Company" }?.let { t -> run("rename my company") { t.title = "Example Consulting"; xTaskDao.update(t, checkAccess = false) } }

// Team calendar with events around today.
val laura = xUserDao.getInternalByName("laura")!!
val cal = TeamCalDO().also { it.title = "Delivery team"; it.owner = laura; it.description = "Workshops, reviews and customer appointments"; it.includeLeaveDaysForGroups = null }
run("calendar") { xTeamCalDao.insert(cal, checkAccess = false) }
data class E(val day: LocalDate, val from: String, val to: String, val subject: String, val location: String)
val monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
val events = listOf(
    E(monday, "09:00", "09:30", "Weekly planning", "Office"),
    E(monday.plusDays(1), "10:00", "12:00", "Claims Portal: sprint review", "Alpine Insurance, Innsbruck"),
    E(monday.plusDays(1), "14:00", "15:00", "1:1 Alex", "Office"),
    E(monday.plusDays(2), "09:30", "11:00", "Shop Relaunch: design walkthrough", "Remote"),
    E(monday.plusDays(2), "13:00", "14:30", "Steering committee Fleet Tracking", "Bluefin Logistics, Hamburg"),
    E(monday.plusDays(3), "10:00", "16:00", "Smart Meter workshop", "Greenfield Energy, Freiburg"),
    E(monday.plusDays(4), "11:00", "12:00", "Forecast review with finance", "Office"),
    E(monday.plusDays(4), "16:00", "17:30", "Team retrospective", "Office"),
    E(monday.plusDays(7), "09:00", "09:30", "Weekly planning", "Office"),
    E(monday.plusDays(8), "13:00", "15:00", "Patient Scheduling: acceptance test", "Harbor Medical"),
)
events.forEach { e ->
    val ev = TeamEventDO().also {
        it.calendar = cal; it.subject = e.subject; it.location = e.location; it.creator = laura
        it.startDate = Date.from(e.day.atTime(LocalTime.parse(e.from)).atZone(zone).toInstant())
        it.endDate = Date.from(e.day.atTime(LocalTime.parse(e.to)).atZone(zone).toInstant())
    }
    run("event ${e.subject}") { xTeamEventDao.insert(ev, checkAccess = false) }
}

// Liquidity: costs of the next months (incoming invoices come from the invoices).
data class L(val date: LocalDate, val amount: Int, val subject: String, val paid: Boolean = false)
val liq = mutableListOf<L>()
for (m in 0..5) {
    val ym = YearMonth.now().plusMonths(m.toLong())
    liq.add(L(ym.atEndOfMonth(), -96500, "Salaries"))
    liq.add(L(ym.atDay(3), -8400, "Office rent Kassel"))
    liq.add(L(ym.atDay(15), -2350, "Cloud hosting"))
    liq.add(L(ym.atDay(10), -21800, "Social security contributions"))
}
liq.add(L(d(2026, 11, 10), -18900, "VAT advance payment"))
liq.add(L(d(2026, 12, 10), -24600, "VAT advance payment"))
liq.add(L(d(2026, 11, 20), 54000, "Expected: Customer App design (Alpine Insurance)"))
liq.add(L(d(2026, 12, 22), 28800, "Expected: payment change request (Lumen Retail)"))
liq.add(L(d(2026, 10, 30), -12400, "Laptops for new colleagues"))
liq.forEach { l ->
    val entry = LiquidityEntryDO().also { it.dateOfPayment = l.date; it.amount = BigDecimal(l.amount); it.subject = l.subject; it.paid = l.paid }
    run("liq ${l.subject}") { xLiquidityEntryDao.insert(entry, checkAccess = false) }
}
out.appendLine("seed4 done")
out.toString()
