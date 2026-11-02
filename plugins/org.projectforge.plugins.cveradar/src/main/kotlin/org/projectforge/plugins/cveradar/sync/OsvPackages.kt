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

package org.projectforge.plugins.cveradar.sync

/**
 * The ecosystems of the libraries with advisories of OSV.dev (INV-23, IMP-13): the OSV key, the name in the catalogue
 * and the purl type; and the spelling of package names both sides agree on ([key]).
 */
object OsvPackages {
    class Ecosystem(val key: String, val name: String, val purlType: String)

    val ECOSYSTEMS = listOf(
        Ecosystem("Maven", "Java/Maven", "maven"),
        Ecosystem("npm", "Node/npm", "npm"),
        Ecosystem("PyPI", "Python/PyPI", "pypi"),
        Ecosystem("NuGet", ".NET/NuGet", "nuget"),
        Ecosystem("Packagist", "PHP/Composer", "composer"),
        Ecosystem("Go", "Go", "golang"),
        Ecosystem("crates.io", "Rust/Cargo", "cargo"),
    )

    private val byKey = ECOSYSTEMS.associateBy { it.key }
    private val byPurlType = ECOSYSTEMS.associateBy { it.purlType }

    fun ecosystem(key: String?): Ecosystem? = byKey[key]

    /**
     * The package of a purl as looked up in the index: the [key] of its name, or (Maven without groupId, a jar
     * listing) the artifact as
     * [CvrPurlMatchDO.shortName][org.projectforge.plugins.cveradar.model.CvrPurlMatchDO.shortName].
     */
    data class PurlPackage(val ecosystem: String, val name: String, val byShortName: Boolean)

    /** The package of a purl (`pkg:maven/group/artifact@1.0`, without version too); null for another type. */
    fun ofPurl(purl: String?): PurlPackage? {
        val match = PURL.matchEntire(purl?.trim() ?: return null) ?: return null
        val segments = match.groupValues[2].trim('/').split('/').filter { it.isNotEmpty() }.map(::decode)
        if (segments.isEmpty()) return null
        return of(match.groupValues[1], segments.dropLast(1).joinToString("/").ifEmpty { null }, segments.last())
    }

    /** The package of a purl given as type, namespace and name (e.g. of a catalogue node); null for another type. */
    fun of(purlType: String?, namespace: String?, name: String?): PurlPackage? {
        val ecosystem = byPurlType[purlType?.lowercase()] ?: return null
        val n = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val ns = namespace?.trim()?.replace("%40", "@")?.takeIf { it.isNotEmpty() }
        if (ecosystem.key == "Maven" && ns == null) {
            return PurlPackage(ecosystem.key, n.lowercase(), byShortName = true)
        }
        val full = when (ecosystem.key) {
            "Maven" -> "$ns:$n"
            "npm", "Packagist", "Go" -> if (ns != null) "$ns/$n" else n
            else -> n
        }
        return PurlPackage(ecosystem.key, key(ecosystem.key, full), byShortName = false)
    }

    /**
     * The name of a package as OSV gives it, in the spelling of the index: lower case (Maven `group:artifact`, npm
     * `@scope/name`, Go the module path); PyPI normalised as by PEP 503 (`Foo_Bar.baz` → `foo-bar-baz`).
     */
    fun key(ecosystem: String, name: String): String {
        val lower = name.trim().lowercase()
        return if (ecosystem == "PyPI") lower.replace(PYPI_SEPARATORS, "-") else lower
    }

    /** The artifact of a Maven package (`group:artifact`), for libraries without groupId; null for the others. */
    fun shortName(ecosystem: String, name: String): String? =
        if (ecosystem == "Maven") name.substringAfter(':', "").trim().lowercase().takeIf { it.isNotEmpty() } else null

    private fun decode(text: String): String =
        if (!text.contains('%')) text
        else runCatching { java.net.URLDecoder.decode(text.replace("+", "%2B"), Charsets.UTF_8) }.getOrDefault(text)

    /** The path may start with a literal `@` (npm scope, the spec writes `%40`). */
    private val PURL = Regex("""^pkg:/*([A-Za-z][A-Za-z0-9.+\-]*)/(@?[^@?#]+)(?:@[^?#]*)?(?:\?[^#]*)?(?:#.*)?$""")
    private val PYPI_SEPARATORS = Regex("[-_.]+")
}
