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

package org.projectforge.plugins.marketing

import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Transient

/**
 * A marketing campaign for addresses (eg. mailings).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_marketing_address_campaign_tenant_id", columnList = "tenant_id")])
open class AddressCampaignDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "title")
    @Field
    @get:Column(length = Constants.LENGTH_TITLE)
    open var title: String? = null

    @PropertyInfo(i18nKey = "values")
    @Field
    @get:Column(length = 1000, name = "s_values")
    open var values: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = Constants.LENGTH_COMMENT)
    open var comment: String? = null

    val valuesArray: Array<String>?
        @Transient
        get() = getValuesArray(values)

    companion object {
        const val MAX_VALUE_LENGTH = 100

        fun getValuesArray(values: String?): Array<String>? {
            return if (StringUtils.isBlank(values)) {
                null
            } else StringUtils.split(values, "; ")
        }
    }
}
