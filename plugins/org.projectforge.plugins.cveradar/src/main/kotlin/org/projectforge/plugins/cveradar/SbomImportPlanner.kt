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

import org.projectforge.plugins.cveradar.rest.SbomImportKind
import org.projectforge.plugins.cveradar.rest.SbomImportStatus

/** An SBOM found in the zip of a bulk import: its path in the zip and what was read from it. */
class SbomImportDocument(val path: String, val document: SbomDocument)

/**
 * Where the SBOMs of one repository or container image go: a system of the area with the component [componentCode]
 * of its baseline. A repository is a system of its own, a container image a component of the one system of the images
 * of the area. [files] are its SBOMs, named by their path in the zip.
 */
class SbomImportTarget(
    val key: String,
    val kind: SbomImportKind,
    val systemCode: String,
    val systemName: String,
    val componentCode: String,
    val componentName: String,
    val files: List<ParsedLibraryFile>,
    val libraryCount: Int,
    val withoutPurl: Int,
    val status: SbomImportStatus,
    /** For an aggregate: the number of the other targets whose libraries it lists. */
    val aggregateOf: Int = 0,
) {
    /** Has libraries and not more than a component takes; a conflict may be resolved by another code. */
    val selectable: Boolean
        get() = libraryCount in 1..LibraryListParser.MAX_ENTRIES

    /** Proposed for the import: the new and updated ones. */
    val proposed: Boolean
        get() = selectable && (status == SbomImportStatus.NEW || status == SbomImportStatus.UPDATE)
}

/**
 * What of the inventory a plan needs: the code of the area, the systems in it by code with the codes of their
 * baseline components, and the codes taken elsewhere (other areas and their systems, one name space, INV-06).
 */
class SbomImportInventory(
    val areaCode: String,
    val systems: Map<String, Set<String>>,
    val takenCodes: Set<String>,
)

/**
 * Proposes the targets of the SBOMs of a bulk import (INV-35), independent of the tool that wrote them:
 * - an SBOM of a container image (`metadata.component.type` `container`) belongs to its image (name and tag),
 *   wherever it is in the zip;
 * - every other SBOM belongs to the uppermost folder on its path that holds an SBOM itself (`repositories/a/bom.json`,
 *   `repositories/a/native/x/bom.json` → `a`), so the SBOMs of the modules of a repository stay together;
 * - an SBOM in the top folder is a target of its own, named by its subject or file name. One listing the libraries of
 *   others (an aggregate of all of them) is recognised and left out by default.
 * The codes are proposed: free ones, or the ones of the systems already in the area (then they are updated).
 */
object SbomImportPlanner {
    const val COMPONENT_CODE = "SBOM"
    private const val CODE_LENGTH = 50
    private val NOT_IN_CODE = Regex("[^A-Z0-9._-]+")
    private val REPEATED_DASH = Regex("-{2,}")
    private val SBOM_EXTENSIONS = listOf(".cdx.json", ".spdx.json", ".bom.json", ".json")

    /** An aggregate lists at least so many of the libraries of the others, and most of the libraries of at least two. */
    private const val AGGREGATE_COVERED = 0.95
    private const val AGGREGATE_CONTAINS = 0.9
    private const val AGGREGATE_MIN_LIBRARIES = 5

