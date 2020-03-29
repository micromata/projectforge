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
import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserRight
import org.projectforge.business.user.UserXmlPreferencesDao
import org.projectforge.continuousdb.UpdateEntry
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter
import org.projectforge.registry.Registry
import org.projectforge.registry.RegistryEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractPlugin(val info: PluginInfo) {
    @Autowired
    protected lateinit var applicationContext: ApplicationContext

    @Autowired
    protected lateinit var databaseService: DatabaseService

    protected lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Autowired
    protected lateinit var accessChecker: AccessChecker

    @Autowired
    protected lateinit var userRights: UserRightService

    val resourceBundleNames = mutableListOf<String>()

    /**
     * @return the initialized
     */
    var initialized = false
        private set

    fun init() {
        synchronized(initializedPlugins) {
            if (initializedPlugins.contains(this.javaClass) || this.initialized) {
                log.warn("Ignoring multiple initialization of plugin.")
                return
            }
            this.initialized = true
            initializedPlugins.add(this.javaClass)
            log.info("Initializing plugin: $javaClass")
            initialize()
        }
    }

    /**
     * Is called on initialization of the plugin by the method [.init].
     */
    protected abstract fun initialize()

    /**
     * @param resourceBundleName
     * @return this for chaining.
     */
    protected fun addResourceBundle(resourceBundleName: String): AbstractPlugin {
        resourceBundleNames.add(resourceBundleName)
        return this
    }

    /**
     * @param daoClassType The dao object type.
     * @param baseDao      The dao itself.
     * @param i18nPrefix   The prefix for i18n keys.
     * @return New RegistryEntry.
     */
    protected fun register(daoClassType: Class<out BaseDao<*>?>?,
                           baseDao: BaseDao<*>?,
                           i18nPrefix: String?): RegistryEntry {
        return register(info.id, daoClassType, baseDao, i18nPrefix)
    }

    /**
     * @param id           The unique dao id.
     * @param daoClassType The dao object type.
     * @param baseDao      The dao itself.
     * @param i18nPrefix   The prefix for i18n keys.
     * @return New RegistryEntry.
     */
    protected fun register(id: String, daoClassType: Class<out BaseDao<*>?>?,
                           baseDao: BaseDao<*>?,
                           i18nPrefix: String?): RegistryEntry {
        requireNotNull(baseDao) {
            "$id: Dao object is null. May-be the developer forgots to initialize it in pluginContext.xml or the setter method is not given in the main plugin class!"
        }
        val entry = RegistryEntry(id, daoClassType, baseDao, i18nPrefix)
        register(entry)
        return entry
    }

    /**
     * Registers the given entry.
     *
     * @param entry
     * @return The registered registry entry for chaining.
     * @see Registry.register
     */
    protected fun register(entry: RegistryEntry): RegistryEntry {
        Validate.notNull(entry)
        Registry.getInstance().register(entry)
        return entry
    }

    /**
     * Registers a right which is responsible for the access management.
     *
     * @param right
     * @return this for chaining.
     */
    protected fun registerRight(right: UserRight?): AbstractPlugin {
        userRights.addRight(right)
        return this
    }

    /**
     * Override this method if an update entry for initialization does exist. This will be called, if the plugin runs the
     * first time.
     *
     * @return null at default.
     *
     */
    @get:Deprecated("Since version 6.18.0 please use flyway db migration.")
    open val initializationUpdateEntry: UpdateEntry?
        get() = null

    /**
     * Override this method if update entries does exist for this plugin.
     *
     * @return null at default.
     */
    @get:Deprecated("Since version 6.18.0 please use flyway db migration.")
    open val updateEntries: List<UpdateEntry>?
        get() = null

    @Deprecated("")
    fun onBeforeRestore(xstreamSavingConverter: XStreamSavingConverter, obj: Any) {
    }

    @Deprecated("")
    fun onAfterRestore(xstreamSavingConverter: XStreamSavingConverter, obj: Any,
                       newId: Serializable?) {
    }

    companion object {
        private val initializedPlugins: MutableSet<Class<*>> = HashSet()
    }
}
