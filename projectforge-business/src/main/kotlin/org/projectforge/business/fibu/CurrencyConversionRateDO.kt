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

import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.history.WithHistory
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Represents time-dependent currency conversion rates for a currency pair.
 * Each rate is valid from a specific date until the next rate becomes valid.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(
    name = "t_fibu_currency_conversion_rate",
    uniqueConstraints = [UniqueConstraint(columnNames = ["currency_pair_fk", "valid_from"])],
    indexes = [
        Index(name = "idx_fk_t_fibu_curr_conv_rate_pair", columnList = "currency_pair_fk"),
        Index(name = "idx_t_fibu_curr_conv_rate_valid", columnList = "valid_from")
    ]
)
@NamedQueries(
    NamedQuery(
        name = CurrencyConversionRateDO.FIND_BY_PAIR_AND_DATE,
        query = "from CurrencyConversionRateDO where currencyPair.id=:currencyPairId and validFrom=:validFrom",
    ),
    NamedQuery(
        name = CurrencyConversionRateDO.FIND_OTHER_BY_PAIR_AND_DATE,
        query = "from CurrencyConversionRateDO where currencyPair.id=:currencyPairId and validFrom=:validFrom and id!=:id",
    ),
    NamedQuery(
        name = CurrencyConversionRateDO.FIND_ALL_BY_PAIR,
        query = "from CurrencyConversionRateDO where currencyPair.id=:currencyPairId order by validFrom desc",
    ),
)
@WithHistory
open class CurrencyConversionRateDO : Serializable, AbstractBaseDO<Long>() {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    override var id: Long? = null

    @PropertyInfo(i18nKey = "fibu.currencyPair")
    @JsonIdentityReference(alwaysAsId = true)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "currency_pair_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var currencyPair: CurrencyPairDO? = null

    @PropertyInfo(i18nKey = "attr.validSince")
    @get:Column(name = "valid_from", nullable = false)
    open var validFrom: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.currencyConversion.conversionRate")
    @get:Column(name = "conversion_rate", scale = 8, precision = 18, nullable = false)
    open var conversionRate: BigDecimal? = null

    @PropertyInfo(i18nKey = "comment")
    @get:Column(name = "comment", length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    /**
     * Copies validFrom, conversionRate and comment from other.
     * Please note: id and currencyPair are not copied.
     * @param other the other CurrencyConversionRateDO to copy from.
     */
    fun copyFrom(other: CurrencyConversionRateDO) {
        this.validFrom = other.validFrom
        this.conversionRate = other.conversionRate
        this.comment = other.comment
    }

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }

    companion object {
        internal const val FIND_BY_PAIR_AND_DATE = "CurrencyConversionRateDO_FindByPairAndDate"
        internal const val FIND_OTHER_BY_PAIR_AND_DATE = "CurrencyConversionRateDO_FindOtherByPairAndDate"
        internal const val FIND_ALL_BY_PAIR = "CurrencyConversionRateDO_FindAllByPair"
    }
}