    fun plan(
        documents: List<SbomImportDocument>,
        inventory: SbomImportInventory,
        imagesSystemName: String,
    ): List<SbomImportTarget> {
        val root = commonRoot(documents.map { it.path })
        val docs = documents.map { SbomImportDocument(it.path.removePrefix(root), it.document) }
        val unitDirs = docs.filter { imageOf(it) == null }.map { parentOf(it.path) }.filter { it.isNotEmpty() }.toSet()
        // Key → kind, name, documents; in the order of the zip.
        val groups = LinkedHashMap<String, Triple<SbomImportKind, String, MutableList<SbomImportDocument>>>()
        docs.forEach { doc ->
            val image = imageOf(doc)
            val (key, kind, name) = when {
                image != null -> Triple("C:${image.lowercase()}", SbomImportKind.CONTAINER, image)
                parentOf(doc.path).isEmpty() -> Triple("F:${doc.path}", SbomImportKind.REPOSITORY, topLevelName(doc))
                else -> unitOf(doc.path, unitDirs).let { Triple("R:$it", SbomImportKind.REPOSITORY, it.substringAfterLast('/')) }
            }
            groups.getOrPut(key) { Triple(kind, name, mutableListOf()) }.third.add(doc)
        }
        val libraries = groups.mapValues { (_, group) ->
            group.third.flatMap { it.document.entries }.map { it.copy(location = null, distro = null, source = null).purl }.toSet()
        }
        val aggregates = aggregates(groups.keys.filter { it.startsWith("F:") }, libraries)
        val codes = Codes(inventory)
        val imagesCode = codes.imagesSystem()
        return groups.map { (key, group) ->
            val (kind, name, list) = group
            val files = list.map { ParsedLibraryFile(it.path, it.document.format, it.document.entries) }
            val count = files.flatMap { it.entries }.map { it.purl.take(500) }.distinct().size
            val withoutPurl = list.sumOf { it.document.withoutPurl }
            val proposal = if (kind == SbomImportKind.CONTAINER) codes.image(imagesCode, name) else codes.repository(name)
            val status = when {
                key in aggregates -> SbomImportStatus.AGGREGATE
                count > LibraryListParser.MAX_ENTRIES -> SbomImportStatus.TOO_LARGE
                count == 0 -> SbomImportStatus.EMPTY
                else -> proposal.status
            }
            SbomImportTarget(
                key = key,
                kind = kind,
                systemCode = proposal.systemCode,
                systemName = if (kind == SbomImportKind.CONTAINER) imagesSystemName else name.take(255),
                componentCode = proposal.componentCode,
                componentName = if (kind == SbomImportKind.CONTAINER) name.take(255) else COMPONENT_CODE,
                files = files,
                libraryCount = count,
                withoutPurl = withoutPurl,
                status = status,
                aggregateOf = aggregates[key] ?: 0,
            )
        }
    }

    /**
     * A code as the inventory takes it (`^[A-Z0-9][A-Z0-9._-]{0,49}$`): upper case, every other character a dash; a
     * longer one is cut and gets a hash of the full text, so two long names stay apart.
     */
    fun code(text: String): String {
        val cleaned = text.uppercase().replace(NOT_IN_CODE, "-").replace(REPEATED_DASH, "-")
            .trimStart('-', '.', '_').trimEnd('-').ifEmpty { "X" }
        if (cleaned.length <= CODE_LENGTH) return cleaned
        val hash = Integer.toHexString(text.hashCode()).uppercase().padStart(8, '0')
        return cleaned.take(CODE_LENGTH - hash.length - 1).trimEnd('-', '.', '_') + "-" + hash
    }

    private class Proposal(val systemCode: String, val componentCode: String, val status: SbomImportStatus)

    /** The codes proposed so far, so two targets never get the same. */
    private class Codes(val inventory: SbomImportInventory) {
        private val usedSystems = mutableSetOf<String>()
        private val usedImages = mutableSetOf<String>()

        /** The system of the images; one of another area makes the next free code (`-2`, ...), as it can't be edited. */
        fun imagesSystem(): String {
            val code = code("${inventory.areaCode}-IMAGES")
            var candidate = code
            var n = 1
            while (candidate in inventory.takenCodes) {
                val suffix = "-${++n}"
                candidate = code.take(CODE_LENGTH - suffix.length) + suffix
            }
            usedSystems.add(candidate)
            return candidate
        }

