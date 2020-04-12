/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.orga

import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractDao
import org.projectforge.business.vacation.model.VacationYearFilter
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/contract")
class ContractPagesRest() : AbstractDOPagesRest<ContractDO, ContractDao>(ContractDao::class.java, "legalAffaires.contract.title") {
    /**
     * Initializes new outbox mails for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): ContractDO {
        val contract = super.newBaseDO(request)
        contract.date = LocalDate.now()
        return contract
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: ContractDO) {
        val date = PFDay.fromOrNull(dto.date)
        if (date != null && PFDay.now().isBefore(date)) { // No dates in the future accepted.
            validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "date"))
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(lc, "number", "date", "type", "status", "title", "coContractorA", "coContractorB", "resubmissionOnDate", "dueDate"))
        layout.getTableColumnById("date").formatter = Formatter.DATE
        return LayoutUtils.processListPage(layout, this)
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(UIFilterElement("year", label = translate("calendar.year"), defaultFilter = true))
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<ContractDO>>? {
        val filters = mutableListOf<CustomResultFilter<ContractDO>>()
        source.entries.find { it.field == "year" }?.let {entry ->
            entry.synthetic = true
            NumberHelper.parseInteger(entry.value.value)?.let { year ->
                target.setYearAndMonth("date", year, -1)
            }
        }
        return filters
    }


    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: ContractDO, userAccess: UILayout.UserAccess): UILayout {
        val title = UIInput("title", lc).enableAutoCompletion(this)
        val coContractorA = UIInput("coContractorA", lc).enableAutoCompletion(this)
        val coContractorB = UIInput("coContractorB", lc).enableAutoCompletion(this)
        val contractPersonA = UIInput("contractPersonA", lc).enableAutoCompletion(this)
        val contractPersonB = UIInput("contractPersonB", lc).enableAutoCompletion(this)
        val signerA = UIInput("signerA", lc).enableAutoCompletion(this)
        val signerB = UIInput("signerB", lc).enableAutoCompletion(this)


        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UIFieldset(UILength(md = 6))
                                .add(lc, "number")
                                .add(title)
                                .add(lc, "type", "status", "reference"))
                        .add(UIFieldset(UILength(md = 6))
                                .add(lc, "date", "resubmissionOnDate", "dueDate", "signingDate")
                                .add(UIRow()
                                        .add(UICol(6).add(lc, "validFrom"))
                                        .add(UICol(6).add(lc, "validUntil")))))
                .add(UIRow()
                        .add(UIFieldset(UILength(md = 6), title = "legalAffaires.contract.coContractorA")
                                .add(coContractorA)
                                .add(contractPersonA)
                                .add(signerA))
                        .add(UIFieldset(UILength(md = 6), title = "legalAffaires.contract.coContractorB")
                                .add(coContractorB)
                                .add(contractPersonB)
                                .add(signerB)))
                .add(lc, "text", "filing")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
