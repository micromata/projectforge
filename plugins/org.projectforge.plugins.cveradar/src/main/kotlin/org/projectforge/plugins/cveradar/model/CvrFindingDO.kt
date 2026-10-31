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

/**
 * A finding (MAT-02): a vulnerability that matches a technology of a component, for one instance (a component of the
 * baseline of a system gives one finding per instance of the system). Created and kept up to date by
 * [org.projectforge.plugins.cveradar.CvrMatchingService]; the only change by hand is discarding a false positive
 * (MAT-06), which the next matching run leaves alone. Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_FINDING",
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_finding_vulnerability_fk", columnList = "vulnerability_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_finding_component_fk", columnList = "component_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_finding_instance_fk", columnList = "instance_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_finding_technology_fk", columnList = "technology_fk"),
    ],
)
open class CvrFindingDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "vulnerability_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var vulnerability: CvrVulnerabilityDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "component_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var component: CvrComponentDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "instance_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var instance: CvrInstanceDO? = null

    /** The technology that matched best. */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "technology_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var technology: CvrTechnologyDO? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.finding.certainty")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var certainty: CvrFindingCertainty? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.column.status")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: CvrFindingStatus? = CvrFindingStatus.NEW

    /** What matched, e.g. the CPE with its version range. */
    @PropertyInfo(i18nKey = "plugins.cveradar.finding.matched")
    @get:Column(length = 1000)
    open var matched: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.finding.discardReason")
    @get:Column(name = "discard_reason", length = 4000)
    open var discardReason: String? = null

    val vulnerabilityId: Long?
        @Transient
        get() = vulnerability?.id

    val componentId: Long?
        @Transient
        get() = component?.id

    val instanceId: Long?
        @Transient
        get() = instance?.id

    val technologyId: Long?
        @Transient
        get() = technology?.id
}
