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

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object NotificationLogEvents {
    @JvmField
    val RULE_FAILED = LogEvent(
        code = "notification.ruleFailed",
        category = LogCategory.BUG,
        explanation = "A notification rule couldn't be evaluated or its notifications couldn't be created. Its recipients aren't notified by this run.",
        action = "Check the rule (type, settings, recipients) by the message and the stack trace; the next run tries again.",
    )

    @JvmField
    val DELIVERY_FAILED = LogEvent(
        code = "notification.deliveryFailed",
        category = LogCategory.EXTERNAL,
        explanation = "A step of the delivery of a notification (e.g. a mail) failed. It is retried after 15 minutes, 1 hour and 4 hours (logged as warning), the last failure is logged as error and the step is given up; the error is stored in the notification's delivery state.",
        action = "Check the mail server (see also the mail tile) and the recipient's mail address.",
    )

    @JvmField
    val JOB_FAILED = LogEvent(
        code = "notification.jobFailed",
        category = LogCategory.BUG,
        explanation = "A job of the notification system (rule run or delivery) failed as a whole. No notifications were created or delivered by this run.",
        action = "Check the stack trace (e.g. the database); the job runs again by its schedule.",
    )
}
