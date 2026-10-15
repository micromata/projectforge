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

import org.projectforge.business.PfCaches
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Users with system access whose password wasn't changed for more than [NotificationParams.passwordMaxAgeDays] days
 * (the creation of the user counts, if the password was never changed). Only password changes in ProjectForge are
 * known. Each password change is a period of its own: resolved once the password is changed, notified again after the
 * new password got too old.
 */
@Service
class PasswordNotChangedRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override val ruleType = NotificationRuleType.PASSWORD_NOT_CHANGED

    override val variables = listOf("passwordChangeDate", "passwordAgeDays", "passwordMaxAgeDays", "passwordLink")

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation {
        val maxAgeDays = rule.readParams().passwordMaxAgeDays
        val affected = mutableMapOf<Long, Map<String, String>>()
        val periodKeys = mutableMapOf<Long, String>()
        userGroupCache.allUsers.filter { it.hasSystemAccess() }.forEach { user ->
            val userId = user.id ?: return@forEach
            periodKeys[userId] = periodKey(user)
            if (isTooOld(user, maxAgeDays, today)) {
                affected[userId] = variables(user, maxAgeDays, today)
            }
        }
        return NotificationRuleHandler.Evaluation(
            PERIOD_KEY_PREFIX, affected, CHANGE_PASSWORD_PATH, userPeriodKeys = periodKeys,
        )
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        return variables(user, rule.readParams().passwordMaxAgeDays, today)
    }

    /** Resolved if the password was changed (or isn't too old any more), expired if the user has no access any more. */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val maxAgeDays = rule.readParams().passwordMaxAgeDays
        val result = mutableMapOf<Long, NotificationStatus>()
        notifications.forEach { notification ->
            val user = caches.getUser(notification.recipientId)
            if (user == null || !user.hasSystemAccess()) {
                result[notification.id!!] = NotificationStatus.EXPIRED
            } else if (notification.periodKey != periodKey(user) || !isTooOld(user, maxAgeDays, today)) {
                result[notification.id!!] = NotificationStatus.RESOLVED
            }
        }
        return result
    }

    private fun variables(user: PFUserDO, maxAgeDays: Int, today: LocalDate): Map<String, String> {
        val locale = UserLocale.determineUserLocale(user)
        val lastChange = lastChange(user)?.let { NotificationService.toLocalDate(it) }
        return mapOf(
            "passwordChangeDate" to (lastChange?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: "–"),
            "passwordAgeDays" to (lastChange?.let { ChronoUnit.DAYS.between(it, today).toString() } ?: "–"),
            "passwordMaxAgeDays" to maxAgeDays.toString(),
            "passwordLink" to notificationLinks.absolute(CHANGE_PASSWORD_PATH),
        )
    }

    companion object {
        /**
         * The change password page (path of the next app, see [NotificationLinks]). Not the menu url
         * (`next/changePassword`): [NotificationLinks.absolute] would prefix it with `next/` once more.
         */
        const val CHANGE_PASSWORD_PATH = "/changePassword"

        private const val PERIOD_KEY_PREFIX = "password"

        /** The last password change, the creation of the user if never changed. */
        internal fun lastChange(user: PFUserDO): Date? = user.lastPasswordChange ?: user.created

        /** Older than [maxAgeDays] days (a user without any date as well). */
        internal fun isTooOld(user: PFUserDO, maxAgeDays: Int, today: LocalDate): Boolean {
            val lastChange = lastChange(user) ?: return true
            return ChronoUnit.DAYS.between(NotificationService.toLocalDate(lastChange), today) > maxAgeDays
        }

        /** The period of the user: the time of the last password change (or of the creation). */
        internal fun periodKey(user: PFUserDO): String =
            "$PERIOD_KEY_PREFIX-${lastChange(user)?.time ?: "never"}"
    }
}
