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
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.projectforge.Constants
import java.util.Date

/**
 * The confirmation of an employee for a case released to the clients (FB-05, CLS-04): one per case and user, the
 * latest answer counts.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_CONFIRMATION",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_plugin_cveradar_confirmation",
        columnNames = ["case_fk", "user_fk"],
    )],
)
open class CvrConfirmationDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "case_fk", nullable = false)
    open var caseId: Long? = null

    @get:Column(name = "user_fk", nullable = false)
    open var userId: Long? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var answer: CvrClientAnswer? = null

    /** The question or problem (answer QUESTION) or a remark. */
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @get:Column(name = "last_update", nullable = false)
    open var lastUpdate: Date? = null
}
