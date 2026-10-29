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

package org.projectforge.plugins.cveradar.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * An edge of the technology catalogue: [child] belongs to [parent] (a product to its ecosystem, a library to a
 * framework). A node may have several parents (a library of several frameworks, INV-28). A removed edge is marked
 * as deleted and revived when added again, so a pair exists once. Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_CATALOG_EDGE",
    uniqueConstraints = [
        UniqueConstraint(name = "unique_t_plugin_cveradar_catalog_edge", columnNames = ["parent_fk", "child_fk"]),
    ],
    indexes = [Index(name = "idx_fk_t_plugin_cveradar_catalog_edge_child_fk", columnList = "child_fk")],
)
open class CvrCatalogEdgeDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "parent_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var parent: CvrCatalogNodeDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "child_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var child: CvrCatalogNodeDO? = null

    val parentId: Long?
        @Transient
        get() = parent?.id

    val childId: Long?
        @Transient
        get() = child?.id
}
