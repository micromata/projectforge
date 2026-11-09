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

import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.sync.NvdCpeParser
import org.projectforge.plugins.cveradar.sync.OsvPackages

/**
 * Rules for an entry of the CPE index ([org.projectforge.plugins.cveradar.model.CvrCpeMatchDO]) against a library
 * known by its purl, against false positives of a match by name (MAT-12):
 * - the platform: an entry for a runtime (`target_sw` `node.js`, `rust`, `python`, …) meets only libraries of that
 *   ecosystem: `isaacs:tar:…:node.js` isn't the Debian package `tar`, `uutils:coreutils:…:rust` not `coreutils`,
 * - the vendor, for a library of an ecosystem whose advisories are imported from OSV (they know its package): by name
 *   only, the vendor of the entry has to fit the package (its name, scope or groupId) unless the platform does:
 *   `adobe:connect` isn't the npm package `connect`, `apple:xcode` not `xcode`; `fasterxml:jackson-databind` is
 *   `com.fasterxml.jackson.core:jackson-databind`.
 */
internal object CpeLibraryRules {
    /** A library as the rules see it: the purl type (`npm`, `deb`, …), the parts of its namespace and its name. */
    class Library(val type: String, val namespace: List<String>, val name: String)

    /** The library of the purl of a technology or of its catalogue node; null without one. */
    fun libraryOf(purl: String?, node: CvrCatalogNodeDO?): Library? {
        OsvPackages.partsOf(purl)?.let { return Library(it.type, split(it.namespace), it.name) }
        val type = node?.purlType?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val name = node.purlName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return Library(type, split(node.purlNamespace), name)
    }

    /** The `target_sw` of the CPE of the entry, lower case; null for any (`*`, `-`) and for the names of the EUVD. */
    fun targetSoftware(criteria: String?): String? {
        if (criteria?.startsWith("cpe:") != true) return null
        return NvdCpeParser.splitCpe(criteria).getOrNull(TARGET_SW)?.lowercase()?.takeIf { it != "*" && it != "-" && it.isNotEmpty() }
    }

    /**
     * Whether the platform of the entry admits the library: an unknown or no platform does; a known one only the purl
     * types of [PLATFORMS].
     */
    fun fitsPlatform(targetSoftware: String?, library: Library): Boolean =
        PLATFORMS[targetSoftware ?: return true]?.let { library.type in it } ?: true

    /** Whether the platform of the entry names the ecosystem of the library (`node.js` for `npm`). */
    fun namesPlatform(targetSoftware: String?, library: Library): Boolean =
        targetSoftware != null && PLATFORMS[targetSoftware]?.contains(library.type) == true

    /**
     * Whether the vendor of the entry fits the library, compared without separators (`webpack.js` as `webpackjs`) and
     * without the suffix `_project` of the CPE dictionary: it is the name or a part of the namespace, or one begins with
     * the other (`expressjs` and `express`); or it is the vendor the dictionary uses for the ecosystem (`python`).
     */
    fun fitsVendor(vendor: String?, library: Library): Boolean {
        val v = compact(vendor?.removeSuffix("_project") ?: return true)
        if (v.isEmpty()) return true
        val name = compact(library.name)
        return v == name || library.namespace.any { compact(it) == v } || (name.isNotEmpty() && (name.startsWith(v) || v.startsWith(name))) ||
                ECOSYSTEM_VENDORS[library.type]?.contains(v) == true
    }

    private fun split(namespace: String?): List<String> =
        namespace?.split('.', '/', '@')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    private fun compact(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private const val TARGET_SW = 10

    /** The runtimes of `target_sw` and the purl types of their libraries; an empty set: no library of a purl. */
    private val PLATFORMS: Map<String, Set<String>> = mapOf(
        "node.js" to setOf("npm"), "nodejs" to setOf("npm"), "node" to setOf("npm"),
        "python" to setOf("pypi"),
        "java" to setOf("maven"), "jenkins" to setOf("maven"),
        "rust" to setOf("cargo"),
        "ruby" to setOf("gem"), "rails" to setOf("gem"), "ruby_on_rails" to setOf("gem"),
        "php" to setOf("composer"), "wordpress" to setOf("composer"), "drupal" to setOf("composer"),
        "joomla" to setOf("composer"), "joomla!" to setOf("composer"), "typo3" to setOf("composer"),
        "go" to setOf("golang"), "golang" to setOf("golang"),
        ".net" to setOf("nuget"), ".net_framework" to setOf("nuget"), ".net_core" to setOf("nuget"), "asp.net" to setOf("nuget"),
        "android" to emptySet(), "iphone_os" to emptySet(), "ios" to emptySet(),
    )

    /** Vendors the CPE dictionary uses for the projects of an ecosystem (`python:pillow`). */
    private val ECOSYSTEM_VENDORS: Map<String, Set<String>> = mapOf(
        "pypi" to setOf("python"),
    )
}
