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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * The [PushSender] the rest of the server uses: hands each push to the sender of the platform of the app (APNs for iOS,
 * FCM for Android). A further sender (a relay of the app's publisher) joins here.
 */
@Service
@Primary
class PushSenders : PushSender {
    @Autowired
    private lateinit var apns: ApnsPushSender

    @Autowired
    private lateinit var fcm: FcmPushSender

    override val isAvailable: Boolean
        get() = apns.isAvailable || fcm.isAvailable

    override fun send(device: AppDeviceDO, message: PushMessage): PushResult {
        val sender = if (isAndroid(device)) fcm else apns
        // A platform whose sender isn't configured: its token stays, the key may be configured later.
        return if (sender.isAvailable) sender.send(device, message) else PushResult.ERROR
    }

    companion object {
        /** The platform as the app reported it when it logged in (lowercase), see [AppDeviceDO.platform]. */
        internal fun isAndroid(device: AppDeviceDO) = device.platform.equals("android", ignoreCase = true)
    }
}
