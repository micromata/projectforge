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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * The conversation about one host object in one channel ([ChatHost]), created when first opened by a user with write
 * access ([ChatService.open]).
 *
 * No [org.projectforge.framework.persistence.entities.DefaultBaseDO]: no history, no BaseDao, the access comes from
 * the host and is checked by [ChatService].
 */
@Entity
@Table(
    name = "T_CHAT_CONVERSATION",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_chat_conversation_host",
        columnNames = ["host_type", "host_id", "channel"],
    )],
)
open class ChatConversationDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(nullable = false)
    open var created: Date? = null

    /** [ChatHost.hostType], e.g. `chatRoom`. */
    @get:Column(name = "host_type", length = 50, nullable = false)
    open var hostType: String? = null

    @get:Column(name = "host_id", nullable = false)
    open var hostId: Long? = null

    @get:Column(name = "channel", length = 50, nullable = false)
    open var channel: String? = null

    /**
     * Counts the changes of the messages: every change increments it and stores the new value in the message
     * ([ChatMessageDO.changeSeq]). Incremented first in every change, so changes of a conversation are serialized
     * (row lock until commit).
     */
    @get:Column(name = "change_seq", nullable = false)
    open var changeSeq: Long = 0

    @get:Column(name = "last_message_at")
    open var lastMessageAt: Date? = null

    /**
     * The synthetic data transfer area of the files of the messages (see [ChatFileService]), null until the first
     * one, and always for direct messages (their files are in the personal box of the recipient).
     */
    @get:Column(name = "file_area_id")
    open var fileAreaId: Long? = null
}