        fun repository(name: String): Proposal {
            // A code of another area or of a system there: the code of the area in front.
            val code = code(name).takeIf { it !in inventory.takenCodes }
                ?: code("${inventory.areaCode}-$name")
            val unique = unique(code, usedSystems)
            val status = when {
                unique in inventory.takenCodes -> SbomImportStatus.CONFLICT
                unique in inventory.systems -> SbomImportStatus.UPDATE
                else -> SbomImportStatus.NEW
            }
            return Proposal(unique, COMPONENT_CODE, status)
        }

        fun image(imagesCode: String, image: String): Proposal {
            val component = unique(code(image), usedImages)
            val status = when {
                inventory.systems[imagesCode]?.contains(component) == true -> SbomImportStatus.UPDATE
                else -> SbomImportStatus.NEW
            }
            return Proposal(imagesCode, component, status)
        }

        private fun unique(code: String, used: MutableSet<String>): String {
            var candidate = code
            var n = 1
            while (!used.add(candidate)) {
                val suffix = "-${++n}"
                candidate = code.take(CODE_LENGTH - suffix.length) + suffix
            }
            return candidate
        }
    }

    /**
     * The targets of [candidates] (SBOMs in the top folder) that are aggregates of the others, with the number of the
     * targets each lists: almost all its libraries are in other targets, and it lists most of those of at least two.
     */
    private fun aggregates(candidates: List<String>, libraries: Map<String, Set<String>>): Map<String, Int> =
        candidates.mapNotNull { key ->
            val own = libraries[key].orEmpty()
            if (own.isEmpty()) return@mapNotNull null
            val others = libraries.filterKeys { it != key }.values
            val elsewhere = others.fold(HashSet<String>()) { all, set -> all.apply { addAll(set) } }
            val covered = own.count { it in elsewhere }.toDouble() / own.size
            val contained = others.count { set ->
                set.size >= AGGREGATE_MIN_LIBRARIES && set.count { it in own } >= AGGREGATE_CONTAINS * set.size
            }
            if (covered >= AGGREGATE_COVERED && contained >= 2) key to contained else null
        }.toMap()

    /** The image of an SBOM of a container: `name:tag`; null for any other SBOM. */
    private fun imageOf(doc: SbomImportDocument): String? {
        val subject = doc.document.subject ?: return null
        if (subject.type != "container") return null
        val name = subject.name ?: return null
        val tagged = subject.version != null && !name.substringAfterLast('/').contains(':') && !name.contains('@')
        return if (tagged) "$name:${subject.version}" else name
    }

    /** An SBOM in the top folder: the name of its subject (the last part of a path), else its file name. */
    private fun topLevelName(doc: SbomImportDocument): String {
        val subject = doc.document.subject?.name?.trimEnd('/', '\\')?.substringAfterLast('/')?.substringAfterLast('\\')
        if (!subject.isNullOrBlank()) return subject
        val file = doc.path.substringAfterLast('/')
        return SBOM_EXTENSIONS.firstOrNull { file.lowercase().endsWith(it) }?.let { file.dropLast(it.length) }
            ?.takeIf { it.isNotBlank() } ?: file
    }

    /** The uppermost folder on the path holding an SBOM itself. */
    private fun unitOf(path: String, unitDirs: Set<String>): String {
        val dir = parentOf(path)
        val segments = dir.split('/')
        return (1..segments.size).map { segments.take(it).joinToString("/") }.firstOrNull { it in unitDirs } ?: dir
    }

    private fun parentOf(path: String) = path.substringBeforeLast('/', "")

    /** The folders all paths share at the top (`all-repositories/`), with the slash; empty if there are none. */
    private fun commonRoot(paths: List<String>): String {
        if (paths.isEmpty()) return ""
        var root = ""
        while (true) {
            val rest = paths.map { it.removePrefix(root) }
            val first = rest.first().substringBefore('/', "")
            if (first.isEmpty() || rest.any { !it.startsWith("$first/") }) return root
            root += "$first/"
        }
    }
}
