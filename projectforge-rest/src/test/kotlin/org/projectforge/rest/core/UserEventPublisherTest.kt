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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The streams of the user events ([UserEventPublisher]): one per open page, capped per user, the user's own only. */
class UserEventPublisherTest {
    @Test
    fun `a user keeps at most the allowed number of streams, the oldest ones are dropped, others are not affected`() {
        val publisher = UserEventPublisher()
        repeat(UserEventPublisher.MAX_PER_USER + 5) { publisher.subscribe(1L) }
        assertEquals(UserEventPublisher.MAX_PER_USER, publisher.size)
        publisher.subscribe(2L)
        publisher.subscribe(2L)
        assertEquals(UserEventPublisher.MAX_PER_USER + 2, publisher.size)
        repeat(UserEventPublisher.MAX_PER_USER + 5) { publisher.subscribe(1L) }
        assertEquals(UserEventPublisher.MAX_PER_USER + 2, publisher.size, "the other user keeps both")
    }

    @Test
    fun `publishing to a user without a stream does nothing`() {
        val publisher = UserEventPublisher()
        publisher.publish(42L, UserEventPublisher.SECRET_OFFERS)
        assertEquals(0, publisher.size)
    }
}
