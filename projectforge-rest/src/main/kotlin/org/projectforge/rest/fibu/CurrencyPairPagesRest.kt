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
import org.projectforge.business.fibu.CurrencyConversionService
import org.projectforge.business.fibu.CurrencyPairDO
import org.projectforge.business.fibu.CurrencyPairDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.CurrencyConversionRate
import org.projectforge.rest.dto.CurrencyPair
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("${Rest.URL}/currencyPair")
class CurrencyPairPagesRest :
    AbstractDTOPagesRest<CurrencyPairDO, CurrencyPair, CurrencyPairDao>(
        CurrencyPairDao::class.java,
        "fibu.currencyPair.title"
    ) {

    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    override fun transformFromDB(obj: CurrencyPairDO, editMode: Boolean): CurrencyPair {
        val currencyPair = CurrencyPair()
        currencyPair.copyFrom(obj)
        // Load current rate only for existing entries (id != null)
        if (obj.id != null) {
            currencyPair.currentRate =
                currencyConversionService.getConversionRate(obj, LocalDate.now(), checkAccess = false)
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

    override fun onBeforeGetItemAndLayout(
        request: HttpServletRequest,
        dto: CurrencyPair,
        userAccess: UILayout.UserAccess
    ) {
        dto.id?.let { id ->
            dto.rateEntries = currencyConversionService.selectAllRates(id, deleted = false)
                .map { CurrencyConversionRate(it) }
        }
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
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            .add(
                lc,
                "sourceCurrency",
                "targetCurrency"
            )
            .add(
                "currentRate", headerName = "fibu.currencyConversion.currentRate",
            )
            .add(lc, "comment")
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: CurrencyPair, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "sourceCurrency")
                    )
                    .add(
                        UICol()
                            .add(lc, "targetCurrency")
                    )
            )
            .add(
                UIRow()
                    .add(UICol().add(lc, "comment"))
            )

        if (dto.id == null) {
            layout.add(UIAlert("fibu.currencyPair.insert.hint", color = UIColor.INFO))
        } else {
            layout.layoutBelowActions
                .add(
                    UIRow()
                        .add(
                            UICol().add(
                                UIFieldset(title = "fibu.currencyConversion.rates")
                                    .add(
                                        UIAgGrid("rateEntries")
                                            .add(
                                                UIAgGridColumnDef.createCol(
                                                    lc,
                                                    "validFrom",
                                                    headerName = "attr.validSince"
                                                )
                                            )
                                            .add(
                                                UIAgGridColumnDef.createCol(
                                                    lc,
                                                    "conversionRate",
                                                    headerName = "fibu.currencyConversion.conversionRate"
                                                )
                                            )
                                            .add(UIAgGridColumnDef.createCol(lc, "comment", headerName = "comment"))
                                            .withRowClickRedirectUrl(
                                                createModalUrl(dto),
                                                openModal = true,
                                            )
                                    ).add(
                                        UIButton.createAddButton(
                                            responseAction = ResponseAction(
                                                createModalUrl(dto, isNew = true),
                                                targetType = TargetType.MODAL
                                            ),
                                            default = false,
                                        )
                                    )
                            )
                        )
                )
        }
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onAfterSave(obj: CurrencyPairDO, postData: PostData<CurrencyPair>): ResponseAction {
        // Redirect to edit page after insert for allowing user to add conversion rates.
        return ResponseAction(
            PagesResolver.getEditPageUrl(
                CurrencyPairPagesRest::class.java,
                obj.id,
                absolute = true
            )
        )
            .addVariable("id", obj.id ?: -1)
    }

    private fun createModalUrl(dto: CurrencyPair, isNew: Boolean = false): String {
        val id = if (isNew) -1 else ":\${row.id}"
        return PagesResolver.getDynamicPageUrl(
            CurrencyConversionRatePageRest::class.java,
            id = id,
            params = mapOf("currencyPairId" to (dto.id ?: -1)),
            absolute = true
        )
    }
}
