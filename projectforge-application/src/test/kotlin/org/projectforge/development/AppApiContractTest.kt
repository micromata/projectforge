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

    @Test
    fun `the contract exists and names fields`() {
        assertTrue(contracts.size() > 0, "No contract found.")
        contracts.forEach { assertTrue(it["reads"].size() > 0, "${it["name"].asText()} reads nothing.") }
    }

    @Test
    fun `the server delivers every field the app reads`() {
        val missing = mutableListOf<String>()
        contracts.forEach { contract ->
            val root = mapper.constructType(Class.forName(contract["answer"].asText()))
            contract["reads"].map { it.asText() }.forEach { path ->
                if (!exists(root, path.split('.'))) {
                    missing.add("${contract["name"].asText()}: $path")
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
    fun `the server has every endpoint the app calls`() {
        val missing = mutableListOf<String>()
        contracts.forEach { contract ->
            val controller = Class.forName(contract["controller"].asText())
            val base = mappingOf(AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java))
            val available = controller.methods.mapNotNull { method ->
                val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
                    ?: return@mapNotNull null
                val suffix = mappingOf(mapping)
                mapping.method.map { "$it ${(base + "/" + suffix).replace(Regex("/+"), "/").trimEnd('/')}" }
            }.flatten().toSet()
            contract["endpoints"].map { it.asText() }.filter { it !in available }
                .forEach { missing.add("${contract["name"].asText()}: $it (has: ${available.sorted()})") }
        }
        assertTrue(missing.isEmpty(), "The published app calls endpoints the server no longer has: $missing")
    }

    @Test
    fun `the contract names every key the listed Dart files read`() {
        val unnamed = mutableListOf<String>()
        contracts.forEach { contract ->
            val named = contract["reads"].map { it.asText().substringAfterLast('.').removeSuffix("[]") }.toSet() +
                    contract["reads"].flatMap { it.asText().split('.') }.map { it.removeSuffix("[]") }
            contract["dartFiles"].map { it.asText() }.forEach { path ->
                val file = File(appDir, path)
                assertTrue(file.isFile, "${file.absolutePath} doesn't exist.")
                KEY_REGEX.findAll(file.readText()).map { it.groupValues[1] }.toSet()
                    .filter { it !in named && it !in IGNORED_KEYS }
                    .forEach { unnamed.add("$path reads '$it'") }
            }
        }
        assertEquals(
            emptyList<String>(), unnamed,
            "Name these in projectforge-flutter/test/contract/app-api-contract.json (or the app reads something the " +
                    "server may drop unnoticed)."
        )
    }

    private fun mappingOf(mapping: RequestMapping?): String =
        (mapping?.path?.firstOrNull() ?: mapping?.value?.firstOrNull() ?: "").let { if (it.startsWith("/")) it else "/$it" }
            .let { resolvePlaceholders(it) }

    /** `${Rest.URL}` is a compile time constant, the annotation already holds its value (`/rs`). */
    private fun resolvePlaceholders(path: String) = path

    /** True if the type has the property of the path; `[]` steps into the elements of a collection. */
    private fun exists(type: JavaType, segments: List<String>): Boolean {
        if (segments.isEmpty()) {
            return true
        }
        val name = segments.first().removeSuffix("[]")
        val property = mapper.serializationConfig.introspect(type).findProperties().firstOrNull { it.name == name }
            ?: return false
        var next = property.primaryType
        if (segments.first().endsWith("[]")) {
            next = next.contentType ?: return false
        }
        return exists(next, segments.drop(1))
    }

    companion object {
        /** `json['key']`, `card['key']`, `this['key']`. */
        private val KEY_REGEX = Regex("""\b(?:json|card|this|data|suggestion)\['([A-Za-z0-9_]+)']""")

        /** Keys that are not of the business card api. */
        private val IGNORED_KEYS = emptySet<String>()
    }
}
