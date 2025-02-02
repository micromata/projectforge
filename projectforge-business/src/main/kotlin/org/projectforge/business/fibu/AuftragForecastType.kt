/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import org.projectforge.common.i18n.I18nEnum

/**
 * When sales of an order are distributed, this can be used to determine, for example, whether sales are
 * invoiced/forecast in the current month or in the following month.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
enum class AuftragForecastType(val key: String) : I18nEnum {
    CURRENT_MONTH("currentMonth"),

    /**
     * Sales are invoiced/forecast in the following month. This is the default.
     */
    FOLLOWING_MONTH("followingMonth");

    /**
     * @return The full i18n key including the i18n prefix "book.type.".
     */
    override val i18nKey: String
        get() = "$baseKey.$key"

    companion object {
        val default = FOLLOWING_MONTH
        @JvmStatic
        val baseKey = "fibu.auftrag.forecastType"
    }
}
