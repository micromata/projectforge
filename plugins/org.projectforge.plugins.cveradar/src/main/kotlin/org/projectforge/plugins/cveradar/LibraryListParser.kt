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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.common.i18n.UserException

/** The formats of the lists a component's libraries are read from. */
enum class LibraryFormat { NPM, MAVEN, GRADLE, JAR }

/**
 * One library of a list: [ecosystem] is the purl type (`maven`, `npm`), [group] the groupId (npm: the scope with its
 * `@`), [name] the artifactId (npm: the name without scope).
 */
data class LibraryEntry(val ecosystem: String, val group: String?, val name: String, val version: String) {
    /** The package URL with the version; it identifies the entry (npm: `@` of a scope as `%40`). */
    val purl: String
        get() = "pkg:$ecosystem/${group?.let { "${it.replace("@", "%40")}/" } ?: ""}$name@$version"

    /** The name of the technology: for npm the full name with scope, for Maven the artifactId. */
    val product: String
        get() = if (ecosystem == "npm" && group != null) "$group/$name" else name
}

/** A file of a list with the format found in it (null: not recognised) and its entries. */
class ParsedLibraryFile(val name: String, val format: LibraryFormat?, val entries: List<LibraryEntry>)

/**
 * Reads the lists a component's libraries come from (INV-26): the `package-lock.json` of npm, the output of
 * `mvn dependency:list` (or `dependency:tree`) and of `gradle dependencies`. The format is told from the content.
 * Nothing is executed or resolved; the files are untrusted (SEC-05): sizes and counts are limited.
 */
object LibraryListParser {
    const val MAX_FILES = 10
    const val MAX_FILE_BYTES = 5 * 1024 * 1024
    const val MAX_ENTRIES = 20_000

    const val ERROR_TOO_LARGE = "plugins.cveradar.libraries.error.tooLarge"
    const val ERROR_TOO_MANY_FILES = "plugins.cveradar.libraries.error.tooManyFiles"
    const val ERROR_TOO_MANY_ENTRIES = "plugins.cveradar.libraries.error.tooManyEntries"

    private val objectMapper = ObjectMapper()

    private const val ID = "[A-Za-z0-9_.\\-]+"
    private val MAVEN_LINE = Regex("""^(?:\[[A-Z]+\]\s+)?[\s|+\\\-]*($ID):($ID):($ID):(?:($ID):)?($ID):(compile|runtime|provided|test|system|import)(?:\s.*)?$""")
    private val GRADLE_LINE = Regex("""^[\s|]*[+\\]---\s+([^\s:]+):([^\s:]+)(?::(\S+))?(?:\s+->\s+(\S+))?(.*)$""")
    private val JAR_NAME = Regex("""^(.+?)-(\d.*?)(?:-(?:sources|javadoc|tests|test-sources|all|linux-\S+|osx-\S+|macos-\S+|windows-\S+))?\.jar$""", RegexOption.IGNORE_CASE)
    private val GRADLE_RANGE = Regex("""\{(?:strictly|require|prefer|reject)?\s*([^}\s]+)[^}]*}""")

    /** All files, parsed; the entries of all of them together are limited. */
    fun parseAll(files: List<Pair<String, String>>): List<ParsedLibraryFile> {
        if (files.size > MAX_FILES) throw UserException(ERROR_TOO_MANY_FILES, MAX_FILES.toString())
        val parsed = files.map { (name, text) -> parse(name, text) }
        if (parsed.sumOf { it.entries.size } > MAX_ENTRIES) throw UserException(ERROR_TOO_MANY_ENTRIES, MAX_ENTRIES.toString())
        return parsed
    }

