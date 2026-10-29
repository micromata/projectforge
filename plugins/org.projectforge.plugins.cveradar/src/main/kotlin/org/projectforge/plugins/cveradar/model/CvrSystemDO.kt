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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
 * A system of the inventory (INV-03) below an area, with its instances and the components of its baseline (INV-08).
 * Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_SYSTEM",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_plugin_cveradar_system_code", columnNames = ["code"])],
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_system_area_fk", columnList = "area_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_system_group_fk", columnList = "group_fk"),
    ],
)
open class CvrSystemDO : DefaultBaseDO() {
    /** Changed by moving only. */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "area_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var area: CvrAreaDO? = null

    /** Unique, the codes below the system are built on it (INV-06). */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.code")
    @get:Column(length = 50, nullable = false)
    open var code: String? = null

    @PropertyInfo(i18nKey = "name")
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    @PropertyInfo(i18nKey = "description")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.system.type")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var type: CvrSystemType? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.system.criticality")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var criticality: CvrCriticality? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.confidentiality")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30, nullable = false)
    open var confidentiality: CvrConfidentiality? = null

    /** Optional group of the system, in addition to those of its areas (INV-03). */
    @PropertyInfo(i18nKey = "group")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "group_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var group: GroupDO? = null

    val areaId: Long?
        @Transient
        get() = area?.id

    val groupId: Long?
        @Transient
        get() = group?.id
}
