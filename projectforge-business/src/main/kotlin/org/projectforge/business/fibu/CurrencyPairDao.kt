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

import mu.KotlinLogging
import org.apache.commons.lang3.ArrayUtils
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * DAO for currency pairs and their conversion rates.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class CurrencyPairDao : BaseDao<CurrencyPairDO>(CurrencyPairDO::class.java) {

    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun select(filter: BaseSearchFilter): List<CurrencyPairDO> {
        val queryFilter = QueryFilter(filter)
        return select(queryFilter)
    }

    override fun afterInsertOrModify(obj: CurrencyPairDO, operationType: OperationType) {
        // Cache invalidation would go here if we implement a cache
        log.info { "Currency pair ${obj.displayName} was ${operationType.name.lowercase()}" }
    }

    override fun newInstance(): CurrencyPairDO {
        return CurrencyPairDO()
    }

    override fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property)
    }

    /**
     * Gets history entries of super and adds all history entries of the CurrencyConversionRateDO children.
     */
    override fun addOwnHistoryEntries(obj: CurrencyPairDO, context: HistoryLoadContext) {
        currencyConversionService.selectAllRates(obj, deleted = null).forEach { rateDO ->
            historyService.loadAndMergeHistory(rateDO, context)
        }
    }

    init {
        userRightId = USER_RIGHT_ID
    }

    companion object {
        val USER_RIGHT_ID = UserRightId.FIBU_CURRENCY_CONVERSION
        private val DEFAULT_SORT_PROPERTIES = arrayOf(
            SortProperty("sourceCurrency"),
            SortProperty("targetCurrency")
        )
        private val ENABLED_AUTOCOMPLETION_PROPERTIES = arrayOf("sourceCurrency", "targetCurrency")
    }
}
