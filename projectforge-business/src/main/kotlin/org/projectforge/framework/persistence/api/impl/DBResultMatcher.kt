/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api.impl

import org.projectforge.common.BeanHelper
import java.util.*

/**
 * After querying, every result entry is matched against matchers (for fields not supported by the full text query).
 */
internal interface DBResultMatcher {
    fun match(obj: Any): Boolean

    class Equals(
            val field: String,
            val expectedValue: Any)
        : DBResultMatcher {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field)
            return Objects.equals(expectedValue, value)
        }
    }

    class IsNull(
            val field: String)
        : DBResultMatcher {
        override fun match(obj: Any): Boolean {
            return BeanHelper.getProperty(obj, field) == null
        }
    }

    class Or(
            vararg matcher: DBResultMatcher
    ) : DBResultMatcher {
        val matcherList = matcher

        override fun match(obj: Any): Boolean {
            if (matcherList.isNullOrEmpty()) {
                return false
            }
            for (matcher in matcherList) {
                if (matcher.match(obj)) {
                    return true
                }
            }
            return false
        }
    }
}
