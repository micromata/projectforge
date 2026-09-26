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

import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed

/**
 * Beside entries of debitors and creditors invoices additional entries (for accommodation, taxes, planned salaries,
 * assurance etc.) are important for a complete liquidity planning.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_LIQUI_ENTRY")
open class LiquidityEntryDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.dateOfPayment")
    @get:Column(name = "date_of_payment")
    open var dateOfPayment: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.common.betrag", type = PropertyType.CURRENCY)
    @get:Column(scale = 2, precision = 12)
    open var amount: BigDecimal? = null

    /**
     * The manual paid override: `true` forces paid, `false` forces unpaid, `null` means "automatic" — then
     * the paid status follows [autoSetPaid] and [dateOfPayment] (see [effectivePaid]). The column used to be
     * NOT NULL; it was made nullable so the third "automatic" state can be expressed.
     */
    @PropertyInfo(i18nKey = "fibu.rechnung.status.bezahlt")
    @get:Column
    open var paid: Boolean? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var subject: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    /**
     * If set, the entry counts as paid automatically once its [dateOfPayment] has passed (see
     * [effectivePaid]). No scheduled job persists this: the paid status is derived at display and usage
     * time. The manual [paid] override still wins: setting [paid] to `true`/`false` overrides the automatic
     * rule; only [paid] == `null` ("automatic") lets this flag decide.
     */
    @PropertyInfo(i18nKey = "plugins.liquidityplanning.entry.autoSetPaid")
    @get:Column(name = "auto_set_paid")
    open var autoSetPaid: Boolean = false

    /**
     * The series this entry was materialized from, or `null` for a plain (non-recurring) entry. A scalar id,
     * not a `@ManyToOne`: the entry is serialized 1:1 to the frontend without a DTO, so a lazy proxy would
     * only get in the way of the JSON. Occurrences of a series stay virtual (projected on read, see
     * [org.projectforge.plugins.liquidityplanning.LiquiditySeriesProjector]) until the user touches one, which
     * materializes them into a real row carrying this id.
     */
    @get:Column(name = "series_id")
    open var seriesId: Long? = null

    /**
     * The stable anchor day of this occurrence within its series — the identity of the n-th occurrence, set
     * from the projection anchor when materialized and never derived from [dateOfPayment]. This lets the user
     * freely move the actual [dateOfPayment] while the occurrence stays the n-th one: suppression of the
     * virtual occurrence matches on `(seriesId, seriesDate)`, not on the payment date. `null` for a plain
     * entry.
     */
    @get:Column(name = "series_date")
    open var seriesDate: LocalDate? = null

    /**
     * Not persisted: the "repeat" block the new-entry form posts to turn this entry into a recurring series.
     * When present with [LiquidityRepeatConfig.enabled] the REST layer creates a [LiquiditySeriesDO] from this
     * entry's template on save and links this entry as the series' first (materialized) occurrence (see
     * `LiquidityEntityRest.onAfterSaveOrUpdate`). Only meaningful for a new, non-series entry; ignored once the
     * entry belongs to a series.
     */
    @get:Transient
    open var repeat: LiquidityRepeatConfig? = null

    /**
     * The paid status actually used everywhere the entry is shown or forecast. The manual [paid] override
     * takes precedence: if it is set (`true`/`false`) that value is used. Only when [paid] is `null`
     * ("automatic") does [autoSetPaid] decide: the entry then counts as paid once its [dateOfPayment] lies
     * strictly before today. An entry without a date of payment is never auto-paid.
     *
     * Transient (computed, never persisted); serialized to the next frontend so the list can highlight and
     * the statistics can count by the effective status.
     */
    val effectivePaid: Boolean
        @Transient
        get() = paid ?: (autoSetPaid && dateOfPayment?.isBefore(LocalDate.now()) == true)
}
