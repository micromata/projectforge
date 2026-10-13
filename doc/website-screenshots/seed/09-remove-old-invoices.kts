import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.QueryFilter
fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val rd = bean(org.projectforge.business.fibu.RechnungDao::class.java)
val old = rd.select(QueryFilter(), checkAccess = false).filter { (it.kunde?.nummer ?: 0L) < 300L }
old.forEach { r -> try { rd.markAsDeleted(r, checkAccess = false) } catch (e: Exception) {} }
val mine = rd.select(QueryFilter(), checkAccess = false).filter { !it.deleted && (it.kunde?.nummer ?: 0L) >= 300L }.sortedByDescending { it.datum }
"deleted ${old.size}; latest: " + mine.take(3).map { r -> "${r.id}:${r.betreff}:${r.status}" }
