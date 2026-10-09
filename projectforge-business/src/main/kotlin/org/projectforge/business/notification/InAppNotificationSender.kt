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
import org.springframework.stereotype.Service
import java.util.Date

/** Shows the notification in the app from now on (toast, banner, counters), fetched by the app by polling. */
@Service
class InAppNotificationSender : NotificationChannelSender {
    override val channel = NotificationChannel.IN_APP

    override fun deliver(notification: NotificationDO, recipient: PFUserDO, now: Date): String? {
        if (notification.inAppSince == null) {
            notification.inAppSince = now
        }
        return null
    }
}
