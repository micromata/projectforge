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
import jakarta.persistence.UniqueConstraint
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * The rating of a case by one member of the security team (TRI-03): priority, exploitability in our own environment and
 * reason. One per person and case; the ratings stand side by side, the binding priority is set on the case
 * ([CvrCaseDO.priority]). Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_RATING",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_plugin_cveradar_rating_case_user", columnNames = ["case_fk", "user_fk"])],
    indexes = [Index(name = "idx_fk_t_plugin_cveradar_rating_user_fk", columnList = "user_fk")],
)
open class CvrRatingDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "case_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var cvrCase: CvrCaseDO? = null

    /** The rating person. */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.case.priority", required = true)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 5, nullable = false)
    open var priority: CvrPriority? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.rating.exploitability", required = true)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var exploitability: CvrExploitability? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.rating.reason")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var reason: String? = null
}
