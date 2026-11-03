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


package org.projectforge.plugins.cveradar

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import org.projectforge.plugins.cveradar.model.CvrTeamCheckAnswerDO
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The answers of the members to the team checks asking every member ([CvrTeamCheckDO.perPerson]) and the state of
 * the checks. No access checks here, the callers check.
 */
@Service
class CvrTeamCheckAnswerService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var answerDao: CvrTeamCheckAnswerDao

    /**
     * A team check with its answers: for the group (one answer in the check) or of every member, then open until every
     * active member answered (and no one is in progress).
     */
    class TeamCheckState(
        val check: CvrTeamCheckDO,
        /** The answers of every member (also of those not in the group any more); empty for an answer for the group. */
        val answers: List<CvrTeamCheckAnswerDO>,
        /** The active members of the group asked (only if every member answers). */
        val memberIds: Set<Long>,
    ) {
        private val answerByUser = answers.associateBy { it.userId }

        /** The active members without an answer or with one in progress. */
        val openMemberIds: List<Long> = memberIds.filter { id ->
            answerByUser[id]?.answer.let { it == null || it == CvrTeamAnswer.IN_PROGRESS }
        }

        /** The active members who answered. */
        val answered: Int
            get() = memberIds.count { it in answerByUser }

        val isOpen: Boolean
            get() = if (check.perPerson) {
                openMemberIds.isNotEmpty()
            } else {
                check.answer == null || check.answer == CvrTeamAnswer.IN_PROGRESS
            }

        /** The answers accepting the risk: they need the security officer (CLS-05). */
        val riskAccepted: Int
            get() = if (check.perPerson) {
                answers.count { it.userId in memberIds && it.answer == CvrTeamAnswer.RISK_ACCEPTED }
            } else if (check.answer == CvrTeamAnswer.RISK_ACCEPTED) 1 else 0

        fun answerOf(userId: Long?): CvrTeamCheckAnswerDO? = answerByUser[userId]
    }

    /** The states of the checks, with one query for the answers. */
    fun states(checks: List<CvrTeamCheckDO>): List<TeamCheckState> {
        val answers = answerDao.selectByChecks(checks.filter { it.perPerson }.mapNotNull { it.id })
            .groupBy { it.teamCheckId }
        return checks.map { check ->
            if (check.perPerson) {
                TeamCheckState(check, answers[check.id] ?: emptyList(), access.activeMemberIds(check.groupId))
            } else {
                TeamCheckState(check, emptyList(), emptySet())
            }
        }
    }

    fun state(check: CvrTeamCheckDO): TeamCheckState = states(listOf(check)).single()

    /** Stores the answer of the user, replacing the own earlier one. */
    fun answer(check: CvrTeamCheckDO, user: PFUserDO, answer: CvrTeamAnswer, comment: String?, date: LocalDate?) {
        val existing = answerDao.findByCheckAndUser(check.id!!, user.id!!)
        val entry = existing ?: CvrTeamCheckAnswerDO().also {
            it.teamCheck = CvrTeamCheckDO().also { c -> c.id = check.id }
            it.user = PFUserDO().also { u -> u.id = user.id }
        }
        entry.answer = answer
        entry.comment = comment
        entry.answerDate = date
        if (existing == null) {
            answerDao.insert(entry, checkAccess = false)
        } else {
            answerDao.update(entry, checkAccess = false)
        }
    }
}
