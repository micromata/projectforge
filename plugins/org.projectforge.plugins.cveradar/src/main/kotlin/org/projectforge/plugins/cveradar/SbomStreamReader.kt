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

package org.projectforge.plugins.cveradar

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.io.InputStream

/** What a CycloneDX SBOM describes (`metadata.component`): `type` is `application`, `container`, `library`, `file`, ... */
class SbomSubject(val type: String?, val name: String?, val version: String?)

/**
 * An SBOM read by [SbomStreamReader]: its libraries ([LibraryEntry], one per library with the places it was found) and
 * the number of components left out for want of a purl with a version ([withoutPurl]: files, the application itself,
 * declarations with an unresolved version).
 */
class SbomDocument(
    val format: LibraryFormat,
    val subject: SbomSubject?,
    val entries: List<LibraryEntry>,
    val withoutPurl: Int,
)

/**
 * Reads an SBOM (CycloneDX JSON, SPDX JSON) as a stream: only the fields the libraries are taken from are kept, all
 * the rest (hashes, licences, properties of the scanner, file entries) is skipped token by token. An SBOM of a
 * container image of some hundred MB thereby needs no more memory than its libraries. The order of the fields doesn't
 * matter (`bomFormat` may come last). Components with scope `excluded` or `optional` are left out, nested components
 * are read up to a depth of [MAX_DEPTH].
 */
object SbomStreamReader {
    private const val MAX_DEPTH = 20
    private val SYFT_LOCATION = Regex("""syft:location:\d+:path""")
    private val factory = JsonFactory()

    /** Null if it isn't an SBOM (other JSON); throws on JSON that isn't well-formed. */
    fun read(input: InputStream): SbomDocument? = factory.createParser(input).use { read(it) }

    fun read(text: String): SbomDocument? = factory.createParser(text).use { read(it) }

    private class Counter(var withoutPurl: Int = 0)

