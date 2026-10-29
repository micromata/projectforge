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

import org.projectforge.plugins.cveradar.model.CvrCatalogKind
import org.projectforge.plugins.cveradar.model.CvrCatalogOrigin

// The JSON of CvrCatalogRest, mirrored by projectforge-next/lib/rs/cve-radar-catalog.ts.

/**
 * One row of the catalogue tree, flat: a node with several parents appears below each of them, [key] is the path
 * (`N3/N17`) and [parentKey] the path of the row above.
 */
class CatalogRow(
    val key: String,
    val id: Long,
    val parentKey: String?,
    val kind: CvrCatalogKind,
    val name: String,
    val aliases: String? = null,
    val description: String? = null,
    val ecosystemKey: String? = null,
    val cpeVendor: String? = null,
    val cpeProduct: String? = null,
    val purlType: String? = null,
    val purlNamespace: String? = null,
    val purlName: String? = null,
    val origin: CvrCatalogOrigin? = null,
    /** All parents of the node (not only the one of this row), for the edit dialog. */
    val parents: List<CatalogRef> = emptyList(),
    /** The technologies of the inventory referring to the node. */
    val usage: Int = 0,
    val lastUpdate: java.util.Date? = null,
)

class CatalogTree(
    val rows: List<CatalogRow>,
    /** The security team maintains the catalogue. */
    val canEdit: Boolean,
)

/** A node to pick, with what a technology of the inventory takes over from it. */
class CatalogRef(
    val id: Long,
    val displayName: String,
    val kind: CvrCatalogKind? = null,
    val name: String? = null,
    val vendor: String? = null,
    val ecosystem: String? = null,
    val cpe: String? = null,
    val purl: String? = null,
)

class CatalogNodeRequest(
    val kind: CvrCatalogKind? = null,
    val name: String? = null,
    val aliases: String? = null,
    val description: String? = null,
    val ecosystemKey: String? = null,
    val cpeVendor: String? = null,
    val cpeProduct: String? = null,
    val purlType: String? = null,
    val purlNamespace: String? = null,
    val purlName: String? = null,
    /** All parents of the node; the ones missing here are removed. */
    val parentIds: List<Long> = emptyList(),
)

class CatalogMergeRequest(val targetId: Long? = null)
