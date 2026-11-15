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

package org.projectforge.framework.configuration.properties

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.env.PropertiesPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Reloads projectforge.properties after the properties editor saved it: replaces its property source in the
 * Environment and lets every [ReloadableProperties] bean read its parameters again. All other parameters keep the
 * value the running system started with — they were copied at startup (`@Value` fields, the database pool, filters,
 * crons, …) and need a restart, so [activeValue] still reports that value for them.
 *
 * Nothing is reloaded if the file didn't exist at startup (Spring then has no property source for it).
 */
@Service
class ProjectForgePropertiesReloader(
    private val environment: ConfigurableEnvironment,
    private val reloadables: List<ReloadableProperties>,
) {
    /** The property source of projectforge.properties as loaded at startup, kept on the first reload. */
    @Volatile
    private var startupSource: PropertySource<*>? = null

    /** Whether [key] takes effect without a restart. */
    fun isLive(key: String): Boolean = reloadables.any { it.isReloadable(key) }

    /**
     * @return false if nothing could be reloaded (no property source of [file]) or a bean failed to reload, so a
     * restart is needed.
     */
    @Synchronized
    fun reload(file: File): Boolean {
        val current = findSource(file)
        if (current == null) {
            log.info { "No property source of '${file.absolutePath}' (didn't exist at startup), nothing reloaded." }
            return false
        }
        val replacement = load(file, current.name)
        if (startupSource == null) {
            startupSource = current
        }
        environment.propertySources.replace(current.name, replacement)
        var success = true
        reloadables.forEach { bean ->
            try {
                bean.reload(environment)
            } catch (ex: Exception) {
                log.error(ex) { "Error while reloading the parameters of ${bean::class.simpleName}: ${ex.message}" }
                success = false
            }
        }
        log.info { "projectforge.properties reloaded, applied: ${reloadables.flatMap { it.reloadableKeys }.joinToString()}." }
        return success
    }

    /**
     * The value the running system uses: the reloaded one for a live key, else the one of the startup (as long as no
     * restart happened, the beans still hold it).
     */
    fun activeValue(key: String): String? {
        val startup = startupSource
        if (startup == null || isLive(key)) {
            return runCatching { environment.getProperty(key) }.getOrNull()
        }
        val raw = environment.propertySources.asSequence()
            .filter { it.name != CONFIGURATION_PROPERTIES_SOURCE }
            .map { if (it.name == startup.name) startup else it }
            .firstNotNullOfOrNull { runCatching { it.getProperty(key) }.getOrNull() }
            ?.toString() ?: return null
        return runCatching { environment.resolvePlaceholders(raw) }.getOrDefault(raw)
    }

    private fun findSource(file: File): PropertySource<*>? {
        val paths = setOf(file.absolutePath, runCatching { file.canonicalPath }.getOrDefault(file.absolutePath))
        return environment.propertySources.firstOrNull { source -> paths.any { source.name.contains(it) } }
    }

    private fun load(file: File, name: String): PropertySource<*> {
        val loaded = if (file.exists()) {
            PropertiesPropertySourceLoader().load(name, FileSystemResource(file)).firstOrNull()
        } else null
        @Suppress("UNCHECKED_CAST")
        return when {
            loaded == null -> MapPropertySource(name, emptyMap())
            loaded.name == name -> loaded
            // Under the name of the replaced one, so the next reload finds it again.
            else -> MapPropertySource(name, loaded.source as Map<String, Any>)
        }
    }

    companion object {
        /** Spring's view on all sources (relaxed names), not a source of its own. */
        private const val CONFIGURATION_PROPERTIES_SOURCE = "configurationProperties"
    }
}
