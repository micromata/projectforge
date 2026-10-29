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

import org.projectforge.plugins.cveradar.LibraryFormat
import org.projectforge.plugins.cveradar.model.CvrConfidentiality
import org.projectforge.plugins.cveradar.model.CvrCriticality
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.projectforge.plugins.cveradar.model.CvrSystemType
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind

// The JSON of CvrInventoryRest, mirrored by projectforge-next/lib/rs/cve-radar-inventory.ts.

enum class InventoryLevel { AREA, SYSTEM, INSTANCE, COMPONENT }

class TechnologyView(
    val id: Long,
    val kind: CvrTechnologyKind?,
    val vendor: String?,
    val product: String?,
    val version: String?,
    val ecosystem: String?,
    val cpe: String?,
    val purl: String?,
    val origin: CvrOrigin?,
    /** The node of the technology catalogue (INV-28); none: "not in catalogue". */
    val catalogNodeId: Long? = null,
    val catalogName: String? = null,
)

/**
 * One node of the tree, flat: [parentKey] points to the row above. [frameOnly] rows are the path to a node the user
 * may see, shown with code and name only (no values, no actions).
 */
class InventoryRow(
    val key: String,
    val id: Long,
    val level: InventoryLevel,
    val parentKey: String?,
    /** The full, hierarchical code (INV-06). */
    val code: String?,
    /** The own part of the code, as entered. */
    val shortCode: String?,
    val name: String?,
    val frameOnly: Boolean,
    /** Whether the user may change the node and add below it. */
    val canEdit: Boolean = false,
    val description: String? = null,
    /** The group set on the node itself. */
    val groupId: Long? = null,
    val groupName: String? = null,
    /** The group that counts (the own one, or the nearest above) and the code of the node it comes from. */
    val effectiveGroupName: String? = null,
    val effectiveGroupFrom: String? = null,
    val systemType: CvrSystemType? = null,
    val criticality: CvrCriticality? = null,
    val confidentiality: CvrConfidentiality? = null,
    val site: String? = null,
    val stage: String? = null,
    val tenant: String? = null,
    val exposed: Boolean = false,
    val contactId: Long? = null,
    val contactName: String? = null,
    /** An instance without a code of its own, the default instance (INV-04). */
    val defaultInstance: Boolean = false,
    val technologies: List<TechnologyView> = emptyList(),
    val lastUpdate: java.util.Date? = null,
)

class InventoryTree(
    val rows: List<InventoryRow>,
    /** Only the security officer moves an area to the root or below an area of others. */
    val isSecurityOfficer: Boolean,
    val sites: List<String>,
    val stages: List<String>,
)

class AreaRequest(
    /** When creating: the area above, null for a root. Ignored when changing (moving has its own endpoint). */
    val parentId: Long? = null,
    val code: String? = null,
    val name: String? = null,
    val description: String? = null,
    val groupId: Long? = null,
)

class SystemRequest(
    /** When creating: the area above. Ignored when changing. */
    val areaId: Long? = null,
    val code: String? = null,
    val name: String? = null,
    val description: String? = null,
    val type: CvrSystemType? = null,
    val criticality: CvrCriticality? = null,
    val confidentiality: CvrConfidentiality? = null,
    val groupId: Long? = null,
)

class InstanceRequest(
    /** When creating: the system. Ignored when changing. */
    val systemId: Long? = null,
    val code: String? = null,
    val name: String? = null,
    val site: String? = null,
    val stage: String? = null,
    val tenant: String? = null,
    val exposed: Boolean = false,
    val contactId: Long? = null,
    val confidentiality: CvrConfidentiality? = null,
    val criticality: CvrCriticality? = null,
)

class TechnologyRequest(
    /** Null: a new technology; otherwise the one of this component to change. */
    val id: Long? = null,
    val kind: CvrTechnologyKind? = null,
    val vendor: String? = null,
    val product: String? = null,
    val version: String? = null,
    val ecosystem: String? = null,
    val cpe: String? = null,
    val purl: String? = null,
    val catalogNodeId: Long? = null,
)

class ComponentRequest(
    /** When creating: exactly one of the baseline of a system or an instance. Ignored when changing. */
    val systemId: Long? = null,
    val instanceId: Long? = null,
    val code: String? = null,
    val name: String? = null,
    /** All technologies of the component; the ones missing here are removed. */
    val technologies: List<TechnologyRequest> = emptyList(),
)

/** The area to move below, null: to the root. */
class MoveRequest(val targetAreaId: Long? = null)

/** A list a component's libraries are read from (npm `package-lock.json`, output of Maven or Gradle). */
class LibraryFile(val name: String? = null, val text: String? = null)

/** [replace]: libraries read from lists earlier and missing in these files are removed; otherwise the files only add. */
class LibraryRequest(val files: List<LibraryFile> = emptyList(), val replace: Boolean = false)

class LibraryLine(val ecosystem: String, val group: String?, val name: String, val version: String)

/** What was found in a file; [format] null: not recognised. */
class LibraryFileView(val name: String, val format: LibraryFormat?, val count: Int)

/** The lines of [added] and [removed] are cut to a few; the counts are complete. */
class LibraryDiff(
    val files: List<LibraryFileView>,
    val added: List<LibraryLine>,
    val addedCount: Int,
    val removed: List<LibraryLine>,
    val removedCount: Int,
    val unchangedCount: Int,
)
