/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*

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

    @Autowired
    private lateinit var databaseService: DatabaseService

    /**
     * All plugins registered as Spring services/components. External plugins should be declared as Spring services.
     * Built-in plugins will also be found by [ServiceLoader.load].
     */
    @Autowired
    private lateinit var registeredSpringPlugins: List<PFPluginService>

    private val afterCreatedActivePluginsCallback: MutableList<PluginCallback> = ArrayList()

    /**
     * List of all activated plugins.
     */
    open val activePlugins: List<AbstractPlugin>
        get() {
            val pluginsRegistry = PluginsRegistry.instance()
            return pluginsRegistry.plugins
        }

    /**
     * All installed plugin services (activated as well as not activated ones).
     *
     * @return the plugin services
     */
    open val availablePlugins: List<AvailablePlugin>
        get() {
            // Set for not adding plugins twice:
            val pluginServiceSet = mutableSetOf<PFPluginService>()
            val serviceLoader = ServiceLoader.load(PFPluginService::class.java)
            serviceLoader.forEach { pluginServiceSet.add(it) }
            registeredSpringPlugins.forEach { pluginServiceSet.add(it) }

            val availablePlugins: MutableList<AvailablePlugin> = ArrayList()
            val activated = readActivatedPluginsFromConfiguration
            pluginServiceSet.forEach { pluginService ->
                val ap = AvailablePlugin(pluginService, activated.contains(pluginService.pluginId))
                availablePlugins.add(ap)
            }
            return availablePlugins
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
        val activated = readActivatedPluginsFromConfiguration
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
            configuration.global = param.isGlobal
        }
        configuration.stringValue = sval
        configurationDao.saveOrUpdate(configuration)
        GlobalConfiguration.getInstance().forceReload()
        return false
    }

    /**
     * Get activated plugins from configuraton.
     *
     * @return the activated plugins as list of id strings.
     * @see [GlobalConfiguration.getStringValue]
     */
    private val readActivatedPluginsFromConfiguration: MutableList<String>
        get() {
            val plugins = GlobalConfiguration.getInstance().getStringValue(ConfigurationParam.PLUGIN_ACTIVATED)
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

    open fun initializeAllPluginsForUnitTest() {
        initializeActivePlugins(false)
    }

    private fun initializeActivePlugins(onlyConfiguredActive: Boolean) {
        val plugins = availablePlugins
        for (plugin in plugins) {
            if (onlyConfiguredActive && !plugin.isActivated) {
                log.info("Skipping not activated plugin '${plugin.projectForgePluginService.pluginName}'.")
                continue
            }
            log.info("Processing activated plugin activated: '${plugin.projectForgePluginService.pluginName}'.")
            activatePlugin(plugin.projectForgePluginService)
        }
    }

    private fun activatePlugin(projectForgePluginService: PFPluginService) {
        val plugin = projectForgePluginService.createPluginInstance()
        val factory = applicationContext.autowireCapableBeanFactory
        factory.initializeBean(plugin, projectForgePluginService.pluginId)
        factory.autowireBean(plugin)
        PluginsRegistry.instance().register(plugin)
        plugin.init()
        setSystemUpdater(plugin)
        for (callback in afterCreatedActivePluginsCallback) {
            callback.call(plugin)
        }
    }

    open fun addExecuteAfterActivePluginCreated(run: PluginCallback) {
        afterCreatedActivePluginsCallback.add(run)
    }

    private fun setSystemUpdater(plugin: AbstractPlugin) {
        val systemUpdater = databaseService.systemUpdater
        val updateEntry = plugin.initializationUpdateEntry
        if (updateEntry != null) {
            if (!updateEntry.isInitial) {
                log.error("The given UpdateEntry returned by plugin.getInitializationUpdateEntry() is not initial! Please use constructor without parameter version: ${plugin.javaClass}")
            }
            systemUpdater.register(updateEntry)
        }
        val updateEntries = plugin.updateEntries
        if (updateEntries != null) {
            for (entry in updateEntries) {
                if (entry.isInitial) {
                    log.error("The given UpdateEntry returned by plugin.getUpdateEntries() is initial! Please use constructor with parameter version: ${plugin.javaClass}: ${entry.description}")
                }
            }
            systemUpdater.register(updateEntries)
        }
    }

    interface PluginCallback {
        fun call(plugin: AbstractPlugin?)
    }
}
