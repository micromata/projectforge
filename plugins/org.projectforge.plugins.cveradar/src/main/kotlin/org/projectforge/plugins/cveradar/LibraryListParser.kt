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
import java.net.URLDecoder

/** The formats of the lists a component's libraries are read from. */
enum class LibraryFormat { NPM, YARN, PIP, GO, CARGO, COMPOSER, MAVEN, GRADLE, JAR, DEB, APK, RPM, CYCLONEDX, SPDX }

/**
 * One library of a list: [ecosystem] is the purl type (`maven`, `npm`, `pypi`, `deb`, ...), [group] the purl namespace
 * (Maven: the groupId, npm: the scope with its `@`, deb: the distribution), [name] the name (Maven: the artifactId,
 * npm: the name without scope). [distro] is the `distro` qualifier of an operating system package (`debian-12`): the
 * same version may be fixed in one release and not in another. The other qualifiers (`arch`, ...) are dropped.
 */
data class LibraryEntry(
    val ecosystem: String,
    val group: String?,
    val name: String,
    val version: String,
    val distro: String? = null,
) {
    /** The package URL with the version; it identifies the entry (npm: `@` of a scope as `%40`). */
    val purl: String
        get() = "pkg:$ecosystem/${group?.let { "${it.replace("@", "%40")}/" } ?: ""}$name@$version" +
            (distro?.let { "?distro=$it" } ?: "")

    /**
     * The name of the technology: the full name where the namespace is part of it (npm with scope, Go module, Composer
     * vendor/package), otherwise the name (the artifactId of Maven, the package of a distribution).
     */
    val product: String
        get() = if (group != null && ecosystem in WITH_NAMESPACE) "$group/$name" else name

    private companion object {
        val WITH_NAMESPACE = setOf("npm", "golang", "composer", "swift")
    }
}

/** A file of a list with the format found in it (null: not recognised) and its entries. */
class ParsedLibraryFile(val name: String, val format: LibraryFormat?, val entries: List<LibraryEntry>)

/**
 * Reads the lists a component's libraries come from (INV-14, INV-26): SBOMs (CycloneDX JSON, SPDX JSON), the
 * `package-lock.json` of npm, the output of `mvn dependency:list` (or `dependency:tree`) and of `gradle dependencies`.
 * The format is told from the content.
 * Nothing is executed or resolved; the files are untrusted (SEC-05): sizes and counts are limited.
 */
object LibraryListParser {
    const val MAX_FILES = 10
    const val MAX_FILE_BYTES = 5 * 1024 * 1024
    const val MAX_ENTRIES = 20_000
    private const val MAX_DEPTH = 20

    const val ERROR_TOO_LARGE = "plugins.cveradar.libraries.error.tooLarge"
    const val ERROR_TOO_MANY_FILES = "plugins.cveradar.libraries.error.tooManyFiles"
    const val ERROR_TOO_MANY_ENTRIES = "plugins.cveradar.libraries.error.tooManyEntries"

    private val objectMapper = ObjectMapper()

