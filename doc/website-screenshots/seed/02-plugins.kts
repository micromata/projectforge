import org.projectforge.framework.configuration.ApplicationContextProvider
fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)
val s = bean(org.projectforge.plugins.core.PluginAdminService::class.java)
listOf("memo", "todo", "ihk", "marketing").map { id -> id + ":" + s.storePluginToBeActivated(id, false) }.joinToString() +
  " | skillmatrix:" + s.storePluginToBeActivated("skillmatrix", true) + " merlin:" + s.storePluginToBeActivated("merlin", true)
