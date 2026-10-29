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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.time.LocalDate

/**
 * A technology of a component with its version (INV-08): a product or an ecosystem (INV-23). The matching against
 * the vulnerabilities uses the CPE or the purl. Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_TECHNOLOGY",
    indexes = [Index(name = "idx_fk_t_plugin_cveradar_technology_component_fk", columnList = "component_fk")],
)
open class CvrTechnologyDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "component_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var component: CvrComponentDO? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.kind")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var kind: CvrTechnologyKind? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.vendor")
    @get:Column(length = 100)
    open var vendor: String? = null

    /** The product, or the ecosystem (e.g. `maven`). */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.product")
    @get:Column(length = 255, nullable = false)
    open var product: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.version")
    @get:Column(name = "technology_version", length = 100)
    open var version: String? = null

    /** The ecosystem a product belongs to, e.g. `maven` for Spring Boot (INV-23). */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.ecosystem")
    @get:Column(length = 50)
    open var ecosystem: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.cpe")
    @get:Column(length = 500)
    open var cpe: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.purl")
    @get:Column(length = 500)
    open var purl: String? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var origin: CvrOrigin? = CvrOrigin.MANUAL

    @get:Column(name = "origin_date")
    open var originDate: LocalDate? = null

    val componentId: Long?
        @Transient
        get() = component?.id
}
