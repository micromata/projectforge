import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.business.fibu.*
import org.projectforge.business.task.*
import org.projectforge.business.timesheet.*

fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val out = StringBuilder()
val xTaskDao = bean(TaskDao::class.java)
val xProjektDao = bean(ProjektDao::class.java)
val xTimesheetDao = bean(TimesheetDao::class.java)
val projekte = xProjektDao.select(QueryFilter(), checkAccess = false).filter { (it.kunde?.nummer ?: 0) >= 300 }
val tasks = xTaskDao.select(QueryFilter(), checkAccess = false)
val sheets = xTimesheetDao.select(QueryFilter(), checkAccess = false)
val hoursByTask = sheets.groupBy { it.task?.id }.mapValues { (_, l) -> l.sumOf { (it.stopTime!!.time - it.startTime!!.time) / 3600000.0 } }
val ratios = listOf(0.42, 0.68, 0.83, 0.94, 1.07, 0.55, 0.77)
var i = 0
projekte.forEach { p ->
    val projectTask = tasks.first { it.id == p.task!!.id }
    val kundeTask = tasks.first { it.id == projectTask.parentTask!!.id }
    if (kundeTask.title != p.kunde!!.name) { kundeTask.title = p.kunde!!.name; xTaskDao.update(kundeTask, checkAccess = false) }
    var total = 0
    tasks.filter { it.parentTask?.id == projectTask.id }.forEach { t ->
        val booked = hoursByTask[t.id] ?: 0.0
        val max = if (booked > 0) (booked / ratios[i++ % ratios.size]).toInt().coerceAtLeast(8) else 80
        t.maxHours = max; total += max
        xTaskDao.update(t, checkAccess = false)
    }
    projectTask.maxHours = (total * 1.1).toInt()
    projectTask.shortDescription = "Project lead: Joe Stone"
    xTaskDao.update(projectTask, checkAccess = false)
}
out.appendLine("tasks updated")
out.toString()
