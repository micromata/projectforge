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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.rest.core.AccessChecked
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

/**
 * Every endpoint of a `@RestController` has to declare its access check with [AccessChecked] (see there).
 *
 * Legacy endpoints that predate the annotation are listed in [BASELINE_FILE] as `class#method`. The baseline
 * may only shrink: a new endpoint missing the annotation fails, and so does a baseline entry that is now
 * annotated or gone (remove it from the file then).
 *
 * Runs here and not in projectforge-rest because this module has all plugins on its class path.
 */
class RestEndpointAccessCheckTest {
    @Test
    fun allEndpointsDeclareAccessCheck() {
        val missing = collectEndpointsWithoutAccessCheck()
        val baseline = readBaseline()
        val unexpected = missing - baseline
        val stale = baseline - missing
        Assertions.assertTrue(
            unexpected.isEmpty(),
            "REST endpoints without @AccessChecked (check the access, then annotate how it is done):\n" +
                    unexpected.joinToString("\n"),
        )
        Assertions.assertTrue(
            stale.isEmpty(),
            "Entries of $BASELINE_FILE that are annotated or don't exist anymore (remove them):\n" +
                    stale.joinToString("\n"),
        )
    }

    private fun collectEndpointsWithoutAccessCheck(): Set<String> {
        val provider = ClassPathScanningCandidateComponentProvider(false)
        provider.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
        val classes = mutableSetOf<Class<*>>()
        provider.findCandidateComponents("org.projectforge").forEach { definition ->
            // The endpoints a controller inherits are declared (and have to be annotated) in its super classes.
            var clazz: Class<*>? = Class.forName(definition.beanClassName)
            while (clazz != null && clazz != Any::class.java) {
                classes.add(clazz)
                clazz = clazz.superclass
            }
        }
        Assertions.assertTrue(classes.size > 100, "Scanning found only ${classes.size} classes, broken class path?")
        val result = sortedSetOf<String>()
        classes.forEach { clazz ->
            clazz.declaredMethods
                .filter { !it.isSynthetic && !it.isBridge && isEndpoint(it) && !isCovered(it) }
                .forEach { result.add("${clazz.name}#${it.name}") }
        }
        return result
    }

    /** Also an override of an inherited endpoint, which Spring maps as well. */
    private fun isEndpoint(method: Method): Boolean {
        return AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java) != null
    }

    /**
     * Annotated itself (or the method it overrides), or declared in a class annotated as a whole, or an override
     * of a method of such a class.
     */
    private fun isCovered(method: Method): Boolean {
        if (AnnotatedElementUtils.findMergedAnnotation(method, AccessChecked::class.java) != null) {
            return true
        }
        var clazz: Class<*>? = method.declaringClass
        while (clazz != null && clazz != Any::class.java) {
            if (clazz.getDeclaredAnnotation(AccessChecked::class.java) != null && declares(clazz, method)) {
                return true
            }
            clazz = clazz.superclass
        }
        return false
    }

    private fun declares(clazz: Class<*>, method: Method): Boolean {
        return runCatching { clazz.getDeclaredMethod(method.name, *method.parameterTypes) }.isSuccess
    }

    private fun readBaseline(): Set<String> {
        val stream = javaClass.getResourceAsStream("/$BASELINE_FILE")
            ?: throw IllegalStateException("$BASELINE_FILE not found on the class path.")
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSortedSet()
        }
    }

    companion object {
        private const val BASELINE_FILE = "rest-endpoint-access-baseline.txt"
    }
}
