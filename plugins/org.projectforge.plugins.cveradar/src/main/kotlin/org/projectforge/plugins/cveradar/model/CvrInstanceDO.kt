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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * An instance of a system, an installation (INV-04). Every system has at least one; the instance of the same name
 * (the system's code) is the default instance, which the tree doesn't show on its own. Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_INSTANCE",
    uniqueConstraints = [
        UniqueConstraint(name = "unique_t_plugin_cveradar_instance_system_code", columnNames = ["system_fk", "code"]),
    ],
    indexes = [Index(name = "idx_fk_t_plugin_cveradar_instance_system_fk", columnList = "system_fk")],
)
open class CvrInstanceDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "system_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var system: CvrSystemDO? = null

    /** The suffix of the code, unique per system, e.g. `HQ-PROD`; null for the default instance. */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.code")
    @get:Column(length = 50)
    open var code: String? = null

    /** A readable name, e.g. "Headquarters production"; null shows the name of the system. */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.name")
    @get:Column(length = 255)
    open var name: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.site")
    @get:Column(length = 100)
    open var site: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.stage")
    @get:Column(length = 100)
    open var stage: String? = null

    /** The tenant the instance is operated for, e.g. a company. */
    @PropertyInfo(i18nKey = "plugins.cveradar.instance.tenant")
    @get:Column(length = 255)
    open var tenant: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.instance.exposed")
    @get:Column(nullable = false)
    open var exposed: Boolean = false

    @PropertyInfo(i18nKey = "plugins.cveradar.instance.contact")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "contact_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var contact: PFUserDO? = null

    /** Null: as the system; may only raise it (INV-07). */
    @PropertyInfo(i18nKey = "plugins.cveradar.inventory.confidentiality")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var confidentiality: CvrConfidentiality? = null

    /** Null: as the system; may only raise it (INV-07). */
    @PropertyInfo(i18nKey = "plugins.cveradar.system.criticality")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var criticality: CvrCriticality? = null

    val systemId: Long?
        @Transient
        get() = system?.id
}
