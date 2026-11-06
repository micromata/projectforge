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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The contract between the published Flutter app and this server: `projectforge-flutter/test/contract/app-api-contract.json`
 * names what the app reads. It stays (frozen) as long as the Flutter app is in use, i.e. until
 * `AppCompatibility.MIN_APP_VERSION` is above its last version (1.0.1); the app projectforge-app has
 * [AppTsApiContractTest]. This test fails
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
    private val checks = ApiContractChecks(mapper)
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
                    if (!checks.exists(root, path.split('.'))) {
                        missing.add("${endpoint["call"].asText()}: $path")
                    }
                }
            }
            types(contract).forEach { type ->
                val root = mapper.constructType(Class.forName(type["class"].asText()))
                type.texts("reads").forEach { path ->
                    if (!checks.exists(root, path.split('.'))) {
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
                    if (!checks.exists(root, path.split('.'), deserialize = true)) {
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
                if (!checks.hasEndpoint(Class.forName(endpoint["controller"].asText()), endpoint["call"].asText())) {
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

    companion object {
        /** `json['key']`, `card['key']`, `_errors['key']`, … : any identifier indexed by a string literal. */
        private val KEY_REGEX = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\['([A-Za-z0-9_]+)']""")
    }
}
