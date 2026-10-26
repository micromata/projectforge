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

package org.projectforge.business.chat

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * A message of a conversation, or a reply in the thread of a message ([threadRootId]). Threads are one level deep: the
 * root of a reply is always a message of the conversation itself.
 *
 * Edits, deletion (a placeholder stays, [deleted]) and hiding by a moderator are historised. The counters, reactions
 * and followers change with the actions of others and are written by [ChatService] only, without history.
 */
@Entity
@Table(
    name = "T_CHAT_MESSAGE",
    indexes = [
        Index(name = "idx_t_chat_message_conversation_seq", columnList = "conversation_fk, change_seq"),
        Index(name = "idx_t_chat_message_conversation_root", columnList = "conversation_fk, thread_root_fk, pk"),
    ],
)
open class ChatMessageDO : DefaultBaseDO() {
    @get:Column(name = "conversation_fk", nullable = false)
    open var conversationId: Long? = null

    @get:Column(name = "author_fk", nullable = false)
    open var authorId: Long? = null

    /** The message whose thread this reply belongs to, null for a message of the conversation. */
    @get:Column(name = "thread_root_fk")
    open var threadRootId: Long? = null

    /** The sanitized HTML ([ChatHtmlSanitizer]). */
    @get:Column(name = "content", length = 100000)
    open var content: String? = null

    /** The plain text of [content] (cut), for the search. */
    @NoHistory
    @get:Column(name = "content_text", length = CONTENT_TEXT_LENGTH)
    open var contentText: String? = null

    /** JSON: the files of the message in the data transfer, see [readAttachments] and [ChatFileService]. */
    @get:Column(name = "attachments", length = 10000)
    open var attachments: String? = null

    /** The users mentioned (csv of ids, checked against the readers), notified once each. */
    @NoHistory
    @get:Column(name = "mentioned_ids", length = 4000)
    open var mentionedIds: String? = null

    /** The time of the last edit by the author, null if never edited. */
    @get:Column(name = "edited_at")
    open var editedAt: Date? = null

    /** The moderator who hid the message, null if not hidden. */
    @get:Column(name = "hidden_by_fk")
    open var hiddenById: Long? = null

    @get:Column(name = "hidden_at")
    open var hiddenAt: Date? = null

    @get:Column(name = "hidden_reason", length = 1000)
    open var hiddenReason: String? = null

    /** The number of replies of the thread (of a message of the conversation). */
    @NoHistory
    @get:Column(name = "reply_count", nullable = false)
    open var replyCount: Int = 0

    @NoHistory
    @get:Column(name = "last_reply_at")
    open var lastReplyAt: Date? = null

    /** The [ChatConversationDO.changeSeq] of the last change of this message (also of its reactions or thread). */
    @NoHistory
    @get:Column(name = "change_seq", nullable = false)
    open var changeSeq: Long = 0

    /** JSON: emoji -> ids of the users who reacted with it, see [readReactions]. */
    @NoHistory
    @get:Column(name = "reactions", length = 10000)
    open var reactions: String? = null

    /** The users following the thread explicitly or by taking part in it (csv of ids). */
    @NoHistory
    @get:Column(name = "follower_ids", length = 10000)
    open var followerIds: String? = null

    /** The users who unfollowed the thread: they aren't made followers again by replying (csv of ids). */
    @NoHistory
    @get:Column(name = "unfollower_ids", length = 10000)
    open var unfollowerIds: String? = null

    @get:Transient
    @get:JsonIgnore
    open val isHidden: Boolean
        get() = hiddenById != null

    /** Emoji -> ids of the users, in the order of the first reaction of each emoji. */
    fun readReactions(): LinkedHashMap<String, List<Long>> {
        val json = reactions
        if (json.isNullOrBlank()) {
            return LinkedHashMap()
        }
        return try {
            val map = JsonUtils.fromJson(json, LinkedHashMap::class.java, failOnUnknownProps = false) ?: return LinkedHashMap()
            LinkedHashMap<String, List<Long>>().also { result ->
                map.forEach { (emoji, ids) ->
                    val userIds = (ids as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }
                    if (emoji is String && !userIds.isNullOrEmpty()) {
                        result[emoji] = userIds
                    }
                }
            }
        } catch (_: Exception) {
            LinkedHashMap()
        }
    }

    fun writeReactions(map: Map<String, List<Long>>) {
        reactions = map.filterValues { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let { JsonUtils.toJson(it) }
    }

    fun readAttachments(): List<ChatAttachment> {
        val json = attachments
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            JsonUtils.fromJson(json, Array<ChatAttachment>::class.java, failOnUnknownProps = false)?.toList() ?: emptyList()
        } catch (ex: Exception) {
            log.error { "Attachments of chat message #$id not readable: ${ex.message}" }
            emptyList()
        }
    }

    fun writeAttachments(list: List<ChatAttachment>) {
        attachments = list.takeIf { it.isNotEmpty() }?.let { JsonUtils.toJson(it) }
    }

    companion object {
        const val CONTENT_TEXT_LENGTH = 10000
    }
}
