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
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

/**
 * The answer of a member to a team check asking every member ([CvrTeamCheckDO.perPerson]): one per check and user,
 * a new answer of the user replaces the own one. Historised.
 */
@Entity
@Table(
    name = "T_PLUGIN_CVERADAR_TEAM_CHECK_ANSWER",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_plugin_cveradar_team_check_answer",
        columnNames = ["team_check_fk", "user_fk"],
    )],
    indexes = [
        Index(name = "idx_fk_t_plugin_cveradar_team_check_answer_user_fk", columnList = "user_fk"),
    ],
)
open class CvrTeamCheckAnswerDO : DefaultBaseDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "team_check_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var teamCheck: CvrTeamCheckDO? = null

    @PropertyInfo(i18nKey = "user")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.teamCheck.answer")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var answer: CvrTeamAnswer? = null

    /** Reason (not affected, risk accepted), version (fixed) or planned date (in progress). */
    @PropertyInfo(i18nKey = "comment")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    /** Date of the fix, or the planned one (in progress). */
    @PropertyInfo(i18nKey = "date")
    @get:Column(name = "answer_date")
    open var answerDate: LocalDate? = null

    val teamCheckId: Long?
        @Transient
        get() = teamCheck?.id

    val userId: Long?
        @Transient
        get() = user?.id
}
