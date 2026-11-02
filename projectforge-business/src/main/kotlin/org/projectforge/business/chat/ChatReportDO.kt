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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * A message reported by a user ([ChatReportService]). Keeps an excerpt of the message, because the administrators have
 * no access to conversations and the author may change or delete the message afterwards.
 */
@Entity
@Table(
    name = "T_CHAT_REPORT",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_chat_report",
        columnNames = ["reporter_fk", "message_fk"],
    )],
)
open class ChatReportDO {
    enum class Status { OPEN, HANDLED }

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "created", nullable = false)
    open var created: Date? = null

    @get:Column(name = "message_fk", nullable = false)
    open var messageId: Long? = null

    @get:Column(name = "conversation_fk", nullable = false)
    open var conversationId: Long? = null

    @get:Column(name = "reporter_fk", nullable = false)
    open var reporterId: Long? = null

    /** The author of the message. */
    @get:Column(name = "author_fk", nullable = false)
    open var authorId: Long? = null

    @get:Column(name = "reason", length = REASON_LENGTH)
    open var reason: String? = null

    /** The start of the text of the message at the time of the report. */
    @get:Column(name = "content_snapshot", length = SNAPSHOT_LENGTH)
    open var contentSnapshot: String? = null

    @get:Column(name = "status", nullable = false, length = 20)
    @get:Enumerated(EnumType.STRING)
    open var status: Status = Status.OPEN

    companion object {
        const val REASON_LENGTH = 1000
        const val SNAPSHOT_LENGTH = 500
    }
}
