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

package org.projectforge.gateway.sync

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for ICS calendar data pushed from the main instance.
 * Key: "userId:queryParam", Value: ICS data as String.
 */
@Component
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewayIcsCache {

    private val cache = ConcurrentHashMap<String, String>()

    fun put(userId: Long, queryParam: String, icsData: String) {
        cache["$userId:$queryParam"] = icsData
    }

    fun get(userId: Long, queryParam: String): String? {
        return cache["$userId:$queryParam"]
    }

    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size
}
