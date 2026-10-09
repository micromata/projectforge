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
import java.util.Date

/** Delivers a notification through one [channel] (a step of the delivery cascade). A Spring bean. */
interface NotificationChannelSender {
    val channel: NotificationChannel

    /**
     * Delivers the notification to its recipient.
     * @return Null if delivered, otherwise the reason why the step was skipped (e.g. no mail address).
     * @throws Exception if the delivery failed (e.g. the mail server isn't reachable).
     */
    fun deliver(notification: NotificationDO, recipient: PFUserDO, now: Date): String?
}
