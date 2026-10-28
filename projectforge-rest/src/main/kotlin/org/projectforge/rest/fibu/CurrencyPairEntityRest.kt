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


package org.projectforge.rest.fibu

import org.projectforge.business.fibu.CurrencyConversionRateDO
import org.projectforge.business.fibu.CurrencyConversionService
import org.projectforge.business.fibu.CurrencyPairDO
import org.projectforge.business.fibu.CurrencyPairDao
import org.projectforge.business.fibu.ExchangeRateApiService
import org.projectforge.common.extensions.formatForUser
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ValidationUtils
import org.projectforge.rest.dto.CurrencyConversionRate
import org.projectforge.rest.dto.CurrencyPair
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Currency pairs (list + form in projectforge-next, `/next/currencyPair`), including the conversion rates of a pair,
 * each valid from its date (see [CurrencyConversionRateDO]). The rates are written right away through `rate/...`,
 * not with the form of the pair.
 */
@RestController
@RequestMapping("${Rest.URL}/currencyPair")
class CurrencyPairEntityRest :
    AbstractDTOEntityRest<CurrencyPairDO, CurrencyPair, CurrencyPairDao>(
        CurrencyPairDao::class.java,
        "fibu.currencyPair.title"
    ) {

    /**
     * Response of the rate endpoints: all (not deleted) rates of the pair.
     */
    class RateEntries(val entries: List<CurrencyConversionRate>)

    /**
     * Response of [getOnlineRates]: the rates of both directions found online for the date (null if none), and a
     * note on their source if any was found.
     */
    class OnlineRates(
        val conversionRate: BigDecimal? = null,
        val inverseConversionRate: BigDecimal? = null,
        val info: String? = null,
    )

    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    @Autowired
    private lateinit var exchangeRateApiService: ExchangeRateApiService

    override fun transformFromDB(obj: CurrencyPairDO, editMode: Boolean): CurrencyPair {
        val currencyPair = CurrencyPair()
        currencyPair.copyFrom(obj)
        // Load current rate only for existing entries (id != null)
        obj.id?.let { id ->
            currencyPair.currentRate =
                currencyConversionService.getConversionRate(obj, today())
            currencyPair.currentInverseRate =
                currencyConversionService.getConversionRate(
                    obj,
                    today(),
                    inverseRate = true,
                )
            currencyPair.currentRateDate =
                currencyConversionService.getConversionRateDate(obj, today())
            if (editMode) {
                currencyPair.rateEntries = selectRates(id)
            }
        }
        return currencyPair
    }

    override fun transformForDB(dto: CurrencyPair): CurrencyPairDO {
        val currencyPairDO = CurrencyPairDO()
        dto.copyTo(currencyPairDO)
        // Normalize currency codes to uppercase
        currencyPairDO.sourceCurrency = currencyPairDO.sourceCurrency?.uppercase()
        currencyPairDO.targetCurrency = currencyPairDO.targetCurrency?.uppercase()
        return currencyPairDO
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: CurrencyPair) {
        // Check if source and target currencies are different (custom business logic)
        if (!dto.sourceCurrency.isNullOrBlank() && !dto.targetCurrency.isNullOrBlank() &&
            dto.sourceCurrency!!.uppercase() == dto.targetCurrency!!.uppercase()
        ) {
            validationErrors.add(
                ValidationError(
                    translate("fibu.currencyPair.validation.currenciesMustDiffer"),
                    fieldId = "targetCurrency"
                )
            )
        }
    }

    /**
     * The rates of both directions for the given date from the online source ([ExchangeRateApiService]), proposed
     * by the dialog of a new rate. Nothing is stored.
     */
    @AccessChecked("CurrencyPairDao: select access to the currency pair")
    @GetMapping("rate/onlineRates")
    fun getOnlineRates(
        @RequestParam("currencyPairId") currencyPairId: Long,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): OnlineRates {
        val currencyPair = baseDao.find(currencyPairId) // Checks select access.
            ?: throw IllegalArgumentException("Currency pair with id $currencyPairId not found.")
        val sourceCurrency = currencyPair.sourceCurrency
        val targetCurrency = currencyPair.targetCurrency
        if (sourceCurrency.isNullOrBlank() || targetCurrency.isNullOrBlank()) {
            return OnlineRates()
        }
        val day = date ?: today()
        val (rate, inverseRate) = exchangeRateApiService.fetchBothRates(sourceCurrency, targetCurrency, day)
        val info = if (rate != null || inverseRate != null) {
            translateMsg("fibu.currencyConversion.api.sourceInfo", day.formatForUser())
        } else {
            null
        }
        return OnlineRates(conversionRate = rate, inverseConversionRate = inverseRate, info = info)
    }

    /**
     * Inserts a conversion rate of a currency pair.
     */
    @AccessChecked("CurrencyConversionService: insert access to the currency pair (CurrencyPairDao)")
    @PostMapping("rate/insert")
    fun insertRate(@RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<*> {
        val dto = postData.data
        require(dto.id == null) { "Can't insert CurrencyConversionRate entry with given id." }
        val currencyPairId = requireNotNull(dto.currencyPairId) {
            "Can't insert CurrencyConversionRate entry without currencyPairId."
        }
        checkWriteAccess(currencyPairId, insert = true)
        val rateDO = dto.cloneAsDO(currencyPairId)
        validate(rateDO)?.let { return it }
        currencyConversionService.insertRate(currencyPairId = currencyPairId, rateDO = rateDO)
        return ratesResponse(currencyPairId)
    }

    /**
     * Updates a conversion rate. The pair is the one of the stored rate: a rate is never moved to another pair.
     */
    @AccessChecked("CurrencyConversionService: update access to the currency pair (CurrencyPairDao)")
    @PostMapping("rate/update")
    fun updateRate(@RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<*> {
        val dto = postData.data
        val currencyPairId = storedCurrencyPairId(dto)
        checkWriteAccess(currencyPairId)
        val rateDO = dto.cloneAsDO(currencyPairId)
        validate(rateDO)?.let { return it }
        currencyConversionService.updateRate(currencyPairId = currencyPairId, rateDO = rateDO)
        return ratesResponse(currencyPairId)
    }

    /**
     * Marks a conversion rate as deleted (kept for the history).
     */
    @AccessChecked("CurrencyConversionService: update access to the currency pair (CurrencyPairDao)")
    @PostMapping("rate/delete")
    fun deleteRate(@RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<*> {
        val dto = postData.data
        val currencyPairId = storedCurrencyPairId(dto)
        checkWriteAccess(currencyPairId)
        currencyConversionService.markRateAsDeleted(currencyPairId = currencyPairId, rateId = dto.id)
        return ratesResponse(currencyPairId)
    }

    private fun selectRates(currencyPairId: Long): List<CurrencyConversionRate> {
        return currencyConversionService.selectAllRates(currencyPairId, deleted = false)
            .map { CurrencyConversionRate(it) }
    }

    private fun ratesResponse(currencyPairId: Long): ResponseEntity<RateEntries> {
        return ResponseEntity.ok(RateEntries(selectRates(currencyPairId)))
    }

    /**
     * The pair of the stored rate, never the posted one (which would let an update move the rate to another pair).
     */
    private fun storedCurrencyPairId(dto: CurrencyConversionRate): Long {
        val id = requireNotNull(dto.id) { "Can't update/delete CurrencyConversionRate entry without id." }
        val stored = currencyConversionService.findRate(id) // Checks select access.
            ?: throw IllegalArgumentException("CurrencyConversionRate with id $id not found.")
        return requireNotNull(stored.currencyPair?.id) { "CurrencyConversionRate with id $id has no currency pair." }
    }

    /**
     * The access check of [CurrencyConversionService] again, but before [validate]: its lookup of a rate with the
     * same date would otherwise tell a user without access whether the pair has a rate on that date.
     */
    private fun checkWriteAccess(currencyPairId: Long, insert: Boolean = false) {
        val currencyPair = baseDao.find(currencyPairId)
            ?: throw IllegalArgumentException("Currency pair with id $currencyPairId not found.")
        if (insert) {
            baseDao.checkLoggedInUserInsertAccess(currencyPair)
        } else {
            baseDao.checkLoggedInUserUpdateAccess(currencyPair, currencyPair)
        }
    }

    private fun validate(rateDO: CurrencyConversionRateDO): ResponseEntity<ResponseAction>? {
        // Required fields and lengths, from the annotations of the entity.
        val validationErrors = ValidationUtils.validateFields(rateDO)
        rateDO.conversionRate?.let { rate ->
            if (rate <= BigDecimal.ZERO) {
                validationErrors.add(
                    ValidationError(translateMsg("validation.error.greaterZero"), fieldId = "conversionRate")
                )
            }
        }
        rateDO.inverseConversionRate?.let { rate ->
            if (rate <= BigDecimal.ZERO) {
                validationErrors.add(
                    ValidationError(translateMsg("validation.error.greaterZero"), fieldId = "inverseConversionRate")
                )
            }
        }
        rateDO.validFrom?.let { validFrom ->
            if (validFrom.isAfter(today())) {
                validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "validFrom"))
            }
            // Another rate of the pair on the same date:
            currencyConversionService.validateRate(rateDO)?.let { msg ->
                validationErrors.add(ValidationError(msg, fieldId = "validFrom"))
            }
        }
        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }
        return null
    }

    /**
     * Today in the user's time zone, as the dialog proposes it: the server's own zone may still be on the day
     * before (a rate entered just after midnight would be refused as in the future).
     */
    private fun today(): LocalDate = PFDateTime.now().localDate
}
