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

package org.projectforge.plugins.banking

import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * The layout-free REST endpoint of the transactions of the bank accounts, serving the hand-built projectforge-next
 * page (see components/features/bank-account-record). Replaces the retired `BankAccountRecordPagesRest`, whose
 * server-side `UILayout` (React) moved onto the frontend; the path `/rs/bankAccountRecord` is unchanged.
 *
 * The list is filtered by period and accounts (a bank account's form jumps here with its account preselected), and
 * may show only doublets or records whose checksum doesn't match (a manipulation after the import). The income,
 * outgo and cash flow of the result set travel as [ResultSet.statistics].
 */
@RestController
@RequestMapping("${Rest.URL}/bankAccountRecord")
class BankAccountRecordEntityRest :
    AbstractDTOEntityRest<BankAccountRecordDO, BankAccountRecord, BankAccountRecordDao>(
        BankAccountRecordDao::class.java,
        "plugins.banking.account.record.title",
        cloneSupport = CloneSupport.CLONE
    ) {
    @Autowired
    private lateinit var bankAccountDao: BankAccountDao

    override fun transformFromDB(obj: BankAccountRecordDO, editMode: Boolean): BankAccountRecord {
        val bankAccountRecord = BankAccountRecord()
        bankAccountRecord.copyFrom(obj)
        bankAccountRecord.checksumError = isChecksumError(obj)
        return bankAccountRecord
    }

    /**
     * The account and the checksum of a stored record are the stored ones, never the client's: an update doesn't
     * move a record to another account. A new record gets the account of the given id, read with the access of the
     * logged-in user (and checked again by [BankAccountRecordDao.hasAccess] on insert).
     */
    override fun transformForDB(dto: BankAccountRecord): BankAccountRecordDO {
        val bankAccountRecordDO = BankAccountRecordDO()
        dto.copyTo(bankAccountRecordDO)
        val dbObj = dto.id?.let { baseDao.find(it) }
        if (dbObj != null) {
            bankAccountRecordDO.checksum = dbObj.checksum
            bankAccountRecordDO.bankAccount = dbObj.bankAccount
        } else {
            bankAccountRecordDO.checksum = null
            bankAccountRecordDO.bankAccount = dto.bankAccount?.id?.let { bankAccountDao.find(it) }
        }
        return bankAccountRecordDO
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterElement(
                "period",
                label = translate("timePeriod"),
                filterType = UIFilterElement.FilterType.DATE,
                defaultFilter = true,
            )
        )
        val accountsFilter = UIFilterListElement(
            "accounts",
            label = translate("plugins.banking.accounts"),
            defaultFilter = true,
            multi = true,
        )
        accountsFilter.values = bankAccountDao.select(BaseSearchFilter()).map { account ->
            UISelectValue("${account.id}", StringUtils.abbreviate(account.name, 20))
        }
        elements.add(accountsFilter)
        elements.add(
            UIFilterElement(
                "doublets", UIFilterElement.FilterType.BOOLEAN, translate("plugins.banking.account.record.doublets"),
                defaultFilter = true,
            )
        )
        elements.add(
            UIFilterElement(
                "checksumErrors",
                UIFilterElement.FilterType.BOOLEAN,
                translate("plugins.banking.account.record.checksumErrors"),
                defaultFilter = true,
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<BankAccountRecordDO>>? {
        source.entries.find { it.field == "doublets" }?.synthetic = true
        source.entries.find { it.field == "checksumErrors" }?.synthetic = true
        source.entries.find { it.field == "period" }?.let { periodFilterEntry ->
            periodFilterEntry.synthetic = true
            val fromDate = PFDayUtils.parseDate(periodFilterEntry.value.fromValue)
            val toDate = PFDayUtils.parseDate(periodFilterEntry.value.toValue)
            if (fromDate != null) {
                if (toDate != null) {
                    target.add(QueryFilter.between("date", fromDate, toDate))
                } else {
                    target.add(QueryFilter.ge("date", fromDate))
                }
            } else if (toDate != null) {
                target.add(QueryFilter.le("date", toDate))
            }
        }
        source.entries.find { it.field == "accounts" }?.let { entry ->
            entry.synthetic = true
            val ids = entry.value.values?.mapNotNull { it.toLongOrNull() }
            if (!ids.isNullOrEmpty()) {
                // All selected accounts (the React list took only the first one of its multi select).
                target.add(QueryFilter.isIn("bankAccount.id", ids))
            }
        }
        return null
    }

    /**
     * Applies the filters of doublets and checksum errors (both need the whole result set) and adds the income,
     * outgo and cash flow of the remaining records as [ResultSet.statistics].
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<BankAccountRecordDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter
    ): ResultSet<*> {
        if (magicFilter.entries.find { it.field == "doublets" }?.isTrueValue == true) {
            val origList = resultSet.resultSet
            resultSet.resultSet = origList.filter { isDoublet(origList, it) }
        }
        if (magicFilter.entries.find { it.field == "checksumErrors" }?.isTrueValue == true) {
            resultSet.resultSet = resultSet.resultSet.filter { isChecksumError(it) }
        }
        val stats = BankAccountRecordStatistics()
        resultSet.resultSet.forEach { record ->
            stats.add(record.amount)
        }
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        result.statistics = Statistics(stats)
        return result
    }

    private fun isDoublet(list: List<BankAccountRecordDO>, element: BankAccountRecordDO): Boolean {
        if (element.deleted) {
            return false
        }
        return list.any { it.id != element.id && it.buildCheckSum() == element.buildCheckSum() }
    }

    private fun isChecksumError(element: BankAccountRecordDO): Boolean {
        return element.checksum != element.buildCheckSum()
    }

    /** The sums the list shows above its table. */
    class Statistics(stats: BankAccountRecordStatistics) {
        val income: BigDecimal = stats.income
        val outgo: BigDecimal = stats.outgo
        val cashflow: BigDecimal = stats.cashflow
    }
}
