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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * A component, e.g. the application or the database of a system. Either of the baseline of a system (every instance
 * inherits it, INV-08) or the own one of an instance (INV-09): exactly one of [system] and [instance] is set.
 * Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_COMPONENT",
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_component_system_fk", columnList = "system_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_component_instance_fk", columnList = "instance_fk"),
    ],
)
open class CvrComponentDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "system_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var system: CvrSystemDO? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "instance_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var instance: CvrInstanceDO? = null

    /** Unique within its system (and instances), e.g. `DB`. */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.code")
    @get:Column(length = 50, nullable = false)
    open var code: String? = null

    @PropertyInfo(i18nKey = "name")
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    val systemId: Long?
        @Transient
        get() = system?.id

    val instanceId: Long?
        @Transient
        get() = instance?.id
}
