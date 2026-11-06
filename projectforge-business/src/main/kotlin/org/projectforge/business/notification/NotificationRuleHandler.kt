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
 * A rule type: describes it for the editor (name, [params], recipients, template) and evaluates its rules: who is
 * concerned (with the values of the variables per user), and whether a pending notification is resolved. A Spring
 * bean (also of a plugin), registered by its [ruleType] in [NotificationRuleRegistry]: the core knows no single type.
 */
interface NotificationRuleHandler {
    /** The result of a rule run. */
    class Evaluation(
        /** The period the run refers to, part of the dedup key: e.g. `2026-10` for the time sheets of October. */
        val periodKey: String,
        /** The users concerned by id, with the values of the variables. */
        val affected: Map<Long, Map<String, String>>,
        /**
         * The link of the notifications, e.g. to the monthly report (path of the app). [USER_ID] is replaced by the
         * id of each recipient, e.g. for the own monthly report.
         */
        val link: String? = null,
        /**
         * False, if the rule has no condition (a manual rule, [ManualRuleHandler]): all recipients are notified, regardless
         * of [NotificationRecipients.onlyAffected].
         */
        val restrictsRecipients: Boolean = true,
        /**
         * Periods of single users by id, overriding [periodKey]: e.g. the last password change, so a user is notified
         * again after the next change got too old.
         */
        val userPeriodKeys: Map<Long, String> = emptyMap(),
        /**
         * The variables of a recipient not in [affected], computed from the data of this run (instead of
         * [NotificationRuleHandler.variables], which would load them again for every recipient).
         */
        val variablesFor: ((PFUserDO) -> Map<String, String>)? = null,
        /**
         * The users of [affected] are the recipients, resolved by the handler from the data and the configured
         * recipients (e.g. the responsible team of a system plus the configured ones in copy); [restrictsRecipients]
         * and [NotificationRecipients.onlyAffected] don't apply then.
         */
        val affectedAreRecipients: Boolean = false,
    ) {
        /** The link for the given recipient. */
        fun linkFor(user: PFUserDO): String? = link?.replace(USER_ID, "${user.id}")

        /** The period of the given recipient. */
        fun periodKeyFor(user: PFUserDO): String = userPeriodKeys[user.id] ?: periodKey

        companion object {
            /** Placeholder in [link] for the id of the recipient. */
            const val USER_ID = "{userId}"
        }
    }

    /** The key of the type, stored in [NotificationRuleDO.ruleType] (at most 50 characters, unique). */
    val ruleType: String

    /** The name of the type. */
    val i18nKey: String
        get() = "notification.ruleTypes.$ruleType"

    /**
     * Is the type offered and run? E.g. not while its plugin is deactivated: its rules wait, the editor doesn't
     * offer the type (its texts aren't loaded either).
     */
    val isAvailable: Boolean
        get() = true

    /** The position of the type in the selection of the editor. */
    val order: Int
        get() = 1000

    /** The settings of the type, shown by the editor, checked by the service, read by the handler. */
    val params: List<NotificationRuleParam<*>>
        get() = emptyList()

    /** Who the recipients are, for the editor (the run decides by [Evaluation.affectedAreRecipients]). */
    val recipientMode: NotificationRecipientMode
        get() = NotificationRecipientMode.RECIPIENTS

    /** A hint of the editor on the recipients, e.g. who gets notified besides the configured ones. */
    val recipientsHintKey: String?
        get() = null

    /** Recipients must be configured (nobody is concerned by the rule itself). */
    val requiresRecipients: Boolean
        get() = false

    /**
     * Subject and text of a new rule of the type (i18n keys; variables written as `[key]`, braces are ICU syntax),
     * null for empty texts.
     */
    val templateSubjectKey: String?
        get() = null

    val templateTextKey: String?
        get() = null

    /**
     * May the recipients always mark a notification of the rule as done, e.g. a banner nothing resolves (a rule
     * without condition)?
     */
    fun isManualDoneForced(display: NotificationDisplay?): Boolean = false

    /** Warnings after saving the rule (i18n keys), e.g. a precondition switched off in the configuration. */
    fun warnings(rule: NotificationRuleDO): List<String> = emptyList()

    /** The keys of the type specific variables (`{{key}}`); the labels are `notification.variable.<key>`. */
    val variables: List<String>

    /** Null if there's nothing to notify about today (e.g. the vacation days don't expire soon). */
    fun evaluate(rule: NotificationRuleDO, today: LocalDate): Evaluation?

    /**
     * The values of the variables for a single user, e.g. a recipient not concerned (if the rule notifies all of its
     * recipients) or the editor sending a test to oneself.
     */
    fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String>

    /**
     * Example values of the [variables] for the editor of a rule, formatted for the given user as the real ones
     * (e.g. a list of two invoices): from synthetic data, without database access.
     */
    fun exampleVariables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> = emptyMap()

    /**
     * The display names of the entities referred to by the params of the rule by param name (e.g. `cvrCaseId` to the
     * case), for the editor: the rest module doesn't know the entities of a plugin.
     */
    fun paramDisplayNames(rule: NotificationRuleDO): Map<String, String> = emptyMap()

    /**
     * The errors of the params of the rule by param name (i18n keys), e.g. a referred entity not found, for the
     * validation of the editor: the rest module doesn't know the entities of a plugin.
     */
    fun paramErrors(rule: NotificationRuleDO): Map<String, String> = emptyMap()

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
