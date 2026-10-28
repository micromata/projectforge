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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The registered chat contexts ([ChatHost]) by their host type.
 */
@Service
class ChatHostRegistry {
    @Autowired
    private lateinit var hostList: List<ChatHost>

    private val hosts: Map<String, ChatHost> by lazy {
        hostList.associateBy { it.hostType }.also {
            require(it.size == hostList.size) { "Chat host types must be unique: ${hostList.map { host -> host.hostType }}" }
        }
    }

    fun get(hostType: String?): ChatHost? = hostType?.let { hosts[it] }
}
