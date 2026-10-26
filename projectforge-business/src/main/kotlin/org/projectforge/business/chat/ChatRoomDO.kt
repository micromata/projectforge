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
import jakarta.persistence.Table
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * A chat room (PF-CHAT-ROOM): the host object of a conversation for discussions belonging to no other object, see
 * [ChatRoomChatHost]. Its participants are the owner, the moderators, the users and the members of the groups, stored
 * as csv lists of ids. Groups are resolved on every check ([ChatRoomDao]), so a later member of a group takes part,
 * too.
 */
@Entity
@Table(name = "T_CHAT_ROOM")
open class ChatRoomDO : DefaultBaseDO() {
    @get:PropertyInfo(i18nKey = "chat.room.name", required = true)
    @get:Column(name = "name", length = 255, nullable = false)
    open var name: String? = null

    @get:PropertyInfo(i18nKey = "chat.room.description")
    @get:Column(name = "description", length = 4000)
    open var description: String? = null

    /** The creator, taken from the logged-in user on insert, never changed by an update. */
    @get:PropertyInfo(i18nKey = "chat.room.owner")
    @get:Column(name = "owner_fk", nullable = false)
    open var ownerId: Long? = null

    @get:PropertyInfo(i18nKey = "chat.room.moderators")
    @get:Column(name = "moderator_user_ids", length = 4000)
    open var moderatorUserIds: String? = null

    @get:PropertyInfo(i18nKey = "chat.room.participantUsers")
    @get:Column(name = "participant_user_ids", length = 10000)
    open var participantUserIds: String? = null

    @get:PropertyInfo(i18nKey = "chat.room.participantGroups")
    @get:Column(name = "participant_group_ids", length = 4000)
    open var participantGroupIds: String? = null

    companion object {
        /** The ids of a csv list of ids. */
        fun toIds(csv: String?): Set<Long> {
            if (csv.isNullOrBlank()) return emptySet()
            return StringHelper.splitToLongs(csv, ",", false).toSet()
        }

        /** The csv list of the given ids (sorted), null for none. */
        fun toCsv(ids: Collection<Long>?): String? {
            return ids?.toSortedSet()?.takeIf { it.isNotEmpty() }?.joinToString(",")
        }
    }
}
