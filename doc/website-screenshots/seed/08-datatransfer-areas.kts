import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.plugins.datatransfer.*
fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val ud = bean(org.projectforge.business.user.UserDao::class.java)
val dd = bean(DataTransferAreaDao::class.java)
fun id(n: String) = ud.getInternalByName(n)!!.id
val ids = mutableListOf<Long>()
listOf(
  Triple("Alpine Insurance – Claims Portal", "Specifications, test reports and release documents shared with the customer", listOf("laura", "chris", "joe")),
  Triple("Bluefin Logistics – Fleet Tracking", "Exchange of route data exports and acceptance protocols", listOf("joe", "alex")),
  Triple("Design assets", "Logos, mockups and style guides for all projects", listOf("ann", "max", "mona")),
).forEach { (name, desc, users) ->
  val a = DataTransferAreaDO().also {
    it.areaName = name; it.description = desc; it.adminIds = "${id("laura")}"; it.accessUserIds = users.map { u -> id(u) }.joinToString(",")
    it.expiryDays = 30; it.maxUploadSizeKB = 500 * 1024
    it.externalDownloadEnabled = !name.startsWith("Design"); it.externalUploadEnabled = name.startsWith("Alpine")
    it.externalPassword = if (it.externalDownloadEnabled == true) "Share-2026" else null
  }
  dd.insert(a, checkAccess = false); ids.add(a.id!!)
}
ids.joinToString(",")
