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

package org.projectforge.development

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

/**
 * The checks of the api contracts of the apps against this server, shared by [AppApiContractTest] (the published
 * Flutter app) and [AppTsApiContractTest] (projectforge-app).
 */
internal class ApiContractChecks(private val mapper: ObjectMapper) {
    /** True if the controller maps the call (`GET /rs/x/{id}`). */
    fun hasEndpoint(controller: Class<*>, call: String): Boolean {
        val (verb, path) = call.split(' ', limit = 2)
        return mappingsOf(controller).any { (v, p) -> (v == null || v == verb) && matches(p, path) }
    }

    /** True if the type has the property of the path; `[]` steps into the elements of a collection. */
    fun exists(type: JavaType, segments: List<String>, deserialize: Boolean = false): Boolean {
        if (segments.isEmpty()) {
            return true
        }
        val name = segments.first().removeSuffix("[]")
        val property = propertiesOf(type, deserialize).firstOrNull { it.name == name } ?: return false
        var next = property.primaryType
        if (segments.first().endsWith("[]")) {
            next = next.contentType ?: return false
        }
        return exists(next, segments.drop(1), deserialize)
    }

    /** The json property names of the class, as Jackson writes ([deserialize] false) or reads them. */
    fun propertyNames(type: Class<*>, deserialize: Boolean = false): Set<String> =
        propertiesOf(mapper.constructType(type), deserialize).map { it.name }.toSet()

    private fun propertiesOf(type: JavaType, deserialize: Boolean) = if (deserialize) {
        mapper.deserializationConfig.introspect(type).findProperties()
    } else {
        mapper.serializationConfig.introspect(type).findProperties()
    }

    /** The verb (null: any, a mapping without method) and full path of every request mapping of the controller. */
    private fun mappingsOf(controller: Class<*>): List<Pair<String?, String>> {
        val base = pathOf(AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java))
        return controller.methods.flatMap { method ->
            val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
                ?: return@flatMap emptyList()
            val verbs = mapping.method.map<RequestMethod, String?> { it.name }.ifEmpty { listOf(null) }
            val paths = (mapping.path.toList() + mapping.value.toList()).distinct().ifEmpty { listOf("") }
            verbs.flatMap { verb -> paths.map { verb to "$base/$it".replace(Regex("/+"), "/").trimEnd('/') } }
        }
    }

    private fun pathOf(mapping: RequestMapping?): String =
        mapping?.let { (it.path.toList() + it.value.toList()).firstOrNull() } ?: ""

    /** A `{id}` segment in either path stands for any segment. */
    private fun matches(mappingPath: String, callPath: String): Boolean {
        val mapping = mappingPath.split('/')
        val call = callPath.split('/')
        return mapping.size == call.size && mapping.zip(call).all { (m, c) -> m == c || isVariable(m) || isVariable(c) }
    }

    private fun isVariable(segment: String) = segment.startsWith("{") && segment.endsWith("}")
}
