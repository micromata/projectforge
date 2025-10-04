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

package org.projectforge.rest.fibu

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.business.fibu.CurrencyConversionRateDO
import org.projectforge.business.fibu.CurrencyConversionService
import org.projectforge.business.fibu.CurrencyPairDao
import org.projectforge.business.fibu.ExchangeRateApiService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.time.PFDay
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.CurrencyConversionRate
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dialog for registering a new currency conversion rate or modifying/deleting an existing one.
 */
@RestController
@RequestMapping("${Rest.URL}/currencyConversionRate")
class CurrencyConversionRatePageRest : AbstractDynamicPageRest() {
    class ResponseData(
        var rateEntries: List<CurrencyConversionRate>? = null,
    )

    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    @Autowired
    private lateinit var currencyPairDao: CurrencyPairDao

    @Autowired
    private lateinit var exchangeRateApiService: ExchangeRateApiService

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("id") idString: String?,
        @RequestParam("currencyPairId") currencyPairId: Long?,
    ): FormLayoutData {
        val id = idString?.toLongOrNull()
        requiredFields(id, currencyPairId)

        // Load currency pair once for both API call and UI customization
        val currencyPair = currencyPairDao.find(currencyPairId, checkAccess = false)
        val sourceCurrency = currencyPair?.sourceCurrency ?: ""
        val targetCurrency = currencyPair?.targetCurrency ?: ""

        val data = if (id!! > 0) {
            CurrencyConversionRate(currencyConversionService.findRate(id))
        } else {
            CurrencyConversionRate(currencyPairId = currencyPairId).apply {
                validFrom = LocalDate.now()
                // Try to fetch current exchange rate from external API
                if (sourceCurrency.isNotBlank() && targetCurrency.isNotBlank()) {
                    conversionRate = exchangeRateApiService.fetchCurrentRate(sourceCurrency, targetCurrency)
                }
            }
        }
        val lc = LayoutContext(CurrencyConversionRateDO::class.java)
        val layout = UILayout("fibu.currencyConversion.conversionRate")
        layout.add(lc, "validFrom")

        // Add conversion rate field (will be modified below for custom format)
        layout.add(
            UIRow()
                .add(
                    UICol(UILength(1))
                        .add(UILabel(label = "$sourceCurrency 1 ="))  // Will be set below with target currency
                )
                .add(
                    UICol(UILength(1))
                        .add(lc, "conversionRate")
                )
                .add(
                    UICol(UILength(2))
                        .add(UILabel(label = targetCurrency))  // Will be set below with target currency
                )
        )

        // Modify label of conversionRate input to show source currency
        (layout.getElementById("conversionRate") as? UIInput)?.label = ""

        layout.add(lc, "comment")

        // Watch validFrom field to fetch exchange rate when date changes
        layout.watchFields.add("validFrom")

        if (id < 0) {
            // New entry
            layout.addAction(
                UIButton.createAddButton(
                    responseAction = ResponseAction(
                        url = RestResolver.getRestUrl(this::class.java, "insert"),
                        targetType = TargetType.POST,
                    )
                )
            )
        } else {
            layout.addAction(
                UIButton.createDeleteButton(
                    layout,
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "delete"),
                        targetType = TargetType.POST,
                    ),
                )
            )
            layout.addAction(
                UIButton.createUpdateButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "update"),
                        targetType = TargetType.POST
                    ),
                )
            )
        }

        layout.addTranslations("cancel", "yes")
        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(@Valid @RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<ResponseAction> {
        val data = postData.data

        // Wenn validFrom geändert wurde, hole neuen Kurs von API
        if (postData.watchFieldsTriggered?.contains("validFrom") == true && data.validFrom != null) {
            val currencyPair = currencyPairDao.find(data.currencyPairId, checkAccess = false)
            if (currencyPair?.sourceCurrency != null && currencyPair.targetCurrency != null) {
                val newRate = exchangeRateApiService.fetchRateForDate(
                    currencyPair.sourceCurrency!!,
                    currencyPair.targetCurrency!!,
                    data.validFrom!!
                )
                data.conversionRate = newRate
            }
        }

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", data)
        )
    }

    @PostMapping("delete")
    fun delete(@RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<*> {
        requiredFields(postData.data)
        val dto = postData.data
        currencyConversionService.markRateAsDeleted(currencyPairId = dto.currencyPairId, rateId = dto.id)
        return closeModal(dto)
    }

    @PostMapping("update")
    fun update(@RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<ResponseAction> {
        requiredFields(postData.data)
        val dto = postData.data
        validate(dto)?.let { return it }
        val rateDO = dto.cloneAsDO()
        currencyConversionService.updateRate(currencyPairId = dto.currencyPairId, rateDO = rateDO)
        return closeModal(dto)
    }

    @PostMapping("insert")
    fun insert(@RequestBody postData: PostData<CurrencyConversionRate>): ResponseEntity<ResponseAction> {
        requiredFields(postData.data, idNull = true)
        validate(postData.data)?.let { return it }
        val dto = postData.data
        val rateDO = dto.cloneAsDO()
        currencyConversionService.insertRate(currencyPairId = dto.currencyPairId!!, rateDO = rateDO)
        return closeModal(dto)
    }

    private fun closeModal(dto: CurrencyConversionRate): ResponseEntity<ResponseAction> {
        val responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
        val rates = currencyConversionService.selectAllRates(dto.currencyPairId!!, deleted = false)
            .map { CurrencyConversionRate(it) }
        responseAction.addVariable("data", ResponseData(rateEntries = rates))
        return ResponseEntity.ok().body(responseAction)
    }

    private fun requiredFields(dto: CurrencyConversionRate, idNull: Boolean = false) {
        requiredFields(dto.id, dto.currencyPairId, idNull)
    }

    private fun requiredFields(
        id: Long?,
        currencyPairId: Long?,
        idNull: Boolean = false,
    ) {
        if (idNull) {
            require(id == null) { "Can't insert CurrencyConversionRate entry with given id." }
        } else {
            requireNotNull(id) { "Can't update/delete CurrencyConversionRate entry without id." }
        }
        requireNotNull(currencyPairId) { "Can't update CurrencyConversionRate entry without currencyPairId." }
    }

    private fun validate(dto: CurrencyConversionRate): ResponseEntity<ResponseAction>? {
        val validationErrors = mutableListOf<ValidationError>()

        // Validate conversion rate (custom business logic)
        if (dto.conversionRate != null && dto.conversionRate!! <= BigDecimal.ZERO) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.greaterZero"),
                    fieldId = "conversionRate",
                )
            )
        }

        // Validate validFrom (custom business logic)
        dto.validFrom?.let { validFrom ->
            val date = PFDay.fromOrNull(validFrom)
            if (date != null && PFDay.now().isBefore(date)) {
                validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "validFrom"))
            }
        }

        // Check for duplicates (custom business logic)
        currencyConversionService.validateRate(dto.cloneAsDO())?.let { msg ->
            validationErrors.add(ValidationError.create(msg))
        }

        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(
                ResponseAction(validationErrors = validationErrors),
                HttpStatus.NOT_ACCEPTABLE
            )
        }
        return null
    }
}
