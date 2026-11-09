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

    /**
     * The distributions with advisories of OSV.dev: the OSV key (its folder in the bucket), the namespace of the purl
     * (`pkg:deb/debian/...`) and the prefix of the ids of the advisories imported, one per CVE (`DEBIAN-CVE-…`); the
     * notices (DSA, DLA, USN) are skipped, they gather many CVEs. The ecosystem of an affected package is the release
     * (`Debian:12`, `Ubuntu:22.04:LTS`), its versions are compared as dpkg does
     * ([org.projectforge.plugins.cveradar.DpkgVersionComparator]). Not in the catalogue: one purl type (`deb`) for both.
     */
    class Distro(val key: String, val namespace: String, val idPrefix: String, private val release: Regex) {
        /** Whether the ecosystem is a release of this distribution (`Debian:12`); not `Ubuntu:Pro:…` or `…:FIPS`. */
        fun isRelease(ecosystem: String): Boolean = release.matches(ecosystem)
    }

    val DISTROS = listOf(
        Distro("Debian", "debian", "DEBIAN-CVE-", Regex("""Debian:\d+""")),
        Distro("Ubuntu", "ubuntu", "UBUNTU-CVE-", Regex("""Ubuntu:\d\d\.\d\d(:LTS)?""")),
    )

    private val byKey = ECOSYSTEMS.associateBy { it.key }
    private val byPurlType = ECOSYSTEMS.associateBy { it.purlType }
    private val distroByKey = DISTROS.associateBy { it.key }
    private val distroByNamespace = DISTROS.associateBy { it.namespace }

    fun ecosystem(key: String?): Ecosystem? = byKey[key]

    fun distro(key: String?): Distro? = distroByKey[key]

    /** Whether the OSV key is an ecosystem or a distribution that can be imported. */
    fun isImportable(key: String?): Boolean = ecosystem(key) != null || distro(key) != null

    /** The ecosystem or distribution an ecosystem of an affected package belongs to: `Debian` of `Debian:12`. */
    private fun family(ecosystem: String): String = ecosystem.substringBefore(':')

    /** Whether the ecosystem of an affected package is the release of a distribution (`Debian:12`). */
    fun isDistro(ecosystem: String?): Boolean = ecosystem != null && distroByKey.containsKey(family(ecosystem))

    /**
     * Whether an affected package of this ecosystem is imported if [imported] are the OSV keys configured: the
     * ecosystem itself, or a release of a distribution configured.
     */
    fun isImported(ecosystem: String, imported: Set<String>): Boolean =
        if (ecosystem in imported) byKey.containsKey(ecosystem)
        else distroByKey[family(ecosystem)]?.takeIf { it.key in imported }?.isRelease(ecosystem) == true

    /**
     * The release of a distribution as OSV names it, of the namespace and the `distro` qualifier of a purl: `debian`
     * with `debian-12`, `debian-12.9`, `12` or `bookworm` is `Debian:12`; `ubuntu` with `ubuntu-22.04` or `jammy` is
     * `Ubuntu:22.04:LTS` (an April release of an even year is LTS). Null if unknown.
     */
    fun release(namespace: String?, distro: String?): String? {
        val d = distroByNamespace[namespace?.lowercase()] ?: return null
        val value = distro?.trim()?.lowercase()?.removePrefix("${d.namespace}-")?.takeIf { it.isNotEmpty() } ?: return null
        return when (d.key) {
            "Debian" -> (DEBIAN_CODENAMES[value] ?: DEBIAN_VERSION.matchEntire(value)?.groupValues?.get(1))
                ?.let { "Debian:$it" }

            else -> (UBUNTU_CODENAMES[value] ?: value.takeIf { UBUNTU_VERSION.matches(it) })?.let {
                val lts = it.substring(0, 2).toInt() % 2 == 0 && it.endsWith(".04")
                "Ubuntu:$it${if (lts) ":LTS" else ""}"
            }
        }
    }

    /** Whether the OSV id of an advisory of the distribution is imported: one per CVE, see [Distro]. */
    fun isImportedId(distroKey: String, osvId: String): Boolean = distro(distroKey)?.let { osvId.startsWith(it.idPrefix) } ?: true

    private val DEBIAN_VERSION = Regex("""(\d+)(\.\d+)*""")
    private val UBUNTU_VERSION = Regex("""\d\d\.\d\d""")
    private val DEBIAN_CODENAMES = mapOf(
        "buster" to "10", "bullseye" to "11", "bookworm" to "12", "trixie" to "13", "forky" to "14", "duke" to "15",
    )
    private val UBUNTU_CODENAMES = mapOf(
        "bionic" to "18.04", "focal" to "20.04", "jammy" to "22.04", "noble" to "24.04", "plucky" to "25.04",
        "questing" to "25.10", "resolute" to "26.04",
    )

    /**
     * The package of a purl as looked up in the index: the [key] of its name, or (Maven without groupId, a jar
     * listing) the artifact as
     * [CvrPurlMatchDO.shortName][org.projectforge.plugins.cveradar.model.CvrPurlMatchDO.shortName].
     */
    data class PurlPackage(val ecosystem: String, val name: String, val byShortName: Boolean)

    /**
     * The package of a purl (`pkg:maven/group/artifact@1.0`, without version too); null for another type. A package
     * of a distribution (`pkg:deb/debian/curl@7.88.1-10?distro=debian-12`) belongs to its release ([release]); without
     * a known release it is null.
     */
    fun ofPurl(purl: String?): PurlPackage? {
        val parts = partsOf(purl) ?: return null
        if (parts.type == "deb") {
            val release = release(parts.namespace, parts.distro) ?: return null
            return PurlPackage(release, parts.name.trim().lowercase().takeIf { it.isNotEmpty() } ?: return null, byShortName = false)
        }
        return of(parts.type, parts.namespace, parts.name)
    }

    /** A purl split: the type in lower case, the namespace (segments joined by `/`), the name, the `distro` qualifier. */
    data class PurlParts(val type: String, val namespace: String?, val name: String, val distro: String?)

    fun partsOf(purl: String?): PurlParts? {
        val match = PURL.matchEntire(purl?.trim() ?: return null) ?: return null
        val segments = match.groupValues[2].trim('/').split('/').filter { it.isNotEmpty() }.map(::decode)
        if (segments.isEmpty()) return null
        val distro = match.groupValues[3].split('&').firstOrNull { it.startsWith("distro=") }?.substringAfter('=')?.let(::decode)
        return PurlParts(match.groupValues[1].lowercase(), segments.dropLast(1).joinToString("/").ifEmpty { null }, segments.last(), distro)
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
    private val PURL = Regex("""^pkg:/*([A-Za-z][A-Za-z0-9.+\-]*)/(@?[^@?#]+)(?:@[^?#]*)?(?:\?([^#]*))?(?:#.*)?$""")
    private val PYPI_SEPARATORS = Regex("[-_.]+")
}
