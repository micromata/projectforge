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

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * Represents a currency pair for currency conversion (e.g. USD -> EUR).
 * The conversion rates are stored in CurrencyConversionRateDO with validity periods.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "t_fibu_currency_pair",
    uniqueConstraints = [UniqueConstraint(columnNames = ["source_currency", "target_currency"])],
    indexes = [
        Index(name = "idx_t_fibu_currency_pair_source", columnList = "source_currency"),
        Index(name = "idx_t_fibu_currency_pair_target", columnList = "target_currency")
    ]
)
@AUserRightId("FIBU_CURRENCY_CONVERSION")
open class CurrencyPairDO : DefaultBaseDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = "$sourceCurrency → $targetCurrency"

    @PropertyInfo(i18nKey = "fibu.currencyPair.sourceCurrency", tooltip = "fibu.currencyPair.currencyCode.tooltip")
    @FullTextField
    @get:Column(name = "source_currency", length = 3, nullable = false)
    open var sourceCurrency: String? = null

    @PropertyInfo(i18nKey = "fibu.currencyPair.targetCurrency", tooltip = "fibu.currencyPair.currencyCode.tooltip")
    @FullTextField
    @get:Column(name = "target_currency", length = 3, nullable = false)
    open var targetCurrency: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.COMMENT_LENGTH)
    open var comment: String? = null

    override fun equals(other: Any?): Boolean {
        if (other !is CurrencyPairDO)
            return false
        if (other.id == null) {
            return false
        }
        return if (this.id == other.id) {
            true
        } else super.equals(other)
    }

    override fun hashCode(): Int {
        return if (id != null) 31 * id.hashCode() else super.hashCode()
    }
}
