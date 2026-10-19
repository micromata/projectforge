/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.feature

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Functions of ProjectForge can be switched off: their menu entries are gone, their REST endpoints and next pages
 * answer 404 (`FeatureFilter`, projectforge-rest). One feature per menu entry, see [FeatureDefinition].
 *
 * Configured by `projectforge.features.enabled` (allowlist, `all` by default) and `projectforge.features.disabled`
 * (denylist, always wins). Ids are the [MenuItemDefId] names (`ADDRESS_LIST`), the ids of the plugins'
 * menu entries (`toDo`, `DataTransferPersonalBox`) and the plugin ids (`datatransfer`). A main menu group
 * (`MISC`) or a plugin id stands for all of its entries. A plugin which isn't active is switched off as well.
 *
 * On the gateway (`projectforge.gateway.enabled`) only the data transfer is enabled by default (without the
 * personal boxes, the areas are administered on the main instance).
 *
 * This is no permission system: the access checks of the functions remain as they are.
 */
@Service
open class FeatureService {
    @Value("\${projectforge.features.enabled:#{null}}")
    private var enabledConfig: String? = null

    @Value("\${projectforge.features.disabled:#{null}}")
    private var disabledConfig: String? = null

    @Value("\${projectforge.gateway.enabled:false}")
    private var gatewayMode: Boolean = false

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    // Got lazily: the MenuCreator asks this service while building the menu.
    private val menuCreator by lazy { applicationContext.getBean(MenuCreator::class.java) }

    private val pluginAdminService by lazy { applicationContext.getBean(PluginAdminService::class.java) }

    val toggles: FeatureToggles by lazy {
        FeatureToggles(
            enabledIds = FeatureToggles.parse(enabledConfig ?: if (gatewayMode) GATEWAY_ENABLED else FeatureToggles.ALL),
            disabledIds = FeatureToggles.parse(disabledConfig ?: if (gatewayMode) GATEWAY_DISABLED else "").orEmpty(),
        )
    }

    private class Index(
        val modificationCount: Int,
        /** Feature id -> ancestors in the menu and its plugin. */
        val parents: Map<String, List<String>>,
        /** Feature id -> descendants in the menu, or the menu entries of a plugin. */
        val descendants: Map<String, Set<String>>,
        val definitions: List<FeatureDefinition>,
    )

    @Volatile
    private var index: Index? = null

    /**
     * The features of all menu entries: core and the ones of the active plugins (the REST endpoints of a plugin
     * are assigned to the plugin's feature by `FeaturePathIndex`).
     */
    val definitions: List<FeatureDefinition>
        get() = getIndex().definitions

    val pluginIds: List<String>
        get() = pluginAdminService.availablePlugins.map { it.id }

    fun isEnabled(defId: MenuItemDefId): Boolean {
        return isEnabled(defId.id)
    }

    fun isEnabled(menuItemDef: MenuItemDef): Boolean {
        return isEnabled(menuItemDef.id)
    }

    /**
     * A feature is on, if it's listed (or one of its ancestors or its plugin) in the allowlist, but neither it nor one
     * of them in the denylist, and its plugin is active. A menu group is on, if one of its entries is on.
     *
     * @param id The feature id, null is always on.
     */
    fun isEnabled(id: String?): Boolean {
        id ?: return true
        val index = getIndex()
        if (isEnabled(id, index)) {
            return true
        }
        return index.descendants[id]?.any { isEnabled(it, index) } == true
    }

    private fun isEnabled(id: String, index: Index): Boolean {
        if (id in CoreFeatures.ALWAYS_ENABLED) {
            return true
        }
        val pluginId = if (pluginIds.contains(id)) id else menuCreator.getPluginId(id)
        if (pluginId != null && !pluginAdminService.isActive(pluginId)) {
            return false
        }
        return toggles.isEnabled(listOf(id) + index.parents[id].orEmpty())
    }

    /** All known feature ids which are switched off (menu entries, groups and plugins). */
    fun disabledFeatureIds(): List<String> {
        val index = getIndex()
        val ids = index.definitions.map { it.id } + index.parents.keys + pluginIds
        return ids.distinct().filter { !isEnabled(it) }.sorted()
    }

    private fun getIndex(): Index {
        val count = menuCreator.modificationCount
        index?.let { if (it.modificationCount == count) return it }
        val parents = mutableMapOf<String, List<String>>()
        val descendants = mutableMapOf<String, MutableSet<String>>()
        val definitions = CoreFeatures.definitions.toMutableList()
        menuCreator.forEachMenuItemDef { menuItemDef, ancestorIds ->
            val pluginId = menuCreator.getPluginId(menuItemDef.id)
            val chain = if (pluginId != null && pluginId != menuItemDef.id) ancestorIds + pluginId else ancestorIds
            parents[menuItemDef.id] = chain
            chain.forEach { descendants.getOrPut(it) { mutableSetOf() }.add(menuItemDef.id) }
            if (menuItemDef.menuItemDefId == null) {
                definitions.add(FeatureDerivation.derive(menuItemDef.id, menuItemDef.url, menuItemDef.featureRestPaths))
            }
        }
        return Index(count, parents, descendants, definitions).also { index = it }
    }

    @EventListener(ApplicationReadyEvent::class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    open fun logConfiguration() {
        if (!toggles.isRestricted) {
            return
        }
        val index = getIndex()
        val known = index.definitions.map { it.id }.toSet() + index.parents.keys + pluginIds + CoreFeatures.ALWAYS_ENABLED
        val unknown = (toggles.enabledIds.orEmpty() + toggles.disabledIds).filter { it !in known }
        if (unknown.isNotEmpty()) {
            log.warn { "Unknown feature ids in projectforge.features.* (or menu ids of plugins not installed): ${unknown.joinToString()}" }
        }
        log.info { "Features switched off: ${disabledFeatureIds().joinToString()}" }
    }

    companion object {
        /** The data transfer only: the default of the gateway. */
        const val GATEWAY_ENABLED = PluginAdminService.PLUGIN_DATA_TRANSFER_ID

        /** No personal boxes on the gateway. */
        const val GATEWAY_DISABLED = "DataTransferPersonalBox"
    }
}