    private fun read(parser: JsonParser): SbomDocument? {
        if (parser.nextToken() != JsonToken.START_OBJECT) return null
        var cycloneDx = false
        var spdx = false
        var subject: SbomSubject? = null
        val components = mutableListOf<LibraryEntry>()
        val packages = mutableListOf<LibraryEntry>()
        val counter = Counter()
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            val field = parser.currentName()
            val token = parser.nextToken()
            when {
                field == "bomFormat" && token == JsonToken.VALUE_STRING -> cycloneDx = parser.text == "CycloneDX"
                field == "spdxVersion" -> {
                    spdx = true
                    parser.skipChildren()
                }

                field == "metadata" && token == JsonToken.START_OBJECT -> subject = readMetadata(parser)
                field == "components" && token == JsonToken.START_ARRAY -> readComponents(parser, 0, components, counter)
                field == "packages" && token == JsonToken.START_ARRAY -> readPackages(parser, packages)
                else -> parser.skipChildren()
            }
        }
        return when {
            cycloneDx -> SbomDocument(LibraryFormat.CYCLONEDX, subject, grouped(components), counter.withoutPurl)
            spdx -> SbomDocument(LibraryFormat.SPDX, null, packages.distinct(), 0)
            else -> null
        }
    }

    /** One entry per library; the places of a library found several times (a Go binary each) together. */
    private fun grouped(entries: List<LibraryEntry>): List<LibraryEntry> =
        entries.groupBy { it.copy(location = null) }.map { (entry, list) ->
            entry.copy(location = LibraryListParser.joinLocations(list.mapNotNull { it.location }))
        }

    private fun readMetadata(parser: JsonParser): SbomSubject? {
        var subject: SbomSubject? = null
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            val field = parser.currentName()
            val token = parser.nextToken()
            if (field == "component" && token == JsonToken.START_OBJECT) {
                var type: String? = null
                var name: String? = null
                var version: String? = null
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    val key = parser.currentName()
                    when (parser.nextToken()) {
                        JsonToken.VALUE_STRING -> when (key) {
                            "type" -> type = parser.text
                            "name" -> name = parser.text
                            "version" -> version = parser.text
                        }

                        else -> parser.skipChildren()
                    }
                }
                subject = SbomSubject(type?.trim(), name?.trim()?.takeIf { it.isNotEmpty() }, version?.trim()?.takeIf { it.isNotEmpty() })
            } else {
                parser.skipChildren()
            }
        }
        return subject
    }

    /** The parser is at the start of an array of components; afterwards at its end. */
    private fun readComponents(parser: JsonParser, depth: Int, out: MutableList<LibraryEntry>, counter: Counter) {
        if (depth > MAX_DEPTH) {
            parser.skipChildren()
            return
        }
        while (true) {
            when (parser.nextToken()) {
                JsonToken.END_ARRAY, null -> return
                JsonToken.START_OBJECT -> readComponent(parser, depth, out, counter)
                else -> parser.skipChildren()
            }
        }
    }

    private fun readComponent(parser: JsonParser, depth: Int, out: MutableList<LibraryEntry>, counter: Counter) {
        // The component before its nested ones, as listed.
        val index = out.size
        var purl: String? = null
        var scope = ""
        val occurrences = mutableListOf<String>()
        val syftPaths = mutableListOf<String>()
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            val field = parser.currentName()
            val token = parser.nextToken()
            when {
                field == "purl" && token == JsonToken.VALUE_STRING -> purl = parser.text
                field == "scope" && token == JsonToken.VALUE_STRING -> scope = parser.text
                field == "evidence" && token == JsonToken.START_OBJECT -> readOccurrences(parser, occurrences)
                field == "properties" && token == JsonToken.START_ARRAY -> readSyftLocations(parser, syftPaths)
                field == "components" && token == JsonToken.START_ARRAY -> readComponents(parser, depth + 1, out, counter)
                else -> parser.skipChildren()
            }
        }
        if (scope == "excluded" || scope == "optional") return
        // Where it was found: the occurrences of its evidence (CycloneDX 1.5), otherwise the locations syft writes.
        val entry = LibraryListParser.fromPurl(purl)
        if (entry == null) {
            counter.withoutPurl++
            return
        }
        out.add(index, entry.copy(location = LibraryListParser.joinLocations(occurrences.ifEmpty { syftPaths })))
    }

    /** `evidence.occurrences[].location`; the parser is at the start of the evidence object, afterwards at its end. */
    private fun readOccurrences(parser: JsonParser, out: MutableList<String>) {
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            val field = parser.currentName()
            val token = parser.nextToken()
            if (field != "occurrences" || token != JsonToken.START_ARRAY) {
                parser.skipChildren()
                continue
            }
            forEachObject(parser) { key ->
                if (key == "location" && parser.currentToken() == JsonToken.VALUE_STRING) {
                    parser.text.trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                } else {
                    parser.skipChildren()
                }
            }
        }
    }

    /** The values of the properties `syft:location:<n>:path`; the parser is at the start of the array, afterwards at its end. */
    private fun readSyftLocations(parser: JsonParser, out: MutableList<String>) {
        var name: String? = null
        var value: String? = null
        forEachObject(
            parser,
            onStart = { name = null; value = null },
            onEnd = { if (name != null && SYFT_LOCATION.matches(name!!)) value?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) } },
        ) { key ->
            if (parser.currentToken() == JsonToken.VALUE_STRING && key == "name") name = parser.text
            else if (parser.currentToken() == JsonToken.VALUE_STRING && key == "value") value = parser.text
            else parser.skipChildren()
        }
    }

    /** The packages of an SPDX document by their purl (external reference of the type `purl`). */
    private fun readPackages(parser: JsonParser, out: MutableList<LibraryEntry>) {
        forEachObject(parser) { key ->
            if (key != "externalRefs" || parser.currentToken() != JsonToken.START_ARRAY) {
                parser.skipChildren()
                return@forEachObject
            }
            var type: String? = null
            var locator: String? = null
            forEachObject(
                parser,
                onStart = { type = null; locator = null },
                onEnd = { if (type == "purl") LibraryListParser.fromPurl(locator)?.let { out.add(it) } },
            ) { refKey ->
                if (parser.currentToken() == JsonToken.VALUE_STRING && refKey == "referenceType") type = parser.text
                else if (parser.currentToken() == JsonToken.VALUE_STRING && refKey == "referenceLocator") locator = parser.text
                else parser.skipChildren()
            }
        }
    }

    /**
     * The objects of the array the parser is at the start of: [field] is called with every field name, the parser at its
     * value (it reads or skips the value). Anything else than an object in the array is skipped.
     */
    private fun forEachObject(
        parser: JsonParser,
        onStart: () -> Unit = {},
        onEnd: () -> Unit = {},
        field: (String) -> Unit,
    ) {
        while (true) {
            when (parser.nextToken()) {
                JsonToken.END_ARRAY, null -> return
                JsonToken.START_OBJECT -> {
                    onStart()
                    while (parser.nextToken() == JsonToken.FIELD_NAME) {
                        val key = parser.currentName()
                        parser.nextToken()
                        field(key)
                    }
                    onEnd()
                }

                else -> parser.skipChildren()
            }
        }
    }
}
