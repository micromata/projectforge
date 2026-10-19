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

import org.projectforge.feature.FeatureDefinition
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.core.AbstractPlugin
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.RequestMapping

/**
 * The REST categories of `@RestController` classes, as path keys of [FeatureDefinition] (`rs/address`,
 * `rsPublic/systemStatus`). Shared by [FeaturePathIndex] and `FeatureCoverageTest`.
 */
object RestControllerPaths {
    /**
     * @return The first segment below `/rs` or `/rsPublic` of each mapping of the class (class level combined with
     * method level), as path key. Others (`/error`, `/api/...`) are left out.
     */
    fun categoryKeys(clazz: Class<*>): Set<String> {
        val classPaths = paths(AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping::class.java))
            .ifEmpty { listOf("") }
        val methodPaths = ReflectionUtils.getUniqueDeclaredMethods(clazz)
            .flatMap { paths(AnnotatedElementUtils.findMergedAnnotation(it, RequestMapping::class.java)) }
            .ifEmpty { listOf("") }
        return classPaths.flatMap { classPath -> methodPaths.map { "$classPath/$it" } }
            .mapNotNull { categoryKey(it) }
            .toSet()
    }

    /** `/rs/address/list` -> `rs/address`. */
    fun categoryKey(path: String): String? {
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size < 2 || segments[0] !in ROOTS || segments[1].contains('{')) {
            return null
        }
        return "${segments[0]}/${segments[1]}"
    }

    /**
     * @return The plugin the class belongs to (by package), or null for a core class.
     */
    fun pluginOf(clazz: Class<*>, plugins: Collection<AbstractPlugin>): AbstractPlugin? {
        val packageName = clazz.packageName
        return plugins.filter { plugin ->
            val pluginPackage = plugin.javaClass.packageName
            packageName == pluginPackage || packageName.startsWith("$pluginPackage.")
        }.maxByOrNull { it.javaClass.packageName.length }
    }

    private fun paths(mapping: RequestMapping?): List<String> {
        mapping ?: return emptyList()
        return (mapping.value.toList() + mapping.path.toList()).distinct()
    }

    private val ROOTS = setOf(RestPaths.REST, RestPaths.REST_PUBLIC)
}
