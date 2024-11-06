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

package org.projectforge.framework.persistence.jpa

import jakarta.persistence.TypedQuery
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils

private val log = KotlinLogging.logger {}

/**
 * Only for internal usage. Please use [PfPersistenceService] and [PfPersistenceContext] instead.
 */
internal object EntityManagerUtil {
    fun queryToString(query: TypedQuery<*>, errorMessage: String?): String {
        val queryString = query.unwrap(org.hibernate.query.Query::class.java)?.getQueryString()
        val sb = StringBuilder()
        sb.append("query='$queryString', params=[") //query.getQueryString())
        var first = true
        try {
            for (param in query.parameters) { // getParameterMetadata().getNamedParameterNames()
                if (!first)
                    sb.append(",")
                else
                    first = false
                sb.append("${param.name}=[${query.getParameterValue(param)}]")
            }
        } catch (ex: Exception) {
            // Do nothing: Session/EntityManager closed.
        }
        sb.append("]")
        if (StringUtils.isNotBlank(errorMessage))
            sb.append(", msg=[$errorMessage]")
        return sb.toString()
    }
}
