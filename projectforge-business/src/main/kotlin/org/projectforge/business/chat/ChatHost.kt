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

import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * The chat context of a host module (PF-CHAT CTX-01): a module embedding the chat for its objects (e.g. chat rooms,
 * vulnerabilities) registers it as Spring bean. The chat has no access rules of its own: who reads, writes and
 * moderates a conversation is decided by its host only, for the host object taken from the database.
 */
interface ChatHost {
    /** The unique type of the host, stored with the conversations, e.g. `chatRoom`. */
    val hostType: String

    /** The channels of the host object (e.g. `internal`, `public`), empty for an unknown host object. */
    fun channels(hostId: Long): List<String>

    /**
     * The title of the host object shown with the conversation, null if the host object doesn't exist (any more) or
     * the channel is unknown: the conversation is refused.
     */
    fun title(hostId: Long, channel: String): String?

    /**
     * The title as seen by the user, e.g. the name of the other one of a direct message. By default [title].
     */
    fun title(hostId: Long, channel: String, user: PFUserDO): String? = title(hostId, channel)

    /** The description of the host object as rich text (HTML), shown above the conversation; null for none. */
    fun description(hostId: Long, channel: String): String? = null

    /**
     * A notice shown above the conversation as plain text in the language of the user, e.g. that the external users
     * of a data transfer area don't see its chat; null for none.
     */
    fun notice(hostId: Long, channel: String, user: PFUserDO): String? = null

    /** The link to the host object (path of the app), shown with the conversation. */
    fun link(hostId: Long): String?

    fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String): Boolean

    /** Write access implies read access. False for a read-only conversation, e.g. of a closed host object. */
    fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String): Boolean

    /** Moderators hide messages of others and see the history of messages (SEC-05). */
    fun isModerator(user: PFUserDO, hostId: Long, channel: String): Boolean

    /**
     * The users taking part in the conversation: the candidates of mentions and the recipients of notifications about
     * new messages. Each one is checked by [hasReadAccess] again.
     */
    fun participantIds(hostId: Long, channel: String): Set<Long>

    /** Further recipients of notifications about new messages (NOT-04), e.g. a security team. */
    fun extraRecipientIds(hostId: Long, channel: String): Set<Long> = emptySet()

    /** May notification mails contain the text of messages (NOT-05)? */
    fun mailContentAllowed(hostId: Long, channel: String): Boolean = false

    /**
     * Do the files of the conversation go into an existing data transfer area given by the host ([fileAreaId], e.g.
     * the personal box of the recipient of a direct message, or the data transfer area being discussed) instead of
     * a synthetic area of the conversation (ATT-02)?
     */
    val usesOwnFileArea: Boolean
        get() = false

    /**
     * The existing data transfer area the files of the user go to, if [usesOwnFileArea]; null if no files can be
     * exchanged there.
     */
    fun fileAreaId(hostId: Long, channel: String, user: PFUserDO): Long? = null

    /**
     * Can files be exchanged in the conversation at all (e.g. not for a data transfer area on the gateway)? Free of side
     * effects, unlike [fileAreaId] (which may create a personal box): asked on every read of the conversation.
     */
    fun filesAllowed(hostId: Long, channel: String): Boolean = true
}
