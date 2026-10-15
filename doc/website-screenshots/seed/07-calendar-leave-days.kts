import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val ud = bean(org.projectforge.business.user.UserDao::class.java)
val cd = bean(org.projectforge.business.teamcal.admin.TeamCalDao::class.java)
val ids = listOf("laura", "alex", "ann", "chris", "joe", "julia", "max", "mona").map { ud.getInternalByName(it)!!.id }.joinToString(",")
val cal = cd.select(QueryFilter(), checkAccess = false).first { it.title == "Delivery team" }
cal.includeLeaveDaysForUsers = ids
cd.update(cal, checkAccess = false)
"cal ${cal.id} users $ids"
