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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.notification.NotificationPreview
import org.projectforge.business.notification.NotificationSeverity
import org.projectforge.business.user.AppDeviceDao
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Wakes the apps of a user when a notification becomes visible for them (the in-app step of its delivery). The push
 * shows the title and the beginning of the notification (its preview as the bell shows it, e.g. the chat message),
 * unless the server or the user keeps the content out of it ([PushContentPrefService]): a generic text then, the app
 * loads the content when opened. Runs in the background: a slow push service must not hold up the rule jobs or a chat
 * post.
 */
@Service
class PushNotifier {
    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var pushContentPrefService: PushContentPrefService

    @Autowired
    private lateinit var pushSender: PushSender

    @Autowired(required = false)
    private var previews: List<NotificationPreview> = emptyList()

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
        val (title, body) = content(notification, pushContentPrefService.contentAllowed(userId)) {
            preview(notification, recipient)
        }
        val message = PushMessage(
            title = title
                ?: I18nHelper.getLocalizedMessage(recipient, if (chat) "notification.push.chat.title" else "notification.push.title"),
            body = body
                ?: I18nHelper.getLocalizedMessage(recipient, if (chat) "notification.push.chat.text" else "notification.push.text"),
            badge = notificationDao.summary(userId).openCount,
            sound = notification.severity >= NotificationSeverity.IMPORTANT,
            link = notification.link,
            notificationId = notification.id,
            subtitle = domainService.plainDomain?.takeIf { it.isNotBlank() },
        )
        devices.forEach { device ->
            if (pushSender.send(device, message) == PushResult.UNREGISTERED) {
                log.info { "Push token of app device #${device.id} refused for good, forgetting it." }
                appDeviceDao.clearPushToken(device.pushToken)
            }
        }
    }

    /** The text the bell shows instead of the body (see NotificationRest), null without one. */
    private fun preview(notification: NotificationDO, recipient: PFUserDO): String? {
        return previews.firstNotNullOfOrNull {
            runCatching { it.preview(notification, recipient) }
                .onFailure { ex -> log.warn { "Preview of notification #${notification.id} failed: ${ex.message}" } }
                .getOrNull()
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val CHAT_MENU_BADGE = MenuItemDefId.CHAT.id

        /** Apple allows 4 KB for the payload; the lock screen shows a few lines anyway. */
        internal const val MAX_TITLE_LENGTH = 100
        internal const val MAX_BODY_LENGTH = 240

        private val BLOCK_TAGS = setOf("p", "div", "br", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre")

        /** The cells of a table row, separated by a blank. */
        private val CELL_TAGS = setOf("td", "th")

        /**
         * The title and the text of the push for [notification]: the text is the [preview] of the bell if there is one,
         * else the SMS text of the rule, else the html body as plain text. Each null if empty, both without
         * [withContent] (a generic text then); [preview] is only called with [withContent].
         */
        internal fun content(
            notification: NotificationDO,
            withContent: Boolean,
            preview: () -> String?,
        ): Pair<String?, String?> {
            if (!withContent) {
                return Pair(null, null)
            }
            val title = notification.title?.let { teaser(it, MAX_TITLE_LENGTH) }
            val body = preview()?.let { teaser(it, MAX_BODY_LENGTH) }
                ?: notification.smsText?.let { teaser(it, MAX_BODY_LENGTH) }
                ?: plainTeaser(notification.body, MAX_BODY_LENGTH)
            return Pair(title, body)
        }

        /**
         * The beginning of the html [html] as plain text: one line per paragraph, list item etc., at most [max]
         * characters. Null if there is no text.
         */
        internal fun plainTeaser(html: String?, max: Int): String? {
            if (html.isNullOrBlank()) {
                return null
            }
            val sb = StringBuilder()
            fun append(node: Node) {
                when (node) {
                    is TextNode -> sb.append(node.wholeText)
                    is Element -> {
                        val block = node.normalName() in BLOCK_TAGS
                        if (block) sb.append('\n')
                        node.childNodes().forEach { append(it) }
                        if (block) sb.append('\n') else if (node.normalName() in CELL_TAGS) sb.append(' ')
                    }
                }
            }
            Jsoup.parseBodyFragment(html).body().childNodes().forEach { append(it) }
            return teaser(sb.toString(), max)
        }

        /** [text] with blanks collapsed, without empty lines, cut to [max] characters (with an ellipsis). */
        internal fun teaser(text: String, max: Int): String? {
            val clean = text.lines()
                .map { it.replace(WHITESPACE, " ").trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
            if (clean.isEmpty()) {
                return null
            }
            if (clean.length <= max) {
                return clean
            }
            // Not in the middle of an emoji (a surrogate pair).
            val cut = clean.take(max - 1).let { if (it.last().isHighSurrogate()) it.dropLast(1) else it }
            return cut.trimEnd() + "…"
        }

        private val WHITESPACE = Regex("[ \t\u00a0]+")
    }
}
