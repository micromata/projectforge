package org.projectforge.rest.orga

import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractDao
import org.projectforge.business.orga.ContractFilter
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/contract")
class ContractRest() : AbstractDORest<ContractDO, ContractDao, ContractFilter>(ContractDao::class.java, ContractFilter::class.java, "legalAffaires.contract.title") {
    /**
     * Initializes new outbox mails for adding.
     */
    override fun newBaseDO(request: HttpServletRequest): ContractDO {
        val contract = super.newBaseDO(request)
        contract.date = PFDate.now().asSqlDate()
        return contract
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: ContractDO) {
        val date = PFDate.from(obj.date)
        val today = PFDate.now()
        if (today.isBefore(date)) { // No dates in the future accepted.
            validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "date"))
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "number", "date", "type", "status", "title", "coContractorA", "coContractorB", "resubmissionOnDate", "dueDate"))
        layout.getTableColumnById("date").formatter = Formatter.DATE
        LayoutUtils.addListFilterContainer(layout, UILabel("'TODO: date range"),
                filterClass = ContractFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: ContractDO): UILayout {
        val title = UIInput("title", lc).enableAutoCompletion(this)
        val coContractorA = UIInput("coContractorA", lc).enableAutoCompletion(this)
        val coContractorB = UIInput("coContractorB", lc).enableAutoCompletion(this)
        val contractPersonA = UIInput("contractPersonA", lc).enableAutoCompletion(this)
        val contractPersonB = UIInput("contractPersonB", lc).enableAutoCompletion(this)
        val signerA = UIInput("signerA", lc).enableAutoCompletion(this)
        val signerB = UIInput("signerB", lc).enableAutoCompletion(this)


        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "number", "date")
                                .add(title)
                                .add(lc, "type", "status"))
                        .add(UICol()
                                .add(lc, "reference", "resubmissionOnDate", "dueDate", "signingDate", "validFrom")))
                .add(UIRow()
                        .add(UICol()
                                .add(coContractorA)
                                .add(contractPersonA)
                                .add(signerA))
                        .add(UICol()
                                .add(coContractorB)
                                .add(contractPersonB)
                                .add(signerB)))
                .add(lc, "text", "filing")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
