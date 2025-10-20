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

package org.projectforge.plugins.marketing

import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField

/**
 * A marketing campaign for addresses (eg. mailings).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN")
open class AddressCampaignDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "title")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var title: String? = null

    @PropertyInfo(i18nKey = "values")
    @FullTextField
    @get:Column(length = 1000, name = "s_values")
    open var values: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.LENGTH_COMMENT)
    open var comment: String? = null

    val valuesArray: Array<String>?
        @Transient
        get() = getValuesArray(values)

    companion object {
        const val MAX_VALUE_LENGTH = 100

        fun getValuesArray(values: String?): Array<String>? {
            if (StringUtils.isBlank(values)) {
                return null
            }
            // Split by semicolon only, then trim whitespace and filter out blank entries
            val array = values!!.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toTypedArray()
            return if (array.isEmpty()) null else array
        }
    }
}
