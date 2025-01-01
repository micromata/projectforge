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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed

/**
 * Beside entries of debitors and creditors invoices additional entries (for accommodation, taxes, planned salaries,
 * assurance etc.) are important for a complete liquidity planning.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
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

    @PropertyInfo(i18nKey = "fibu.rechnung.status.bezahlt")
    @get:Column
    open var paid: Boolean = false

    @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var subject: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null
}
