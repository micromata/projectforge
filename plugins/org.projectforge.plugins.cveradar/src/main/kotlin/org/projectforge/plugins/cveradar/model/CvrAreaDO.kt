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
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO

/**
 * An area of the inventory: the upper levels of the tree (e.g. customer → division → country), of any depth. Systems
 * hang below an area. The group sees and maintains the area with everything below it; the security officer can move
 * an area elsewhere, e.g. below a customer. Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_AREA",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_plugin_cveradar_area_code", columnNames = ["code"])],
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_area_parent_fk", columnList = "parent_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_area_group_fk", columnList = "group_fk"),
    ],
)
open class CvrAreaDO : DefaultBaseDO() {
    /** Null: a root of the tree. Changed by moving only. */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "parent_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var parent: CvrAreaDO? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.code")
    @get:Column(length = 50, nullable = false)
    open var code: String? = null

    @PropertyInfo(i18nKey = "name")
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    @PropertyInfo(i18nKey = "description")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    /** Sees and maintains the area and everything below; mandatory on a root, optional below (inherited). */
    @PropertyInfo(i18nKey = "group")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "group_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var group: GroupDO? = null

    val parentId: Long?
        @Transient
        get() = parent?.id

    val groupId: Long?
        @Transient
        get() = group?.id
}
