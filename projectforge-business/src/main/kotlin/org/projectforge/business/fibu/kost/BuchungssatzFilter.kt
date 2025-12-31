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

package org.projectforge.business.fibu.kost

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.time.PFDayUtils
import java.io.Serializable

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class BuchungssatzFilter
@JvmOverloads
constructor(filter: BaseSearchFilter? = null) : BaseSearchFilter(filter), Serializable {
    var fromYear: Int? = null
        set(value) {
            // Backwards compatibility for year <= 0
            field = if (value != null && value.toInt() > 0) value else null
        }

    var toYear: Int? = null
        set(value) {
            // Backwards compatibility for year <= 0
            field = if (value != null && value.toInt() > 0) value else null
        }

    /**
     * month (1-12)
     */
    var fromMonth: Int? = null
        set(value) {
            field = PFDayUtils.validateMonthValue(value)
        }

    /**
     * month (1-12)
     */
    var toMonth: Int? = null
        set(value) {
            field = PFDayUtils.validateMonthValue(value)
        }

    override fun reset(): BuchungssatzFilter {
        super.reset()
        toMonth = fromMonth
        toYear = fromYear
        return this
    }

    /**
     * month (1-12)
     */
    fun setFrom(year: Int, month: Int?) {
        fromYear = year
        fromMonth = month
    }

    /**
     * month (1-12)
     */
    fun setTo(year: Int, month: Int?) {
        toYear = year
        toMonth = month
    }
}
