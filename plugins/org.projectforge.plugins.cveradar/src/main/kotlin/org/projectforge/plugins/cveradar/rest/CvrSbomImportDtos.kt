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


package org.projectforge.plugins.cveradar.rest

// The JSON of the bulk import of SBOMs (CvrSbomImportRest), mirrored by projectforge-next/lib/rs/cve-radar-sbom-import.ts.

/** A repository (a system with a component) or a container image (a component of the images system of the area). */
enum class SbomImportKind { REPOSITORY, CONTAINER }

/**
 * NEW: the system or component is created; UPDATE: it exists in the area, its libraries are replaced; CONFLICT: the
 * code is taken outside the area; AGGREGATE: an SBOM listing the libraries of the others (left out by default);
 * TOO_LARGE: more libraries than one component takes; EMPTY: no library with a version.
 */
enum class SbomImportStatus { NEW, UPDATE, CONFLICT, AGGREGATE, TOO_LARGE, EMPTY }

class SbomImportTargetView(
    val key: String,
    val kind: SbomImportKind,
    val systemCode: String,
    val systemName: String,
    val componentCode: String,
    val componentName: String,
    /** The SBOMs in the zip, cut to a few; [sourceCount] counts all. */
    val sources: List<String>,
    val sourceCount: Int,
    val libraryCount: Int,
    /** Components left out for want of a purl with a version (files, unresolved declarations). */
    val withoutPurlCount: Int,
    val status: SbomImportStatus,
    /** Proposed for the import: new and updated ones with libraries. */
    val selected: Boolean,
    val selectable: Boolean,
    val aggregateOf: Int = 0,
    /** What changes in a component already stored; null for a new one. */
    val diff: SbomImportDiff? = null,
)

class SbomImportDiff(val added: Int, val removed: Int, val changed: Int, val unchanged: Int)

/** What was not read as an SBOM: other files, JSON that is no SBOM, files that couldn't be read (empty, broken). */
class SbomImportSkipped(val otherFiles: Int, val notSbom: Int, val invalid: Int)

class SbomImportPlanView(
    val importId: Int,
    val areaId: Long,
    val areaCode: String,
    val targets: List<SbomImportTargetView>,
    val documentCount: Int,
    val skipped: SbomImportSkipped,
    /** The systems of the area the zip has nothing for; they stay as they are. */
    val notInImport: List<String>,
)

/** A target to import, with the code and name of its node as edited: the system of a repository, the component of an image. */
class SbomImportSelection(val key: String? = null, val code: String? = null, val name: String? = null)

class SbomImportApplyRequest(val targets: List<SbomImportSelection> = emptyList())
