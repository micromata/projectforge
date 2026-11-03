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
 * A user who blocked another one ([ChatBlockService]): no direct messages between them, and the blocker gets no
 * notifications, mentions, unread counts or search hits of the blocked one.
 */
@Entity
@Table(
    name = "T_CHAT_USER_BLOCK",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_chat_user_block",
        columnNames = ["blocker_fk", "blocked_fk"],
    )],
)
open class ChatUserBlockDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "created", nullable = false)
    open var created: Date? = null

    @get:Column(name = "blocker_fk", nullable = false)
    open var blockerId: Long? = null

    @get:Column(name = "blocked_fk", nullable = false)
    open var blockedId: Long? = null
}
