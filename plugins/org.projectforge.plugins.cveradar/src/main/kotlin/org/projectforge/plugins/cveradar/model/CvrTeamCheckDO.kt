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
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

/**
 * A team check (CLS-03): the question to a group whether a project or system (not yet in the inventory) is affected
 * by a case, answered by a member of the group like team feedback (FB-03). Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_TEAM_CHECK",
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_team_check_case_fk", columnList = "case_fk"),
        Index(name = "idx_fk_t_plugin_cveradar_team_check_group_fk", columnList = "group_fk"),
    ],
)
open class CvrTeamCheckDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "case_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var cvrCase: CvrCaseDO? = null

    /** The group asked; its members answer. */
    @PropertyInfo(i18nKey = "group")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "group_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var group: GroupDO? = null

    /** The project or system checked, e.g. its name or short code. */
    @PropertyInfo(i18nKey = "plugins.cveradar.teamCheck.subject")
    @get:Column(length = 255, nullable = false)
    open var subject: String? = null

    /** Null: not answered yet. */
    @PropertyInfo(i18nKey = "plugins.cveradar.teamCheck.answer")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var answer: CvrTeamAnswer? = null

    /** Reason (not affected, risk accepted), version (fixed) or planned date (in progress). */
    @PropertyInfo(i18nKey = "comment")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    /** Date of the fix, or the planned one (in progress). */
    @PropertyInfo(i18nKey = "date")
    @get:Column(name = "answer_date")
    open var answerDate: LocalDate? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "answered_by_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var answeredBy: PFUserDO? = null

    val caseId: Long?
        @Transient
        get() = cvrCase?.id

    val groupId: Long?
        @Transient
        get() = group?.id
}