    private const val ID = "[A-Za-z0-9_.\\-]+"
    private val MAVEN_LINE = Regex("""^(?:\[[A-Z]+\]\s+)?[\s|+\\\-]*($ID):($ID):($ID):(?:($ID):)?($ID):(compile|runtime|provided|test|system|import)(?:\s.*)?$""")
    private val GRADLE_LINE = Regex("""^[\s|]*[+\\]---\s+([^\s:]+):([^\s:]+)(?::(\S+))?(?:\s+->\s+(\S+))?(.*)$""")
    private val JAR_NAME = Regex("""^(.+?)-(\d.*?)(?:-(?:sources|javadoc|tests|test-sources|all|linux-\S+|osx-\S+|macos-\S+|windows-\S+))?\.jar$""", RegexOption.IGNORE_CASE)
    private val GRADLE_RANGE = Regex("""\{(?:strictly|require|prefer|reject)?\s*([^}\s]+)[^}]*}""")
    private val PIP_LINE = Regex("""^([A-Za-z0-9][A-Za-z0-9._\-]*)(?:\[[^\]]*])?\s*===?\s*([A-Za-z0-9][A-Za-z0-9.!+_\-]*)(?:\s*(?:;|#|--|\\).*)?$""")
    private val GO_LINE = Regex("""^\s*(?:require\s+)?([a-z0-9][A-Za-z0-9._~\-]*(?:/[A-Za-z0-9._~\-]+)+)\s+(v\d[A-Za-z0-9.+\-]*)(?:\s*//.*)?$""")
    private val APK_LINE = Regex("""^([a-z0-9][a-z0-9._+\-]*)-(\d[A-Za-z0-9._]*-r\d+)$""")
    private val RPM_LINE = Regex("""^(?:\d+:)?([A-Za-z0-9][A-Za-z0-9._+\-]*)-(\d[^\s-]*-[^\s-]+)\.(x86_64|aarch64|noarch|i[3-6]86|armv\d\w*|ppc64le|s390x|src)$""")
    private val DEB_LINE = Regex("""^(?:ii\s+)?((?=[a-z0-9+.\-]*[a-z])[a-z0-9][a-z0-9+.\-]*)(?::[a-z0-9]+)?\s+((?:\d+:)?\d[A-Za-z0-9.+~:\-]*)(?:\s+.*)?$""")
    private val PURL = Regex("""^pkg:/*([A-Za-z][A-Za-z0-9.+\-]*)/(.+?)@([^?#]+)(?:\?([^#]*))?(?:#.*)?$""")

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
            val parsed = runCatching {
                val root = objectMapper.readTree(trimmed)
                when {
                    root.path("bomFormat").asText() == "CycloneDX" -> LibraryFormat.CYCLONEDX to parseCycloneDx(root)
                    root.has("spdxVersion") -> LibraryFormat.SPDX to parseSpdx(root)
                    root.path("packages").isArray -> LibraryFormat.COMPOSER to parseComposer(root)
                    else -> LibraryFormat.NPM to parseNpm(root)
                }
            }.getOrNull()
            return if (parsed == null || parsed.second.isEmpty()) ParsedLibraryFile(name, null, emptyList())
            else ParsedLibraryFile(name, parsed.first, parsed.second)
        }
        val lines = text.lineSequence().toList()
        // Lists in lines: the format with the most entries wins, the others hardly match the text of a tool.
        val candidates = listOf(
            LibraryFormat.CARGO to parseCargo(lines),
            LibraryFormat.YARN to parseYarn(lines),
            LibraryFormat.GO to lines.mapNotNull { parseGoLine(it) },
            LibraryFormat.PIP to lines.mapNotNull { parsePipLine(it) },
            LibraryFormat.GRADLE to lines.mapNotNull { parseGradleLine(it) },
            LibraryFormat.MAVEN to lines.mapNotNull { parseMavenLine(it) },
            LibraryFormat.APK to lines.mapNotNull { parseApkLine(it) },
            LibraryFormat.RPM to lines.mapNotNull { parseRpmLine(it) },
            LibraryFormat.DEB to lines.mapNotNull { parseDebLine(it) },
        ).map { (format, entries) -> format to entries.distinct() }
        val best = candidates.filter { it.second.isNotEmpty() }.maxByOrNull { it.second.size }
        if (best != null) return ParsedLibraryFile(name, best.first, best.second)
        // None of them: perhaps the file names of a jar (`unzip -l`, `ls`, `jar tf`).
        val jars = lines.flatMap { parseJarLine(it) }.distinct()
        return ParsedLibraryFile(name, if (jars.isEmpty()) null else LibraryFormat.JAR, jars)
    }

    /**
     * A package URL with a version (`pkg:maven/group/artifact@1.0`, `pkg:deb/debian/openssl@3.0.11?distro=debian-12`)
     * as an entry; null without a version or for anything else. The last segment of the path is the name, the ones
     * before it the namespace.
     */
    fun fromPurl(purl: String?): LibraryEntry? {
        val match = PURL.matchEntire(purl?.trim() ?: return null) ?: return null
        val (type, path, version, qualifiers) = match.destructured
        val segments = path.trim('/').split('/').filter { it.isNotEmpty() }.map(::decode)
        if (segments.isEmpty() || segments.any { it.isBlank() }) return null
        val distro = qualifiers.split('&').firstOrNull { it.startsWith("distro=") }?.substringAfter('=')
            ?.let(::decode)?.takeIf { it.isNotBlank() }
        return LibraryEntry(
            ecosystem = type.lowercase(),
            group = segments.dropLast(1).joinToString("/").takeIf { it.isNotEmpty() },
            name = segments.last(),
            version = decode(version).takeIf { it.isNotBlank() } ?: return null,
            distro = distro,
        )
    }

    /** Percent-decoded as UTF-8; a `+` stays a `+` (versions of distributions have it), a broken escape stays as is. */
    private fun decode(text: String): String =
        if (!text.contains('%')) text
        else runCatching { URLDecoder.decode(text.replace("+", "%2B"), Charsets.UTF_8) }.getOrDefault(text)

    /**
     * The components of a CycloneDX SBOM (nested ones too) by their purl. Components without a purl (files, the
     * application itself) and those not needed at runtime (scope `excluded` or `optional`) are left out.
     */
    private fun parseCycloneDx(root: JsonNode): List<LibraryEntry> {
        val entries = mutableListOf<LibraryEntry>()
        fun walk(components: JsonNode, depth: Int) {
            if (!components.isArray || depth > MAX_DEPTH) return
            components.forEach { component ->
                val scope = component.path("scope").asText("")
                if (scope != "excluded" && scope != "optional") {
                    fromPurl(component.path("purl").asText(null))?.let { entries.add(it) }
                }
                walk(component.path("components"), depth + 1)
            }
        }
        walk(root.path("components"), 0)
        return entries.distinct()
    }

    /** The packages of an SPDX document by their purl (external reference of the type `purl`). */
    private fun parseSpdx(root: JsonNode): List<LibraryEntry> =
        root.path("packages").flatMap { pkg ->
            pkg.path("externalRefs").filter { it.path("referenceType").asText() == "purl" }
                .mapNotNull { fromPurl(it.path("referenceLocator").asText(null)) }
        }.distinct()

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

    /** The packages of a `composer.lock` (the ones for development are in `packages-dev`); the `v` of a tag is dropped. */
    private fun parseComposer(root: JsonNode): List<LibraryEntry> =
        root.path("packages").mapNotNull { pkg ->
            val name = pkg.path("name").asText("")
            val version = pkg.path("version").asText("").removePrefix("v")
            if (!name.contains('/') || version.isEmpty()) null
            else LibraryEntry("composer", name.substringBefore('/').lowercase(), name.substringAfter('/').lowercase(), version)
        }.distinct()

    /** The registry crates of a `Cargo.lock` (`[[package]]` with name, version and source; the own crates have no source). */
    private fun parseCargo(lines: List<String>): List<LibraryEntry> {
        val entries = mutableListOf<LibraryEntry>()
        var name: String? = null
        var version: String? = null
        var registry = false
        fun flush() {
            if (registry && name != null && version != null) entries.add(LibraryEntry("cargo", null, name!!, version!!))
            name = null; version = null; registry = false
        }
        lines.forEach { raw ->
            val line = raw.trim()
            when {
                line == "[[package]]" -> flush()
                line.startsWith("name = ") -> name = line.substringAfter("= ").trim('"')
                line.startsWith("version = ") -> version = line.substringAfter("= ").trim('"')
                line.startsWith("source = ") -> registry = line.contains("registry+")
            }
        }
        flush()
        return entries
    }

    /** The resolved packages of a `yarn.lock` (classic: `version "1.2.3"`, berry: `version: 1.2.3`). */
    private fun parseYarn(lines: List<String>): List<LibraryEntry> {
        val entries = mutableListOf<LibraryEntry>()
        var name: String? = null
        lines.forEach { line ->
            when {
                line.isBlank() || line.startsWith("#") -> Unit
                !line.startsWith(" ") && line.trimEnd().endsWith(":") -> {
                    // `"@scope/name@^1.0.0", "@scope/name@^1.1.0":` — one name for all its ranges; the own workspaces of
                    // berry (`app@workspace:.`) are no libraries.
                    val spec = line.trimEnd().trimEnd(':').split(',').first().trim().trim('"')
                    val at = spec.indexOf('@', 1)
                    name = if (at > 0 && !spec.startsWith("workspace:", at + 1)) spec.substring(0, at) else null
                }
                name != null && line.trimStart().let { it.startsWith("version ") || it.startsWith("version:") } -> {
                    val version = line.trim().removePrefix("version").trimStart(':', ' ').trim('"')
                    val scope = name!!.takeIf { it.startsWith("@") }?.substringBefore('/')
                    if (version.isNotEmpty()) {
                        entries.add(LibraryEntry("npm", scope, if (scope != null) name!!.substringAfter('/') else name!!, version))
                    }
                    name = null
                }
            }
        }
        return entries
    }

    /** `name==1.2.3` of a `requirements.txt` or `pip freeze`; the name as the purl has it (lower case, `-` for `_` and `.`). */
    private fun parsePipLine(line: String): LibraryEntry? {
        val match = PIP_LINE.matchEntire(line.trim()) ?: return null
        val (name, version) = match.destructured
        return LibraryEntry("pypi", null, name.lowercase().replace(Regex("[_.]+"), "-"), version)
    }

    /** A module with its version of a `go.mod` (`require` line or block) or of `go list -m all`. */
    private fun parseGoLine(line: String): LibraryEntry? {
        val match = GO_LINE.matchEntire(line.trimEnd()) ?: return null
        val (module, version) = match.destructured
        return LibraryEntry("golang", module.substringBeforeLast('/'), module.substringAfterLast('/'), version)
    }

    /** `apk info -v`: `musl-1.2.4-r2`. */
    private fun parseApkLine(line: String): LibraryEntry? {
        val (name, version) = APK_LINE.matchEntire(line.trim())?.destructured ?: return null
        return LibraryEntry("apk", null, name, version)
    }

    /** `rpm -qa`: `openssl-libs-3.0.7-24.el9.x86_64`; the version is version and release. */
    private fun parseRpmLine(line: String): LibraryEntry? {
        val match = RPM_LINE.matchEntire(line.trim()) ?: return null
        return LibraryEntry("rpm", null, match.groupValues[1], match.groupValues[2])
    }

    /** `dpkg-query -W -f '${Package} ${Version}\n'` or a line of `dpkg -l` (`ii  name  version  arch  description`). */
    private fun parseDebLine(line: String): LibraryEntry? {
        val match = DEB_LINE.matchEntire(line.trim()) ?: return null
        return LibraryEntry("deb", null, match.groupValues[1], match.groupValues[2])
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
