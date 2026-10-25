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

/**
 * Per user and conversation: the last message read ("new" line, unread count) and whether the conversation is muted.
 */
@Entity
@Table(
    name = "T_CHAT_MEMBER_STATE",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_chat_member_state",
        columnNames = ["conversation_fk", "user_fk"],
    )],
)
open class ChatMemberStateDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "conversation_fk", nullable = false)
    open var conversationId: Long? = null

    @get:Column(name = "user_fk", nullable = false)
    open var userId: Long? = null

    /** The id of the newest message read; messages with a higher id are unread. */
    @get:Column(name = "last_read_message_id")
    open var lastReadMessageId: Long? = null

    /** No notifications about new messages (mentions are still notified). */
    @get:Column(name = "muted", nullable = false)
    open var muted: Boolean = false
}
