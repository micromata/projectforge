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

package org.projectforge.business.push

import org.projectforge.business.user.AppDeviceDO

/** A push to one app. The text is generic: Apple sees the payload, so no content of the notification is in it. */
class PushMessage(
    val title: String,
    val body: String,
    /** The number on the icon of the app: the visible notifications of the user. */
    val badge: Int,
    val sound: Boolean,
    /** The app path to open when the push is tapped (see NotificationDO.link). */
    val link: String?,
    val notificationId: Long?,
)

enum class PushResult {
    OK,

    /** The push service refused the token for good (app removed, wrong environment): forget it. */
    UNREGISTERED,

    /** Failed for now (network, push service down, key refused). */
    ERROR,
}

/** Sends pushes to the apps. A Spring bean; a relay of the app's publisher could be another implementation. */
interface PushSender {
    /** False if not configured (e.g. no APNs key): nothing is sent then. */
    val isAvailable: Boolean

    fun send(device: AppDeviceDO, message: PushMessage): PushResult
}
