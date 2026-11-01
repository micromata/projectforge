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

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.notification.NotificationSeverity
import org.projectforge.business.user.AppDeviceDao
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Wakes the apps of a user when a notification becomes visible for them (the in-app step of its delivery). The text is
 * generic ([PushMessage]), the app loads the content when opened. Runs in the background: a slow push service must not
 * hold up the rule jobs or a chat post.
 */
@Service
class PushNotifier {
    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var pushSender: PushSender

    fun notifyAsync(notification: NotificationDO, recipient: PFUserDO) {
        if (!pushSender.isAvailable) {
            return
        }
        scope.launch {
            try {
                notify(notification, recipient)
            } catch (ex: Exception) {
                log.warn { "Push for notification #${notification.id} failed: ${ex.message}" }
            }
        }
    }

    internal fun notify(notification: NotificationDO, recipient: PFUserDO) {
        val userId = recipient.id ?: return
        val devices = appDeviceDao.getPushDevices(userId)
        if (devices.isEmpty()) {
            return
        }
        val chat = notification.menuBadge == CHAT_MENU_BADGE
        val message = PushMessage(
            title = I18nHelper.getLocalizedMessage(recipient, if (chat) "notification.push.chat.title" else "notification.push.title"),
            body = I18nHelper.getLocalizedMessage(recipient, if (chat) "notification.push.chat.text" else "notification.push.text"),
            badge = notificationDao.summary(userId).openCount,
            sound = notification.severity >= NotificationSeverity.IMPORTANT,
            link = notification.link,
            notificationId = notification.id,
        )
        devices.forEach { device ->
            if (pushSender.send(device, message) == PushResult.UNREGISTERED) {
                log.info { "Push token of app device #${device.id} refused for good, forgetting it." }
                appDeviceDao.clearPushToken(device.pushToken)
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val CHAT_MENU_BADGE = MenuItemDefId.CHAT.id
    }
}
