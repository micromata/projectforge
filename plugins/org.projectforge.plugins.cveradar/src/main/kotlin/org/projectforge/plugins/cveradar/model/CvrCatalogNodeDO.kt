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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * A node of the technology catalogue (INV-28): an ecosystem (e.g. Java/Maven), a product or framework (Spring Boot,
 * PostgreSQL) or a library (jackson-databind). The nodes form a graph ([CvrCatalogEdgeDO]), shown as a tree; the
 * technologies of the inventory refer to them. Maintained by the security team, historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_CATALOG_NODE",
    uniqueConstraints = [
        UniqueConstraint(name = "unique_t_plugin_cveradar_catalog_node_ecosystem_key", columnNames = ["ecosystem_key"]),
    ],
)
open class CvrCatalogNodeDO : DefaultBaseDO() {
    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.kind")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var kind: CvrCatalogKind? = null

    @PropertyInfo(i18nKey = "name")
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    /** Other names, one per line (e.g. `Log4j 2` for `log4j-core`). */
    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.aliases")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var aliases: String? = null

    @PropertyInfo(i18nKey = "description")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    /** The ecosystem of the OSV data, e.g. `Maven` or `npm`; of an ecosystem only. */
    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.ecosystemKey")
    @get:Column(name = "ecosystem_key", length = 50)
    open var ecosystemKey: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.cpeVendor")
    @get:Column(name = "cpe_vendor", length = 100)
    open var cpeVendor: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.cpeProduct")
    @get:Column(name = "cpe_product", length = 100)
    open var cpeProduct: String? = null

    /** The purl type, e.g. `maven`; of an ecosystem the type of its packages. */
    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.purlType")
    @get:Column(name = "purl_type", length = 50)
    open var purlType: String? = null

    /** The purl namespace, e.g. the groupId `com.fasterxml.jackson.core`. */
    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.purlNamespace")
    @get:Column(name = "purl_namespace", length = 255)
    open var purlNamespace: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.purlName")
    @get:Column(name = "purl_name", length = 255)
    open var purlName: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.catalog.origin")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var origin: CvrCatalogOrigin? = CvrCatalogOrigin.MANUAL
}
