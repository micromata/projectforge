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
import org.apache.commons.lang3.Validate
import org.flywaydb.core.Flyway
import org.projectforge.business.user.UserRight
import org.projectforge.business.user.UserXmlPreferencesDao
import org.projectforge.common.DatabaseDialect
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.registry.Registry
import org.projectforge.registry.RegistryEntry
import org.projectforge.security.My2FAShortCut
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import java.io.Serializable
import javax.sql.DataSource
import kotlin.reflect.KFunction

private val log = KotlinLogging.logger {}

/**
 * @param pluginId See. [PluginInfo]
 * @param pluginName See. [PluginInfo]
 * @param pluginDescription See. [PluginInfo]
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractPlugin(pluginId: String, pluginName: String, pluginDescription: String) {
  @Autowired
  protected lateinit var applicationContext: ApplicationContext

  @Autowired
  protected lateinit var databaseService: DatabaseService

  protected lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

  @Autowired
  protected lateinit var accessChecker: AccessChecker

  @Autowired
  protected lateinit var dataSource: DataSource

  @Autowired
  protected lateinit var userRights: UserRightService

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  val id: String
    get() = info.id

  val info = PluginInfo(pluginId, this::class.java, pluginName, pluginDescription)

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
      if (!internalJunitTestMode) { // Don't init flyway: schema-update is auto on test cases.
        flywayInit()
      }
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
  protected fun register(
    daoClassType: Class<out BaseDao<*>?>?,
    baseDao: BaseDao<*>?,
    i18nPrefix: String?
  ): RegistryEntry {
    return register(info.id, daoClassType, baseDao, i18nPrefix)
  }

  /**
   * @param id           The unique dao id.
   * @param daoClassType The dao object type.
   * @param baseDao      The dao itself.
   * @param i18nPrefix   The prefix for i18n keys.
   * @return New RegistryEntry.
   */
  protected fun register(
    id: String, daoClassType: Class<out BaseDao<*>?>?,
    baseDao: BaseDao<*>?,
    i18nPrefix: String?
  ): RegistryEntry {
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

  fun registerShortCutValues(shortCut: My2FAShortCut, vararg values: String) {
    pluginAdminService.projectForge2FAInitialization.registerShortCutValues(shortCut, *values)
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun registerShortCutClasses(shortCut: My2FAShortCut, vararg restClasses: Class<*>) {
      pluginAdminService.projectForge2FAInitialization.registerShortCutClasses(shortCut, *restClasses)
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun registerShortCutMethods(shortCut: My2FAShortCut, restClass: Class<*>, vararg methods: KFunction<*>) {
      pluginAdminService.projectForge2FAInitialization.registerShortCutMethods(shortCut, restClass, *methods)
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun registerShortCutMethods(shortCut: My2FAShortCut, vararg methods: KFunction<*>) {
      pluginAdminService.projectForge2FAInitialization.registerShortCutMethods(shortCut, *methods)
  }

  /**
   * Initializes Flyway mechanism, if any flyway script or migration class is found.
   * Searches for classpath in [buildFlywayClasspath].
   */
  protected open fun flywayInit() {
    val flywayClasspath = buildFlywayClasspath()
    if (flywayClasspath.isEmpty()) {
      if (this::class.java.`package`.name.contains("org.projectforge.plugins"))
        log.info { "No flyway scripts found, so no automatically database initialization and migration is done by plugin '$id' (might be OK)." }
      return
    }
    log.info("Initializing flyway with locations for plugin '$id': ${flywayClasspath.joinToString(",") { it }}")
    val flyway = Flyway.configure()
      .dataSource(dataSource)
      .table("t_flyway_${id.lowercase()}_schema_version")
      .locations(*flywayClasspath)
      .baselineVersion(flywayBaselineVersion)
      .baselineOnMigrate(true)
      .load()
    flyway.migrate()
  }

  protected open val flywayBaselineVersion: String
    get() = "0.1"


  /**
   * Tries to find flyway init and migration scripts (sql as well as Java/Kotlin) in classpath:
   * '/flyway/${plugin.id}/init/common', '/flyway/${plugin.id}/init/${db-vendor}',
   * '/flyway/${plugin.id}/migrate/common', '/flyway/${plugin.id}/migrate/${db-vendor}' and
   * '/${plugin.package}/flyway/dbmigration'
   */
  protected open fun buildFlywayClasspath(): Array<String> {
    val vendor = DatabaseDialect.getFlywayVendorName(dataSource.connection.metaData.databaseProductName)
    // Class.packageName since java 9, but want to be compatible with Java 8:
    val packageLocation = this::class.java.`package`.name.replace('.', '/')
    return arrayOf(
      "/flyway/$id/init/common",
      "/flyway/$id/init/$vendor",
      "/flyway/$id/migrate/common",
      "/flyway/$id/migrate/$vendor",
      "/$packageLocation/flyway/dbmigration"
    )
      .filter { this::class.java.getResource(it) != null } // Avoids flyway warnings on non-existent resources.
      .map { "classpath:$it" }
      .toTypedArray()
  }

  open fun handleFavoriteMenu(menu: Menu, allMenuItems: List<MenuItem>) {
  }

  @Deprecated("")
  fun onBeforeRestore(xstreamSavingConverter: XStreamSavingConverter, obj: Any) {
  }

  @Deprecated("")
  fun onAfterRestore(
    xstreamSavingConverter: XStreamSavingConverter, obj: Any,
    newId: Serializable?
  ) {
  }

  companion object {
    private val initializedPlugins: MutableSet<Class<*>> = HashSet()

    @JvmStatic
    var internalJunitTestMode: Boolean = false
  }
}
