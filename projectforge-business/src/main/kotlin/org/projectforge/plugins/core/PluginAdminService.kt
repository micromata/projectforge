/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.core

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * For administration of plugins.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 * @author Kai Reinhard
 */
@Service
open class PluginAdminService {
  @Autowired
  private lateinit var configurationDao: ConfigurationDao

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  /**
   * All plugins registered as Spring components (activated as well as not activated ones).
   */
  private lateinit var allPlugins: List<AbstractPlugin>

  open val availablePlugins: List<AbstractPlugin>
    get() = allPlugins

  private val afterCreatedActivePluginsCallback: MutableList<PluginCallback> = ArrayList()

  @PostConstruct
  private fun postConstruct() {
    val serviceLoader: ServiceLoader<AbstractPlugin> = ServiceLoader.load(AbstractPlugin::class.java)
    allPlugins = serviceLoader.toList()
    // val pluginNames = applicationContext.getBeanNamesForType(AbstractPlugin::class.java)
    // allPlugins = pluginNames.map { applicationContext.getBean(it, AbstractPlugin::class.java) }
    log.info { "Plugins found: ${allPlugins.joinToString { it.id }}." }
  }

  /**
   * List of all activated plugins.
   */
  open val activePlugins: List<AbstractPlugin>
    get() {
      val pluginsRegistry = PluginsRegistry.instance()
      return pluginsRegistry.plugins
    }

  /**
   * Store a plugin as activated.
   * read LocalSettings pf.plugins.active. If not defined, uses ConfigurationParam.
   *
   * @param id
   * @param activate
   * @return the active plugins
   */
  open fun storePluginToBeActivated(id: String, activate: Boolean): Boolean {
    val activated = activatedPluginsFromConfiguration
    if (activate) {
      activated.add(id)
    } else {
      activated.remove(id)
    }
    activated.sort()
    val sval = StringUtils.join(activated, ",")
    var configuration = configurationDao.getEntry(ConfigurationParam.PLUGIN_ACTIVATED)
    if (configuration == null) {
      configuration = ConfigurationDO()
      val param = ConfigurationParam.PLUGIN_ACTIVATED
      configuration.parameter = param.key
      configuration.configurationType = param.type
    }
    configuration.stringValue = sval
    configurationDao.internalSaveOrUpdate(configuration)
    Configuration.instance.forceReload()
    return false
  }

  /**
   * Get activated plugins from configuration by reading values.
   *
   * @return the activated plugins as list of id strings.
   * @see [GlobalConfiguration.getStringValue]
   */
  open val activatedPluginsFromConfiguration: MutableList<String>
    get() {
      val plugins = Configuration.instance.getStringValue(ConfigurationParam.PLUGIN_ACTIVATED)
      if (plugins.isNullOrBlank()) {
        return mutableListOf()
      }
      return plugins.split(",").map { it.trim { it <= ' ' } }.sorted().toMutableList()
    }

  /**
   * Will be active plugins
   */
  open fun initializeActivePlugins() {
    initializeActivePlugins(true)
  }

  /**
   * Will be called by SetupPage.
   */
  open fun afterSetup() {
    log.info { "Activating default plugins on first start..." }
    INITIAL_ACTIVATED_PLUGINS.forEach {
      storePluginToBeActivated(it, true)
    }
  }

  open fun initializeAllPluginsForUnitTest() {
    initializeActivePlugins(false)
  }

  private fun initializeActivePlugins(onlyConfiguredActive: Boolean) {
    val plugins = allPlugins
    val activatedPluginsByConfig = if (isFirstStart()) {
      INITIAL_ACTIVATED_PLUGINS
    } else {
      activatedPluginsFromConfiguration
    }
    for (plugin in plugins) {
      if (onlyConfiguredActive && !activatedPluginsByConfig.contains(plugin.info.id)) {
        log.info("Skipping not activated plugin '${plugin.info.name}'.")
        continue
      }
      log.info("Processing activated plugin: '${plugin.info.name}'.")
      activatePlugin(plugin)
    }
  }

  private fun activatePlugin(plugin: AbstractPlugin) {
    val factory = applicationContext.autowireCapableBeanFactory
    factory.initializeBean(plugin, plugin.id)
    factory.autowireBean(plugin)
    PluginsRegistry.instance().register(plugin)
    plugin.init()
    for (callback in afterCreatedActivePluginsCallback) {
      callback.call(plugin)
    }
  }

  open fun addExecuteAfterActivePluginCreated(run: PluginCallback) {
    afterCreatedActivePluginsCallback.add(run)
  }

  interface PluginCallback {
    fun call(plugin: AbstractPlugin?)
  }

  companion object {
    const val PLUGIN_BANKING_ID = "banking"
    const val PLUGIN_DATA_TRANSFER_ID = "datatransfer"
    const val PLUGIN_LICENSE_MANAGEMENT_ID = "licenseManagementPlugin"
    const val PLUGIN_LIQUIDITY_PLANNING_ID = "liquidityplanning"
    const val PLUGIN_MEMO_ID = "memo"
    const val PLUGIN_MERLIN_ID = "merlin"
    const val PLUGIN_SKILL_MATRIX_ID = "skillmatrix"
    const val PLUGIN_TODO_ID = "todo"

    val INITIAL_ACTIVATED_PLUGINS = listOf(
      PLUGIN_BANKING_ID,
      PLUGIN_DATA_TRANSFER_ID,
      PLUGIN_LICENSE_MANAGEMENT_ID,
      PLUGIN_LIQUIDITY_PLANNING_ID,
      PLUGIN_MEMO_ID,
      PLUGIN_MERLIN_ID,
      PLUGIN_SKILL_MATRIX_ID,
      PLUGIN_TODO_ID
    )

    private const val SYSTEM_PROPERTY_FIRST_START = "SYSTEM_PROPERTY_FIRST_START"

    private fun isFirstStart(): Boolean {
      return System.getProperty(SYSTEM_PROPERTY_FIRST_START) == "true"
    }

    fun registerFirstStart() {
      System.setProperty(SYSTEM_PROPERTY_FIRST_START, "true")
    }
  }
}
