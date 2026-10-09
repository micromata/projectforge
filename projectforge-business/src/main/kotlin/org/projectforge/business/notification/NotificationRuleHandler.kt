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


package org.projectforge.business.notification

import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

/**
 * Evaluates the rules of one [ruleType]: who is concerned (with the values of the variables per user), and
 * whether a pending notification is resolved. A Spring bean, found by [NotificationService] via its [ruleType].
 */
interface NotificationRuleHandler {
    /** The result of a rule run. */
    class Evaluation(
        /** The period the run refers to, part of the dedup key: e.g. `2026-10` for the time sheets of October. */
        val periodKey: String,
        /** The users concerned by id, with the values of the variables. */
        val affected: Map<Long, Map<String, String>>,
        /** The link of the notifications, e.g. to the monthly report (path of the app). */
        val link: String? = null,
        /**
         * False, if the rule has no condition ([NotificationRuleType.MANUAL]): all recipients are notified, regardless
         * of [NotificationRecipients.onlyAffected].
         */
        val restrictsRecipients: Boolean = true,
    )

    val ruleType: NotificationRuleType

    /** The keys of the type specific variables (`{{key}}`); the labels are `notification.variable.<key>`. */
    val variables: List<String>

    /** Null if there's nothing to notify about today (e.g. the vacation days don't expire soon). */
    fun evaluate(rule: NotificationRuleDO, today: LocalDate): Evaluation?

    /**
     * The values of the variables for a single user, e.g. a recipient not concerned (if the rule notifies all of its
     * recipients) or the editor sending a test to oneself.
     */
    fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String>

    /** May the user be a recipient of the run at all (e.g. employed in the referred month)? */
    fun isRelevant(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Boolean = true

    /**
     * The pending notifications of the rule, which are finished: [NotificationStatus.RESOLVED] or
     * [NotificationStatus.EXPIRED] by id. The others stay pending.
     */
    fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> = emptyMap()
}
