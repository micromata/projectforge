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


package org.projectforge.rest.notification

import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.notification.NotificationDisplay
import org.projectforge.business.notification.NotificationSeverity
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * The notifications of the logged-in user shown in the app (toasts, banners, the bell of the top navigation), and
 * their confirmation. The app polls the summary via the user status (UserStatusRest) and loads them from here only if
 * it changed.
 */
@RestController
@RequestMapping("${Rest.URL}/notification")
class NotificationRest {
    class Notification(
        val id: Long?,
        val severity: NotificationSeverity,
        val display: NotificationDisplay,
        val status: NotificationStatus,
        /** May the recipient mark it as done (a banner, which is otherwise resolved by its rule only)? */
        val manualDone: Boolean,
        val title: String?,
        /** Sanitized HTML. */
        val body: String?,
        /** A path of the app, e.g. `next/monthlyEmployeeReport`. */
        val link: String?,
        val created: Date?,
    )

    @Autowired
    private lateinit var notificationDao: NotificationDao

    /** The notifications of the logged-in user visible in the app, the newest first. */
    @AccessChecked("Own user only (notifications of the logged-in user)")
    @GetMapping("my")
    fun getMy(): List<Notification> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return notificationDao.findVisible(user.id!!).map { toDTO(it) }
    }

    /** Confirms the notification: no further escalation; a toast disappears, a banner stays until done. */
    @AccessChecked("Own user only (NotificationDao.acknowledge checks the recipient)")
    @PostMapping("{id}/acknowledge")
    fun acknowledge(@PathVariable("id") id: Long): Notification? {
        return notificationDao.acknowledge(id, ThreadLocalUserContext.requiredLoggedInUser)?.let { toDTO(it) }
    }

    /** Marks the notification as done (if its rule allows it, see [NotificationDO.manualDone]). */
    @AccessChecked("Own user only (NotificationDao.done checks the recipient)")
    @PostMapping("{id}/done")
    fun done(@PathVariable("id") id: Long): Notification? {
        return notificationDao.done(id, ThreadLocalUserContext.requiredLoggedInUser)?.let { toDTO(it) }
    }

    private fun toDTO(notification: NotificationDO): Notification {
        return Notification(
            id = notification.id,
            severity = notification.severity,
            display = notification.display,
            status = notification.status,
            manualDone = notification.isManualDoneAllowed,
            title = notification.title,
            body = notification.body,
            link = notification.link,
            created = notification.created,
        )
    }
}
