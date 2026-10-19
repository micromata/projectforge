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


package org.projectforge.rest.feature

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.projectforge.NextMigration
import org.projectforge.feature.CoreFeatures
import org.projectforge.feature.FeatureDefinition
import org.projectforge.plugins.core.AbstractPlugin
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.util.ServiceLoader

/**
 * Every REST category and every page of projectforge-next belongs to a feature (see
 * [org.projectforge.feature.FeatureService]), to a plugin, or to the core (always reachable): otherwise it would
 * stay reachable with its feature switched off.
 *
 * Runs here and not in projectforge-rest because this module has all plugins on its class path.
 */
class FeatureCoverageTest {
    private val plugins: List<AbstractPlugin> = ServiceLoader.load(AbstractPlugin::class.java).toList()

    private val corePaths: Set<String> = CoreFeatures.definitions.flatMap { it.paths }.toSet()

    @Test
    fun allRestCategoriesBelongToAFeature() {
        val keysByClass = restControllers().associateWith { RestControllerPaths.categoryKeys(it) }
        val uncovered = sortedSetOf<String>()
        keysByClass.forEach { (clazz, keys) ->
            if (RestControllerPaths.pluginOf(clazz, plugins) != null) {
                return@forEach // Switched off with its plugin at least.
            }
            keys.filter { it !in corePaths && !isCore(it) }.forEach { uncovered.add("$it (${clazz.name})") }
        }
        Assertions.assertTrue(
            uncovered.isEmpty(),
            "REST categories without feature (add them to CoreFeatures or to FeaturePathIndex.CORE_REST*):\n" +
                    uncovered.joinToString("\n"),
        )
        val allKeys = keysByClass.values.flatten().toSet()
        val stale = (FeaturePathIndex.CORE_REST.map { "rs/$it" } + FeaturePathIndex.CORE_REST_PUBLIC.map { "rsPublic/$it" })
            .filter { it !in allKeys }
        Assertions.assertTrue(stale.isEmpty(), "Core REST categories without controller (remove them): $stale")
    }

    @Test
    fun allNextPagesBelongToAFeature() {
        val app = nextAppDir()
        Assumptions.assumeTrue(app != null, "projectforge-next/app not found.")
        val routes = routeDirs(File(app, "(authenticated)")) +
                routeDirs(File(app, "public")).map { "public/$it" }
        val owned = corePaths + plugins.flatMap { plugin ->
            plugin.featureNextRoutes.map { "${FeatureDefinition.NEXT_PREFIX}$it" }
        } + pluginNextRoutes()
        val uncovered = routes.filter { route ->
            route !in CORE_NEXT && owned.none { it == "next/$route" || it.startsWith("next/$route/") }
        }
        Assertions.assertTrue(
            uncovered.isEmpty(),
            "Pages of projectforge-next without feature (add them to CoreFeatures or to CORE_NEXT here): $uncovered",
        )
    }

    /** The next pages of the plugins' REST categories (see FeaturePathIndex.pluginPaths). */
    private fun pluginNextRoutes(): Set<String> {
        return restControllers().filter { RestControllerPaths.pluginOf(it, plugins) != null }
            .flatMap { RestControllerPaths.categoryKeys(it) }
            .filter { it.startsWith(FeatureDefinition.REST_PREFIX) }
            .map { "${FeatureDefinition.NEXT_PREFIX}${NextMigration.routeOrCategory(it.removePrefix(FeatureDefinition.REST_PREFIX))}" }
            .toSet()
    }

    private fun isCore(key: String): Boolean {
        val (root, category) = key.split('/', limit = 2)
        return if (root == "rs") category in FeaturePathIndex.CORE_REST else category in FeaturePathIndex.CORE_REST_PUBLIC
    }

    private fun restControllers(): List<Class<*>> {
        val provider = ClassPathScanningCandidateComponentProvider(false)
        provider.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
        val classes = provider.findCandidateComponents("org.projectforge").map { Class.forName(it.beanClassName) }
        Assertions.assertTrue(classes.size > 100, "Scanning found only ${classes.size} classes, broken class path?")
        return classes
    }

    private fun routeDirs(dir: File): List<String> {
        return dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith("[") && !it.name.startsWith("(") }
            ?.map { it.name }.orEmpty()
    }

    private fun nextAppDir(): File? {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val file = File(dir, "projectforge-next/app")
            if (file.isDirectory) {
                return file
            }
            dir = dir.parentFile
        }
        return null
    }

    companion object {
        /** Pages of the app itself, always reachable. */
        private val CORE_NEXT = setOf("changelog", "demo")
    }
}
