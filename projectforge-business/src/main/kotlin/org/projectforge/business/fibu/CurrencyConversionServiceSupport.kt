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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDOPersistenceService
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Support class for currency conversion service providing database query methods.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
internal class CurrencyConversionServiceSupport {
    @Autowired
    private lateinit var baseDOPersistenceService: BaseDOPersistenceService

    @Autowired
    private lateinit var cache: CurrencyConversionCache

    @Autowired
    private lateinit var currencyPairDao: CurrencyPairDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * @param id The id of the rate to select.
     * @param checkAccess If true, the logged-in user must have access to the rate.
     * @return The rate with the given id or null if it does not exist.
     */
    fun findRate(
        id: Long?,
        checkAccess: Boolean = true
    ): CurrencyConversionRateDO? {
        id ?: return null
        if (checkAccess) {
            currencyPairDao.checkLoggedInUserSelectAccess()
        }
        return persistenceService.find(CurrencyConversionRateDO::class.java, id)
    }

    /**
     * @param currencyPair The currency pair to select rates for.
     * @param deleted If true, only deleted rates will be returned, if false, only not deleted rates will be returned. If null, deleted and not deleted rates will be returned.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     */
    internal fun selectAllRates(
        currencyPair: CurrencyPairDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<CurrencyConversionRateDO> {
        requireNotNull(currencyPair.id) { "Currency pair id must not be null." }
        if (checkAccess) {
            currencyPairDao.checkLoggedInUserSelectAccess(currencyPair)
        }
        val list = persistenceService.executeNamedQuery(
            CurrencyConversionRateDO.FIND_ALL_BY_PAIR,
            CurrencyConversionRateDO::class.java,
            Pair("currencyPairId", currencyPair.id)
        )
        if (deleted != null) {
            return list.filter { it.deleted == deleted }
        }
        return list
    }

    /**
     * Finds the active rate for the given date from a list of rates.
     * The active rate is the one with the latest validFrom date that is not after the given date.
     * @param rates List of rates sorted by validFrom desc.
     * @param validAtDate The date for which to find the active rate. Defaults to today.
     * @return The active rate or null if no rate is valid for the given date.
     */
    fun getActiveRate(
        rates: List<CurrencyConversionRateDO>,
        validAtDate: LocalDate? = null,
    ): CurrencyConversionRateDO? {
        val date = validAtDate ?: LocalDate.now()
        // List is already sorted by validFrom desc, so we find the first one that is not after the given date
        return rates.firstOrNull { rateDO ->
            val validFrom = rateDO.validFrom
            validFrom != null && !validFrom.isAfter(date)
        }
    }

    /**
     * Saves or updates a currency conversion rate.
     * @param rate The rate to save or update.
     * @param checkAccess If true, the logged-in user must have insert/update access.
     */
    fun saveOrUpdate(
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true
    ): CurrencyConversionRateDO {
        if (rate.id == null) {
            baseDOPersistenceService.insert(rate, checkAccess = checkAccess)
        } else {
            baseDOPersistenceService.update(rate, checkAccess = checkAccess)
        }
        cache.setExpired()
        return rate
    }

    /**
     * Deletes a currency conversion rate.
     * @param rate The rate to delete.
     * @param checkAccess If true, the logged-in user must have delete access.
     */
    fun delete(
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true
    ) {
        baseDOPersistenceService.markAsDeleted(obj = rate, checkAccess = checkAccess)
        cache.setExpired()
    }

    /**
     * Validates a rate for uniqueness.
     * @param rate The rate to validate.
     * @return The existing rate with the same pair and date, if it exists and is deleted (for re-use).
     * @throws IllegalArgumentException If a rate with the same pair and date already exists, and the existing other isn't deleted.
     */
    @Throws(IllegalArgumentException::class)
    internal fun validate(rate: CurrencyConversionRateDO): CurrencyConversionRateDO? {
        val other = if (rate.id == null) {
            // New entry
            persistenceService.selectNamedSingleResult(
                CurrencyConversionRateDO.FIND_BY_PAIR_AND_DATE,
                CurrencyConversionRateDO::class.java,
                Pair("currencyPairId", rate.currencyPair!!.id!!),
                Pair("validFrom", rate.validFrom),
            )
        } else {
            // Rate already exists:
            persistenceService.selectNamedSingleResult(
                CurrencyConversionRateDO.FIND_OTHER_BY_PAIR_AND_DATE,
                CurrencyConversionRateDO::class.java,
                Pair("currencyPairId", rate.currencyPair!!.id!!),
                Pair("validFrom", rate.validFrom),
                Pair("id", rate.id),
            )
        }
        if (other == null) {
            return null
        }
        if (other.deleted && other.validFrom == rate.validFrom) {
            // This algorithm is needed to handle deleted entries. Otherwise, they aren't visible for the users and
            // the users can't add or modify entries with the validFrom date of the deleted ones.
            return other
        }
        throw IllegalArgumentException("A rate with the same date already exists: $other")
    }

    /**
     * Inserts a new rate.
     * @param currencyPairId The currency pair id.
     * @param rate The rate to insert.
     * @param checkAccess If true, the logged-in user must have insert access.
     * @return The id of the inserted rate.
     */
    fun insert(
        currencyPairId: Long,
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true,
    ): Long? {
        val currencyPair = currencyPairDao.find(currencyPairId)!!
        return insert(currencyPair, rate, checkAccess)
    }

    /**
     * Inserts a new rate.
     * @param currencyPair The currency pair.
     * @param rate The rate to insert.
     * @param checkAccess If true, the logged-in user must have insert access.
     * @return The id of the inserted rate.
     */
    fun insert(
        currencyPair: CurrencyPairDO,
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true,
    ): Long? {
        if (currencyPair.id != rate.currencyPair?.id) {
            throw IllegalArgumentException("Currency pair id of rate does not match currency pair id.")
        }
        if (checkAccess) {
            currencyPairDao.checkLoggedInUserInsertAccess(currencyPair)
        }
        val other = validate(rate)
        try {
            if (other != null) {
                // This algorithm is needed to handle deleted entries. Otherwise, they aren't visible for the users and
                // the users can't add or modify entries with the validFrom date of the deleted ones.
                other.copyFrom(rate)
                baseDOPersistenceService.undelete(obj = other, checkAccess = checkAccess)
                return other.id
            }
            return baseDOPersistenceService.insert(rate, checkAccess = checkAccess)
        } finally {
            cache.setExpired()
        }
    }

    /**
     * Updates an existing rate.
     * @param currencyPair The currency pair.
     * @param rate The rate to update.
     * @param checkAccess If true, the logged-in user must have update access.
     */
    fun updateRate(
        currencyPair: CurrencyPairDO,
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true,
    ): org.projectforge.framework.persistence.api.EntityCopyStatus {
        if (currencyPair.id != rate.currencyPair?.id) {
            throw IllegalArgumentException("Currency pair id of rate does not match currency pair id.")
        }
        if (checkAccess) {
            currencyPairDao.checkLoggedInUserUpdateAccess(currencyPair, currencyPair)
        }
        val other = validate(rate)
        try {
            if (other != null) {
                // This algorithm is needed to handle deleted entries. Otherwise, they aren't visible for the users and
                // the users can't add or modify entries with the validFrom date of the deleted ones.
                findRate(rate.id, checkAccess = checkAccess).let { dbEntry ->
                    requireNotNull(dbEntry) { "Can't update CurrencyConversionRate entry without existing id." }
                    // Mark current entry as deleted and modify existing deleted entry with desired validFrom date.
                    markRateAsDeleted(currencyPair, dbEntry, checkAccess)
                }
                other.copyFrom(rate)
                // Undeleting and updating existing entry instead of inserting new entry.
                return baseDOPersistenceService.undelete(obj = other, checkAccess = checkAccess)
            }
            return baseDOPersistenceService.update(rate, checkAccess = checkAccess)
        } finally {
            cache.setExpired()
        }
    }

    /**
     * Marks a rate as deleted.
     * @param currencyPairId The currency pair id.
     * @param rateId The rate id.
     * @param checkAccess If true, the logged-in user must have update access.
     */
    fun markRateAsDeleted(
        currencyPairId: Long?,
        rateId: Long?,
        checkAccess: Boolean = true,
    ) {
        val currencyPair = currencyPairDao.find(currencyPairId)!!
        val rate = findRate(rateId, checkAccess = checkAccess)!!
        markRateAsDeleted(currencyPair, rate, checkAccess)
        cache.setExpired()
    }

    /**
     * Marks a rate as deleted.
     * @param currencyPair The currency pair.
     * @param rate The rate to delete.
     * @param checkAccess If true, the logged-in user must have update access.
     */
    fun markRateAsDeleted(
        currencyPair: CurrencyPairDO,
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true,
    ) {
        require(rate.currencyPair!!.id == currencyPair.id!!) { "Currency pair id of rate does not match currency pair id." }
        if (checkAccess) {
            currencyPairDao.checkLoggedInUserUpdateAccess(currencyPair, currencyPair)
        }
        validate(rate)
        baseDOPersistenceService.markAsDeleted(obj = rate, checkAccess = checkAccess)
        cache.setExpired()
    }
}
