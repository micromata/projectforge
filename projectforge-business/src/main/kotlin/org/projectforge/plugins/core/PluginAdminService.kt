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

import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.GlobalConfiguration
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.database.DatabaseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*

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

    val activePlugin: List<AbstractPlugin>
        get() {
            val pluginsRegistry = PluginsRegistry.instance()
            return pluginsRegistry.plugins
        }

    /**
     * All installed plugin services.
     *
     * @return the plugin services
     */
    val availablePlugins: List<AvailablePlugin>
        get() {
            val activated: Set<String?> = activePlugins
            val ls = ServiceLoader.load(PFPluginService::class.java)
            val ret: MutableList<AvailablePlugin> = ArrayList()
            for (e in ls) {
                val ap = AvailablePlugin(e, activated.contains(e.pluginId))
                ret.add(ap)
            }
            return ret
        }

    /**
     * Store a plugin as activated.
     * read LocalSettings pf.plugins.active. If not defined, uses ConfigurationParam.
     *
     * @param id
     * @param activate
     * @return the active plugins
     */
    fun storePluginToBeActivated(id: String?, activate: Boolean): Boolean {
        val active = activePlugins
        if (activate) {
            active.add(id)
        } else {
            active.remove(id)
        }
        val sval = StringUtils.join(active, ",")
        var configuration = configurationDao!!.getEntry(ConfigurationParam.PLUGIN_ACTIVATED)
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
     * read LocalSettings pf.plugins.active. If not defined, uses ConfigurationParam.
     *
     * @return the active plugins
     */
    val activePlugins: MutableSet<String?>
        get() {
            val activateds = GlobalConfiguration.getInstance().getStringValue(ConfigurationParam.PLUGIN_ACTIVATED)
            var sa = arrayOfNulls<String>(0)
            if (!StringUtils.isBlank(activateds)) {
                sa = StringUtils.split(activateds, ", ")
            }
            return TreeSet(Arrays.asList(*sa))
        }

    /**
     * Will be active plugins
     */
    fun initializeActivePlugins() {
        initializeActivePlugins(true)
    }

    fun initializeAllPluginsForUnittest() {
        initializeActivePlugins(false)
    }

    protected fun initializeActivePlugins(onlyConfiguredActive: Boolean) {
        val plugins = availablePlugins
        for (plugin in plugins) {
            LOG.info("Plugin found: " + plugin.projectForgePluginService.pluginName)
            if (onlyConfiguredActive && !plugin.isActivated) {
                continue
            }
            activatePlugin(plugin.projectForgePluginService)
        }
    }

    protected fun activatePlugin(projectForgePluginService: PFPluginService) {
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
        LOG.info("Plugin activated: " + projectForgePluginService.pluginId)
    }

    fun addExecuteAfterActivePluginCreated(run: PluginCallback) {
        afterCreatedActivePluginsCallback.add(run)
    }

    private fun setSystemUpdater(plugin: AbstractPlugin) {
        val systemUpdater = databaseService.systemUpdater
        val updateEntry = plugin.initializationUpdateEntry
        if (updateEntry != null) {
            if (!updateEntry.isInitial) {
                LOG.error("The given UpdateEntry returned by plugin.getInitializationUpdateEntry() is not initial! Please use constructor without parameter version: "
                        + plugin.javaClass)
            }
            systemUpdater.register(updateEntry)
        }
        val updateEntries = plugin.updateEntries
        if (updateEntries != null) {
            for (entry in updateEntries) {
                if (entry.isInitial) {
                    LOG.error(
                            "The given UpdateEntry returned by plugin.getUpdateEntries() is initial! Please use constructor with parameter version: "
                                    + plugin.javaClass
                                    + ": "
                                    + entry.description)
                }
            }
            systemUpdater.register(updateEntries)
        }
    }

    interface PluginCallback {
        fun call(plugin: AbstractPlugin?)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PluginAdminService::class.java)
    }
}
