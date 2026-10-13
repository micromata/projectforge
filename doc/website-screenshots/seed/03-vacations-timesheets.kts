import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.business.user.*
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.*
import org.projectforge.business.task.*
import org.projectforge.business.timesheet.*
import org.projectforge.business.vacation.model.*
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.VacationValidator
import java.time.*
import java.util.*

fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val out = StringBuilder()
val xUserDao = bean(UserDao::class.java)
val xTaskDao = bean(TaskDao::class.java)
val xProjektDao = bean(ProjektDao::class.java)
val xKost2Dao = bean(Kost2Dao::class.java)
val xTimesheetDao = bean(TimesheetDao::class.java)
val xEmployeeDao = bean(EmployeeDao::class.java)
val xVacationDao = bean(VacationDao::class.java)
val zone = ZoneId.of("Europe/Berlin")
val today = LocalDate.now()
// The data was designed for Tuesday, 2026-10-13; all dates are shifted by the weeks since then.
val shiftDays = java.time.temporal.ChronoUnit.WEEKS.between(LocalDate.of(2026, 10, 13), today) * 7
fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day).plusDays(shiftDays)

val projekte = xProjektDao.select(QueryFilter(), checkAccess = false).filter { it.kunde?.nummer ?: 0 >= 300 }.associateBy { it.name }
val allTasks = xTaskDao.select(QueryFilter(), checkAccess = false)
val kost2s = xKost2Dao.select(QueryFilter(), checkAccess = false)
fun sub(projekt: String, title: String) = allTasks.first { it.title == title && it.parentTask?.id == projekte[projekt]!!.task!!.id }
fun kost(projekt: String, art: Long) = kost2s.first { it.projekt?.id == projekte[projekt]!!.id && it.kost2Art?.id == art }
val employees = xEmployeeDao.select(QueryFilter(), checkAccess = false).associateBy { it.user?.username }

// Vacations (past ones have no time sheets).
data class V(val user: String, val from: LocalDate, val to: LocalDate, val status: VacationStatus)
val vacations = listOf(
    V("ann", d(2026, 9, 28), d(2026, 10, 2), VacationStatus.APPROVED),
    V("alex", d(2026, 10, 19), d(2026, 10, 23), VacationStatus.APPROVED),
    V("chris", d(2026, 10, 15), d(2026, 10, 16), VacationStatus.IN_PROGRESS),
    V("julia", d(2026, 11, 2), d(2026, 11, 13), VacationStatus.APPROVED),
    V("max", d(2026, 8, 10), d(2026, 8, 21), VacationStatus.APPROVED),
    V("laura", d(2026, 12, 21), d(2026, 12, 30), VacationStatus.APPROVED),
    V("mona", d(2026, 10, 26), d(2026, 10, 30), VacationStatus.APPROVED),
)
vacations.forEach { v ->
    val vac = VacationDO().also {
        it.employee = employees[v.user]; it.startDate = v.from; it.endDate = v.to; it.status = v.status
        it.manager = employees["laura"]; it.replacement = employees[if (v.user == "max") "mona" else "max"]
        it.special = false
    }
    try { xVacationDao.insert(vac, checkAccess = false) } catch (ex: Exception) { out.appendLine("vacation ${v.user}: ${ex.message}") }
}
out.appendLine("vacations done")

// Time sheets: last 9 weeks, two blocks per working day.
data class Plan(val user: String, val am: Pair<String, String>, val pm: Pair<String, String>)
val plans = listOf(
    Plan("laura", "Claims Portal" to "Specification", "Fleet Tracking Platform" to "Go-live"),
    Plan("alex", "Fleet Tracking Platform" to "Development", "Fleet Tracking Platform" to "Testing"),
    Plan("ann", "Customer App" to "Design", "Shop Relaunch" to "Design"),
    Plan("chris", "Claims Portal" to "Development", "Claims Portal" to "Testing"),
    Plan("joe", "Smart Meter Analytics" to "Specification", "Patient Scheduling" to "Specification"),
    Plan("julia", "Patient Scheduling" to "Development", "Smart Meter Analytics" to "Development"),
    Plan("max", "Shop Relaunch" to "Development", "Customer App" to "Development"),
    Plan("mona", "Smart Meter Analytics" to "Development", "Shop Relaunch" to "Testing"),
)
val texts = mapOf(
    "Specification" to listOf("Requirements workshop with the customer", "User stories and acceptance criteria", "Refined the backlog for the next sprint", "Specification of the interfaces"),
    "Design" to listOf("Wireframes for the dashboard", "Clickable prototype, feedback round", "Design system: buttons and forms", "Usability test with five users"),
    "Development" to listOf("Implemented the REST API for orders", "Code review and refactoring", "Database migration and import", "Fixed issues from the test", "Frontend: list and filter views", "Integration of the payment provider"),
    "Testing" to listOf("Regression test of the release candidate", "Automated end-to-end tests", "Load test and tuning", "Bug fixing after the acceptance test"),
    "Go-live" to listOf("Go-live planning with operations", "Release notes and training material", "Hypercare after the go-live"),
)
val holidays = setOf(LocalDate.of(2026, 10, 3))
var count = 0
val rnd = Random(42)
plans.forEach { p ->
    val user = xUserDao.getInternalByName(p.user)!!
    val off = vacations.filter { it.user == p.user && it.status == VacationStatus.APPROVED }
    var day = today.minusWeeks(9)
    while (!day.isAfter(today)) {
        val skip = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY || day in holidays ||
                off.any { !day.isBefore(it.from) && !day.isAfter(it.to) }
        if (!skip) {
            val blocks = if (day == today) listOf(p.am to (LocalTime.of(8, 30) to LocalTime.of(12, 0)))
            else listOf(p.am to (LocalTime.of(8, 30) to LocalTime.of(12, 30)), p.pm to (LocalTime.of(13, 15) to LocalTime.of(17, 15 + rnd.nextInt(3) * 15)))
            blocks.forEach { (target, time) ->
                val task = sub(target.first, target.second)
                val ts = TimesheetDO().also {
                    it.user = user; it.task = task
                    it.kost2 = kost(target.first, if (target.second == "Specification" && rnd.nextInt(4) == 0) 1L else 2L)
                    it.startTime = Date.from(day.atTime(time.first).atZone(zone).toInstant())
                    it.stopTime = Date.from(day.atTime(time.second).atZone(zone).toInstant())
                    it.timeZone = "Europe/Berlin"
                    it.location = if (rnd.nextInt(3) == 0) "Customer site" else if (rnd.nextBoolean()) "Office" else "Remote"
                    it.description = texts[target.second]!![rnd.nextInt(texts[target.second]!!.size)]
                }
                try { xTimesheetDao.insert(ts, checkAccess = false); count++ } catch (ex: Exception) { if (count < 3) out.appendLine("ts: ${ex.message}") }
            }
        }
        day = day.plusDays(1)
    }
}
out.appendLine("timesheets: $count")
out.toString()
