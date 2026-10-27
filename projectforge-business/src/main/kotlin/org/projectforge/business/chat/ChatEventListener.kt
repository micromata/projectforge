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

/**
 * Informed about the changes of conversations (after they are written), e.g. to push them to the open chat pages
 * (server-sent events). Implemented outside of the business module.
 */
interface ChatEventListener {
    /** The messages of the conversation changed, the newest change has the given sequence number. */
    fun onChange(conversationId: Long, changeSeq: Long)

    /** Has the user the conversation open right now (no notification about new messages needed)? */
    fun isWatching(conversationId: Long, userId: Long): Boolean = false

    /** A confidential session in the conversation changed for the user ([ChatSecretRelay]). */
    fun onSecret(conversationId: Long, userId: Long) {}

    /**
     * The confidential conversations offered to the user changed (new, accepted, ended or expired), in any of the
     * user's direct chats ([ChatSecretRelay.offers]): wakes every open page of the user, not only the open chat.
     */
    fun onSecretOffers(userId: Long) {}
}
