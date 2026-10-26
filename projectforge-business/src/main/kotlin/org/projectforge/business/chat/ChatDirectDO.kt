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
 * The host object of direct messages ([ChatDirectChatHost]): a pair of users, the one with the lower id first, so a
 * pair has exactly one conversation, whoever starts it.
 */
@Entity
@Table(
    name = "T_CHAT_DIRECT",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_chat_direct_users",
        columnNames = ["user1_fk", "user2_fk"],
    )],
)
open class ChatDirectDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(nullable = false)
    open var created: Date? = null

    /** The user with the lower id. */
    @get:Column(name = "user1_fk", nullable = false)
    open var user1Id: Long? = null

    /** The user with the higher id. */
    @get:Column(name = "user2_fk", nullable = false)
    open var user2Id: Long? = null

    fun isMember(userId: Long?): Boolean = userId != null && (userId == user1Id || userId == user2Id)

    /** The other one of the pair, null if the user isn't one of them. */
    fun partnerOf(userId: Long?): Long? {
        return when (userId) {
            null -> null
            user1Id -> user2Id
            user2Id -> user1Id
            else -> null
        }
    }
}
