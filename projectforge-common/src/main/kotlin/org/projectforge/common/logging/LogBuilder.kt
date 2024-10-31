/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common.logging

import mu.KLogger

class LogBuilder(val log: KLogger) {
    val sb = StringBuilder()
    fun mtd(method: String): LogBuilder {
        sb.append("fun $method:")
        return this
    }
    fun msg(msg: String): LogBuilder {
        sb.append(" $msg")
        return this
    }
    fun params(vararg params: Pair<String, Any?>): LogBuilder {
        if (params.isNotEmpty()) {
            sb.append(", params={")
            params.joinTo(sb) { "${it.first}=${it.second}" }
            sb.append("}")
        }
        return this
    }
    fun log() {
        log.debug { sb.toString() }
    }
}
