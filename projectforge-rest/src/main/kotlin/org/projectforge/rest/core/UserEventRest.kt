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

import jakarta.servlet.http.HttpServletResponse
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/** The server-sent events of the logged-in user, for all open pages ([UserEventPublisher]). */
@RestController
@RequestMapping("${Rest.URL}/userEvents")
class UserEventRest {
    @Autowired
    private lateinit var publisher: UserEventPublisher

    /** Read only, so GET. The events carry no content: the page asks with the access checks of its requests. */
    @AccessChecked("the events of the logged-in user only, without content")
    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(response: HttpServletResponse): SseEmitter {
        val userId = ThreadLocalUserContext.requiredLoggedInUserId
        // No buffering by a reverse proxy, as the chat events (ChatRest.events).
        response.setHeader("X-Accel-Buffering", "no")
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
        return publisher.subscribe(userId)
    }
}
