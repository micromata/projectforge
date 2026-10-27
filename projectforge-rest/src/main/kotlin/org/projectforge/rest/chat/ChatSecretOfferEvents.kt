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

package org.projectforge.rest.chat

import org.projectforge.business.chat.ChatEventListener
import org.projectforge.rest.core.UserEventPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Wakes all open pages of the recipient of an offer of a confidential conversation, not only its open chat
 * ([ChatEventPublisher]): the page draws the attention of the user to it (PF-CHAT CONF-10).
 */
@Service
class ChatSecretOfferEvents : ChatEventListener {
    @Autowired
    private lateinit var publisher: UserEventPublisher

    override fun onChange(conversationId: Long, changeSeq: Long) {}

    override fun onSecretOffers(userId: Long) {
        publisher.publish(userId, UserEventPublisher.SECRET_OFFERS)
    }
}
