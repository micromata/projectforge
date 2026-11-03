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
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.File

/**
 * The contract between the published app and this server: `projectforge-flutter/test/contract/app-api-contract.json`
 * names what the app reads. This test fails
 * - if the server no longer delivers a field or an endpoint the contract names (a break for the published app: keep it
 *   `@Deprecated` and list it in `dev-docs/app-api-deprecations.md`, or raise `AppCompatibility.MIN_APP_VERSION`), and
 * - if a Dart file of the contract reads a key the contract doesn't name (the contract would be incomplete).
 *
 * Rules: `dev-docs/flutter-client.md`, "Changing an api the app uses".
 */
class AppApiContractTest {
    private val rootDir = GenerateChangelogMain.resolveRootDir()
    private val appDir = File(rootDir, "projectforge-flutter")
    private val mapper = ObjectMapper().registerKotlinModule()
    private val contracts = mapper.readTree(File(appDir, "test/contract/app-api-contract.json"))["contracts"]

    private fun JsonNode.texts(field: String): List<String> = this[field]?.map { it.asText() } ?: emptyList()

    private fun endpoints(contract: JsonNode): List<JsonNode> = contract["endpoints"]?.toList() ?: emptyList()

    /** Classes the app reads without an endpoint of their own, e.g. the answers of failed requests. */
    private fun types(contract: JsonNode): List<JsonNode> = contract["types"]?.toList() ?: emptyList()

    @Test
    fun `the contract exists and names fields`() {
        assertTrue(contracts.size() > 0, "No contract found.")
        contracts.forEach { contract ->
            val all = endpoints(contract) + types(contract)
            assertTrue(
                all.isNotEmpty() || contract.texts("ignoreKeys").isNotEmpty(),
                "${contract["name"].asText()} has nothing."
            )
            assertTrue(
                all.isEmpty() || all.any { it.texts("reads").isNotEmpty() || it.texts("sends").isNotEmpty() },
                "${contract["name"].asText()} reads and sends nothing."
            )
        }
    }

    @Test
    fun `the server delivers every field the app reads`() {
        val missing = mutableListOf<String>()
        contracts.forEach { contract ->
            endpoints(contract).filter { it.has("answer") }.forEach { endpoint ->
                val root = mapper.constructType(Class.forName(endpoint["answer"].asText()))
                endpoint.texts("reads").forEach { path ->
                    if (!exists(root, path.split('.'))) {
                        missing.add("${endpoint["call"].asText()}: $path")
                    }
                }
            }
            types(contract).forEach { type ->
                val root = mapper.constructType(Class.forName(type["class"].asText()))
                type.texts("reads").forEach { path ->
                    if (!exists(root, path.split('.'))) {
                        missing.add("${type["name"].asText()}: $path")
                    }
                }
            }
        }
        assertTrue(
            missing.isEmpty(),
            "The published app reads fields the server no longer delivers (keep them deprecated or raise " +
                    "MIN_APP_VERSION, see dev-docs/flutter-client.md): $missing"
        )
    }

    @Test
    fun `the server still takes every field the app sends`() {
        val missing = mutableListOf<String>()
        contracts.forEach { contract ->
            endpoints(contract).filter { it.has("request") }.forEach { endpoint ->
                val root = mapper.constructType(Class.forName(endpoint["request"].asText()))
                endpoint.texts("sends").forEach { path ->
                    if (!exists(root, path.split('.'), deserialize = true)) {
                        missing.add("${endpoint["call"].asText()}: $path")
                    }
                }
            }
        }
        assertTrue(
            missing.isEmpty(),
            "The published app sends fields the server no longer reads (keep them deprecated or raise " +
                    "MIN_APP_VERSION, see dev-docs/flutter-client.md): $missing"
        )
    }

    @Test
    fun `the server has every endpoint the app calls`() {
        val missing = mutableListOf<String>()
        contracts.forEach { contract ->
            endpoints(contract).forEach { endpoint ->
                val available = mappingsOf(Class.forName(endpoint["controller"].asText()))
                val (verb, path) = endpoint["call"].asText().split(' ', limit = 2)
                if (available.none { (v, p) -> (v == null || v == verb) && matches(p, path) }) {
                    missing.add("${endpoint["call"].asText()} (${endpoint["controller"].asText()})")
                }
            }
        }
        assertTrue(missing.isEmpty(), "The published app calls endpoints the server no longer has: $missing")
    }

    @Test
    fun `the contract names every key the listed Dart files read`() {
        val unnamed = mutableListOf<String>()
        contracts.forEach { contract ->
            val named = mutableSetOf<String>()
            named.addAll(contract.texts("ignoreKeys"))
            (endpoints(contract) + types(contract)).forEach { endpoint ->
                (endpoint.texts("reads") + endpoint.texts("optionalReads")).forEach { path ->
                    named.addAll(path.split('.').map { it.removeSuffix("[]") })
                }
            }
            contract.texts("dartFiles").forEach { path ->
                val file = File(appDir, path)
                assertTrue(file.isFile, "${file.absolutePath} doesn't exist.")
                KEY_REGEX.findAll(file.readText()).map { it.groupValues[1] }.toSet()
                    .filter { it !in named }
                    .forEach { unnamed.add("$path reads '$it'") }
            }
        }
        assertEquals(
            emptyList<String>(), unnamed,
            "Name these in projectforge-flutter/test/contract/app-api-contract.json (reads, optionalReads or " +
                    "ignoreKeys), or the app reads something the server may drop unnoticed."
        )
    }

    @Test
    fun `every Dart file that reads keys is part of the contract`() {
        val listed = contracts.flatMap { it.texts("dartFiles") }.toSet()
        val libDir = File(appDir, "lib")
        val unlisted = libDir.walkTopDown().filter { it.isFile && it.extension == "dart" }
            .filter { KEY_REGEX.containsMatchIn(it.readText()) }
            .map { it.relativeTo(appDir).invariantSeparatorsPath }
            .filter { it !in listed }.sorted().toList()
        assertEquals(
            emptyList<String>(), unlisted,
            "These Dart files read ['keys'] of a json answer: add them to a contract in " +
                    "projectforge-flutter/test/contract/app-api-contract.json (new endpoints included)."
        )
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

    /** True if the type has the property of the path; `[]` steps into the elements of a collection. */
    private fun exists(type: JavaType, segments: List<String>, deserialize: Boolean = false): Boolean {
        if (segments.isEmpty()) {
            return true
        }
        val name = segments.first().removeSuffix("[]")
        val properties = if (deserialize) {
            mapper.deserializationConfig.introspect(type).findProperties()
        } else {
            mapper.serializationConfig.introspect(type).findProperties()
        }
        val property = properties.firstOrNull { it.name == name } ?: return false
        var next = property.primaryType
        if (segments.first().endsWith("[]")) {
            next = next.contentType ?: return false
        }
        return exists(next, segments.drop(1), deserialize)
    }

    companion object {
        /** `json['key']`, `card['key']`, `_errors['key']`, … : any identifier indexed by a string literal. */
        private val KEY_REGEX = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\['([A-Za-z0-9_]+)']""")
    }
}
