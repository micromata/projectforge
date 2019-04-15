package org.projectforge.rest.orga

import org.projectforge.business.orga.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import javax.ws.rs.Path

@Component
@Path("contract")
class ContractRest() : AbstractStandardRest<PostausgangDO, PostausgangDao, ContractFilter>(PostausgangDao::class.java, ContractFilter::class.java, "orga.postausgang.title") {
    /**
     * Initializes new outbox mails for adding.
     */
    override fun newBaseDO(): PostausgangDO {
        val outbox = super.newBaseDO()
        outbox.datum = PFDate.now().asSqlDate()
        outbox.type = PostType.BRIEF
        return outbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: PostausgangDO) {
        val date = PFDate.from(obj.datum)
        val today = PFDate.now()
        if (today.isBefore(date)) { // No dates in the future accepted.
            validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "datum"))
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "number", "date", "type", "status", "title", "coContractorA", "coContractorB",
                                "resubmissionOnDate", "dueDate"))
        layout.getTableColumnById("datum").formatter = Formatter.DATE
        LayoutUtils.addListFilterContainer(layout, UILabel("'TODO: date range"),
                filterClass = ContractFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: PostausgangDO?): UILayout {
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
                                .add(lc, "number")
                                .add(lc, "date")
                                .add(title)
                                .add(lc, "status"))
                        .add(UICol()
                                .add(lc, "reference")
                                .add(lc, "resubmissionOnDate")
                                .add(lc, "dueDate")
                                .add(lc, "signingDate")
                                .add(lc, "validity")))
                .add(UIRow()
                        .add(UICol()
                                .add(coContractorA)
                                .add(contractPersonA)
                                .add(signerA))
                        .add(UICol()
                                .add(coContractorB)
                                .add(contractPersonB)
                                .add(signerB)))
                .add(lc, "text")
                .add(lc, "filling")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}