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

package org.projectforge.plugins.liquidityplanning

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.time.RecurrenceFrequency
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A recurring series of liquidity entries — a rule (start date, frequency, interval, optional installment
 * count) plus the template values every occurrence starts from. Stored once; the occurrences themselves are
 * projected virtually on read ([LiquiditySeriesProjector]) and only materialized into a real
 * [LiquidityEntryDO] when the user touches one. There is deliberately no `paid` here: a virtual occurrence is
 * always "automatic" (`paid = null`), and the manual paid state only exists once an occurrence is
 * materialized.
 *
 * @author Kai Reinhard
 */
@Entity
@Table(name = "T_PLUGIN_LIQUI_SERIES")
open class LiquiditySeriesDO : DefaultBaseDO() {

    /** The anchor of the recurrence: the first occurrence's date and the day-of-month every occurrence keeps. */
    @PropertyInfo(i18nKey = "plugins.liquidityplanning.series.startDate")
    @get:Column(name = "start_date")
    open var startDate: LocalDate? = null

    /**
     * The recurrence unit. Only [RecurrenceFrequency.MONTHLY] is offered for now; the field is kept so a later
     * weekly/yearly extension needs no migration.
     */
    @PropertyInfo(i18nKey = "common.recurrence.frequency.label")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "frequency", length = 20)
    open var frequency: RecurrenceFrequency? = RecurrenceFrequency.MONTHLY

    /** Every how many [frequency] units an occurrence falls (1 = every month). */
    @PropertyInfo(i18nKey = "plugins.liquidityplanning.series.interval.months")
    @get:Column(name = "interval_months")
    open var intervalMonths: Int = 1

    /** The number of installments, or `null` for an endless series. */
    @PropertyInfo(i18nKey = "plugins.liquidityplanning.series.count")
    @get:Column(name = "installments")
    open var count: Int? = null

    @PropertyInfo(i18nKey = "fibu.common.betrag", type = PropertyType.CURRENCY)
    @get:Column(scale = 2, precision = 12)
    open var amount: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
    @get:Column(length = Constants.LENGTH_TITLE)
    open var subject: String? = null

    @PropertyInfo(i18nKey = "comment")
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.autoSetPaid")
    @get:Column(name = "auto_set_paid")
    open var autoSetPaid: Boolean = false
}