    fun parse(name: String, text: String): ParsedLibraryFile {
        if (text.length > MAX_FILE_BYTES) throw UserException(ERROR_TOO_LARGE, name, (MAX_FILE_BYTES / 1024 / 1024).toString())
        val trimmed = text.trimStart()
        if (trimmed.startsWith("{")) {
            val entries = runCatching { parseNpm(objectMapper.readTree(trimmed)) }.getOrNull()
            return if (entries.isNullOrEmpty()) ParsedLibraryFile(name, null, emptyList())
            else ParsedLibraryFile(name, LibraryFormat.NPM, entries)
        }
        val lines = text.lineSequence().toList()
        val gradle = lines.mapNotNull { parseGradleLine(it) }
        val maven = lines.mapNotNull { parseMavenLine(it) }
        // The line pattern of the format with more entries wins; the other one hardly matches the text of a tool.
        if (gradle.isEmpty() && maven.isEmpty()) {
            // Neither tool: perhaps the file names of a jar (`unzip -l`, `ls`, `jar tf`).
            val jars = lines.flatMap { parseJarLine(it) }.distinct()
            return ParsedLibraryFile(name, if (jars.isEmpty()) null else LibraryFormat.JAR, jars)
        }
        return when {
            gradle.size > maven.size -> ParsedLibraryFile(name, LibraryFormat.GRADLE, gradle.distinct())
            else -> ParsedLibraryFile(name, LibraryFormat.MAVEN, maven.distinct())
        }
    }

    /** `pkg:maven/group/artifact@1.0` back to an entry; null for anything else. */
    fun fromPurl(purl: String?): LibraryEntry? {
        val match = Regex("""^pkg:(maven|npm)/(?:([^/@]+)/)?([^/@]+)@(.+)$""").matchEntire(purl ?: return null) ?: return null
        val (type, group, name, version) = match.destructured
        return LibraryEntry(type, group.takeIf { it.isNotEmpty() }?.replace("%40", "@"), name, version)
    }

    private fun parseNpm(root: JsonNode): List<LibraryEntry> {
        val entries = mutableListOf<LibraryEntry>()
        fun add(fullName: String, node: JsonNode) {
            val version = node.path("version").asText("").takeIf { it.isNotEmpty() } ?: return
            // Development dependencies are not part of what runs.
            if (node.path("link").asBoolean(false) || node.path("dev").asBoolean(false)) return
            val scope = fullName.takeIf { it.startsWith("@") }?.substringBefore('/')
            entries.add(LibraryEntry("npm", scope, if (scope != null) fullName.substringAfter('/') else fullName, version))
        }
        val packages = root.path("packages")
        if (packages.isObject) {
            packages.fields().forEach { (path, node) ->
                if (path.isEmpty() || !path.contains("node_modules/")) return@forEach
                add(path.substringAfterLast("node_modules/"), node)
            }
        } else {
            fun walk(dependencies: JsonNode) {
                dependencies.fields().forEach { (name, node) ->
                    add(name, node)
                    if (node.path("dependencies").isObject) walk(node.path("dependencies"))
                }
            }
            if (root.path("dependencies").isObject) walk(root.path("dependencies"))
        }
        return entries.distinct()
    }

    /**
     * The jar names in a line (`unzip -l`, `unzip -t`, `ls`, `find`, `jar tf`): the path is dropped, the name is
     * `artifactId-version[-classifier].jar` where the version starts with a digit. The groupId isn't in a name.
     */
    private fun parseJarLine(line: String): List<LibraryEntry> =
        line.split(Regex("\\s+")).filter { it.endsWith(".jar", ignoreCase = true) }.mapNotNull { token ->
            val match = JAR_NAME.matchEntire(token.substringAfterLast('/')) ?: return@mapNotNull null
            LibraryEntry("maven", null, match.groupValues[1], match.groupValues[2])
        }

    private fun parseMavenLine(line: String): LibraryEntry? {
        val match = MAVEN_LINE.matchEntire(line.trimEnd()) ?: return null
        val (group, artifact, _, _, version, scope) = match.destructured
        if (scope == "test" || scope == "provided") return null
        return LibraryEntry("maven", group, artifact, version)
    }

    private fun parseGradleLine(line: String): LibraryEntry? {
        val match = GRADLE_LINE.matchEntire(GRADLE_RANGE.replace(line.trimEnd(), "$1")) ?: return null
        val (group, artifact, requested, resolved, rest) = match.destructured
        // A constraint (c) isn't a dependency, (n) wasn't resolved.
        if (rest.contains("(n)") || rest.contains("(c)") || rest.contains("FAILED")) return null
        val version = resolved.ifEmpty { requested }.trim()
        if (version.isEmpty() || version.any { it in "[]()," }) return null
        return LibraryEntry("maven", group, artifact, version)
    }
}
