import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.business.user.*
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.*
import org.projectforge.business.task.*
import org.projectforge.common.task.TaskStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val out = StringBuilder()
val xUserDao = bean(UserDao::class.java)
val xGroupDao = bean(GroupDao::class.java)
val xUserGroupCache = bean(UserGroupCache::class.java)
val xUserRightDao = bean(UserRightDao::class.java)
val xUserPasswordDao = bean(UserPasswordDao::class.java)
val xEmployeeDao = bean(EmployeeDao::class.java)
val xEmployeeService = bean(EmployeeService::class.java)
val xKundeDao = bean(KundeDao::class.java)
val xProjektDao = bean(ProjektDao::class.java)
val xKost2Dao = bean(Kost2Dao::class.java)
val xKost2ArtDao = bean(Kost2ArtDao::class.java)
val xTaskDao = bean(TaskDao::class.java)

// 1. Screenshot user Laura Becker with the groups and rights of e2e-full-access.
val template = xUserDao.getInternalByName("e2e-full-access")!!
var laura = xUserDao.getInternalByName("laura")
if (laura == null) {
    laura = PFUserDO().also {
        it.username = "laura"; it.firstname = "Laura"; it.lastname = "Becker"
        it.email = "laura.becker@example.com"; it.locale = Locale.ENGLISH
        it.timeZone = TimeZone.getTimeZone("Europe/Berlin"); it.localUser = true
        it.organization = "Example Consulting"
    }
    val l = laura!!
    xUserDao.insert(l, checkAccess = false)
    xGroupDao.assignGroupByIds(l, xUserGroupCache.getUserGroups(template)!!.toList(), null, true)
    xUserRightDao.select(template, checkAccess = false).forEach { r ->
        xUserRightDao.insert(UserRightDO().also { n -> n.user = l; n.rightIdString = r.rightIdString; n.value = r.value }, checkAccess = false)
    }
    xUserPasswordDao.encryptAndSavePassword(l.id!!, "Screenshots-2026!".toCharArray(), false)
    out.appendLine("laura created")
}
// English locale for the fictional users, so lists show English texts.
val people = listOf("laura", "alex", "ann", "chris", "joe", "julia", "max", "michael", "mona")
people.forEach { name ->
    val u = xUserDao.getInternalByName(name)!!
    if (u.locale == null) { u.locale = Locale.ENGLISH; xUserDao.update(u, checkAccess = false) }
}

// 2. Employees.
val existingEmployees = xEmployeeDao.select(QueryFilter(), checkAccess = false).mapNotNull { it.user?.username }.toSet()
val positions = mapOf("laura" to "Head of Delivery", "alex" to "Senior Developer", "ann" to "UX Designer",
    "chris" to "Developer", "joe" to "Project Manager", "julia" to "Consultant", "max" to "Developer",
    "michael" to "Finance", "mona" to "Developer")
people.filter { it !in existingEmployees }.forEach { name ->
    val e = EmployeeDO().also {
        it.user = xUserDao.getInternalByName(name)
        it.position = positions[name]; it.eintrittsDatum = LocalDate.of(2019, 4, 1); it.abteilung = "Delivery"
    }
    xEmployeeDao.insert(e, checkAccess = false)
    xEmployeeService.insertStatus(e, LocalDate.of(2019, 4, 1), EmployeeStatus.FEST_ANGESTELLTER, checkAccess = false)
    xEmployeeService.insertAnnualLeaveDays(e, LocalDate.of(2019, 4, 1), BigDecimal(30), checkAccess = false)
    xEmployeeService.insertWeeklyHours(e, LocalDate.of(2019, 4, 1), BigDecimal(40), checkAccess = false)
}
out.appendLine("employees ok")

// 3. Customers, projects, tasks, cost 2.
val kost2Arts = xKost2ArtDao.select(QueryFilter(), checkAccess = false).associateBy { it.id }
out.appendLine("kost2arts: " + kost2Arts.values.joinToString { "${it.id}:${it.name}:${it.fakturiert}" })
val root = xTaskDao.select(QueryFilter(), checkAccess = false).first { it.parentTask == null }
data class P(val kunde: Long, val kundeName: String, val nr: Int, val name: String, val ident: String)
val projects = listOf(
    P(300, "Bluefin Logistics GmbH", 1, "Fleet Tracking Platform", "Fleet"),
    P(301, "Alpine Insurance AG", 1, "Claims Portal", "Claims"),
    P(301, "Alpine Insurance AG", 2, "Customer App", "App"),
    P(302, "Greenfield Energy GmbH", 1, "Smart Meter Analytics", "Meter"),
    P(303, "Harbor Medical Systems", 1, "Patient Scheduling", "Scheduling"),
    P(304, "Lumen Retail Group", 1, "Shop Relaunch", "Shop"),
)
val kundeTasks = mutableMapOf<Long, TaskDO>()
projects.forEach { p ->
    val kunde = xKundeDao.find(p.kunde, checkAccess = false) ?: KundeDO().also {
        it.nummer = p.kunde; it.name = p.kundeName; it.identifier = p.kundeName.substringBefore(' ')
        it.status = KundeStatus.ACTIVE
        xKundeDao.insert(it, checkAccess = false)
    }
    val kundeTask = kundeTasks.getOrPut(p.kunde) {
        TaskDO().also { it.parentTask = root; it.title = kunde.identifier; it.status = TaskStatus.O; xTaskDao.insert(it, checkAccess = false) }
    }
    val projectTask = TaskDO().also { it.parentTask = kundeTask; it.title = p.name; it.status = TaskStatus.O; it.maxHours = 2400; xTaskDao.insert(it, checkAccess = false) }
    listOf("Specification" to 300, "Design" to 250, "Development" to 1200, "Testing" to 300, "Go-live" to 120).forEach { (t, h) ->
        TaskDO().also { it.parentTask = projectTask; it.title = t; it.status = TaskStatus.O; it.maxHours = h; xTaskDao.insert(it, checkAccess = false) }
    }
    val projekt = ProjektDO().also {
        it.nummer = p.nr; it.name = p.name; it.identifier = p.ident; it.kunde = kunde
        it.status = ProjektStatus.BUILD; it.task = projectTask
        it.projectManager = xUserDao.getInternalByName("joe"); it.headOfBusinessManager = xUserDao.getInternalByName("laura")
    }
    xProjektDao.insert(projekt, checkAccess = false)
    kost2Arts.values.filter { it.projektStandard || it.id == 0L || it.id == 1L || it.id == 2L }.forEach { art ->
        val k = Kost2DO().also {
            it.nummernkreis = 5; it.bereich = p.kunde.toInt(); it.teilbereich = p.nr; it.kost2Art = art
            it.projekt = projekt; it.kostentraegerStatus = KostentraegerStatus.ACTIVE; it.description = art.name
        }
        xKost2Dao.insert(k, checkAccess = false)
    }
    out.appendLine("project ${p.name} ok")
}
out.toString()
