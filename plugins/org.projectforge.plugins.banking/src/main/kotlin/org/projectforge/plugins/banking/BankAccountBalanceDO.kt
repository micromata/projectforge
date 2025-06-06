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

package org.projectforge.plugins.banking

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "T_PLUGIN_BANKING_ACCOUNT_BALANCE",
)
@NamedQueries(
    NamedQuery(
        name = BankAccountBalanceDO.FIND_BY_BANK_ACCOUNT,
        query = "from BankAccountBalanceDO where bankAccount.id=:bankAccountId"
    ),
)
open class BankAccountBalanceDO : DefaultBaseDO() {
    @PropertyInfo(i18nKey = "plugins.banking.account")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "banking_account_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var bankAccount: BankAccountDO? = null

    @PropertyInfo(i18nKey = "plugins.banking.account.record.amount", type = PropertyType.CURRENCY)
    @get:Column(name = "amount", scale = 2, precision = 12)
    open var amount: BigDecimal? = null

    @PropertyInfo(i18nKey = "plugins.banking.account.record.date")
    @GenericField
    @get:Column(name = "date_col")
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    companion object {
        const val FIND_BY_BANK_ACCOUNT = "BankAccountBalanceDO_FindByBankAccount"

    }
}
