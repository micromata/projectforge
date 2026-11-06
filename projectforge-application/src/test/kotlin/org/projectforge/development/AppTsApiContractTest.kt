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
 * The contract between the app (projectforge-app, React Native) and this server. The app reads the server's data only
 * through the TypeScript interfaces of `projectforge-shared/src/rs/`, each naming its Kotlin class:
 * - `@server <class>`: an answer; every field must be delivered by the class (as Jackson writes it), except a field
 *   marked `@optionalRead` (read with a fallback, e.g. of a newer server),
 * - `@request <class>`: sent by the app; the class must still take every field,
 * - `@noServerClass <why>`: no single class (e.g. the message of any error answer).
 *
 * The endpoints the app calls are listed in `projectforge-app/test/contract/app-api-contract.json`.
 *
 * A failure is a break for the app: keep the field or endpoint `@Deprecated` and list it in
 * `dev-docs/app-api-deprecations.md`, or raise `AppCompatibility.MIN_APP_VERSION`. The published Flutter app has
 * [AppApiContractTest].
 */
class AppTsApiContractTest {
    private val rootDir = GenerateChangelogMain.resolveRootDir()
    private val rsDir = File(rootDir, "projectforge-shared/src/rs")
    private val mapper = ObjectMapper().registerKotlinModule()
    private val checks = ApiContractChecks(mapper)
    private val contracts = mapper.readTree(File(rootDir, CONTRACT_FILE))["contracts"]
    private val interfaces = parseInterfaces()

    private class Field(val name: String, val optionalRead: Boolean)

    private class Interface(
        val file: String,
        val name: String,
        val server: String?,
        val request: String?,
        val noServerClass: Boolean,
        val fields: List<Field>,
    )

    @Test
    fun `contract and interfaces exist`() {
        assertTrue(contracts.size() > 0, "No contract in $CONTRACT_FILE.")
        assertTrue(interfaces.isNotEmpty(), "No interface found in ${rsDir.absolutePath}.")
    }

    @Test
    fun `every interface names its Kotlin class`() {
        val unnamed = interfaces.filter { listOf(it.server != null, it.request != null, it.noServerClass).count { b -> b } != 1 }
            .map { "${it.file}: ${it.name}" }
        assertEquals(
            emptyList<String>(), unnamed,
            "Give each of these interfaces exactly one of @server <class>, @request <class> or @noServerClass <why>."
        )
    }

    @Test
    fun `the server delivers every field the app reads`() {
        val missing = interfaces.filter { it.server != null }.flatMap { itf ->
            val names = checks.propertyNames(checks.classOf(itf.server!!))
            itf.fields.filter { !it.optionalRead && it.name !in names }.map { "${itf.name}.${it.name} (${itf.server})" }
        }
        assertEquals(
            emptyList<String>(), missing,
            "The app reads fields the server doesn't deliver (keep them deprecated or raise MIN_APP_VERSION; a field " +
                    "read with a fallback is marked @optionalRead)."
        )
    }

    @Test
    fun `the server takes every field the app sends`() {
        val missing = interfaces.filter { it.request != null }.flatMap { itf ->
            val names = checks.propertyNames(checks.classOf(itf.request!!), deserialize = true)
            itf.fields.filter { it.name !in names }.map { "${itf.name}.${it.name} (${itf.request})" }
        }
        assertEquals(emptyList<String>(), missing, "The app sends fields the server doesn't take.")
    }

    @Test
    fun `the server has every endpoint the app calls`() {
        val missing = contracts.flatMap { contract -> contract["endpoints"]?.toList() ?: emptyList() }
            .filter { !checks.hasEndpoint(checks.classOf(it["controller"].asText()), it["call"].asText()) }
            .map { "${it["call"].asText()} (${it["controller"].asText()})" }
        assertEquals(emptyList<String>(), missing, "The app calls endpoints the server doesn't have.")
    }

    /**
     * The exported interfaces of the files of [rsDir] with their JSDoc tags and fields. A deliberately small parser:
     * one interface per `export interface X {` … `}` (closing at the start of a line), one field per line
     * (`name?: type;`), its JSDoc right above it. Nested object types are not allowed: a nested object is an
     * interface of its own (with its own Kotlin class).
     */
    private fun parseInterfaces(): List<Interface> =
        rsDir.walkTopDown().filter { it.isFile && it.extension == "ts" }.sortedBy { it.name }.flatMap { file ->
            val text = file.readText()
            INTERFACE_REGEX.findAll(text).map { match ->
                val doc = match.groupValues[1]
                Interface(
                    file = file.relativeTo(rootDir).invariantSeparatorsPath,
                    name = match.groupValues[2],
                    server = SERVER_REGEX.find(doc)?.groupValues?.get(1),
                    request = REQUEST_REGEX.find(doc)?.groupValues?.get(1),
                    noServerClass = doc.contains("@noServerClass"),
                    fields = parseFields(match.groupValues[3]),
                )
            }.toList()
        }.toList()

    private fun parseFields(body: String): List<Field> {
        val fields = mutableListOf<Field>()
        var comment = ""
        var inComment = false
        body.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("/**") -> {
                    comment = trimmed
                    inComment = !trimmed.endsWith("*/")
                }

                inComment -> {
                    comment += "\n" + trimmed
                    inComment = !trimmed.endsWith("*/")
                }

                else -> FIELD_REGEX.matchEntire(line)?.let {
                    fields.add(Field(it.groupValues[1], comment.contains("@optionalRead")))
                    comment = ""
                }
            }
        }
        return fields
    }

    companion object {
        private const val CONTRACT_FILE = "projectforge-app/test/contract/app-api-contract.json"

        /** `/** … */ export interface Name {` … `\n}`, the JSDoc directly above it. */
        private val INTERFACE_REGEX =
            Regex("""/\*\*((?:(?!\*/)[\s\S])*)\*/\s*export interface (\w+)(?:<[^>]*>)?\s*\{([\s\S]*?)\n}""")
        private val SERVER_REGEX = Regex("""@server\s+([\w.$]+)""")
        private val REQUEST_REGEX = Regex("""@request\s+([\w.$]+)""")

        /** A field at the first level of the interface (indented by two spaces): `name?: type;`. */
        private val FIELD_REGEX = Regex("""^ {2}(?:readonly\s+)?(\w+)\??\s*:.*$""")
    }
}
