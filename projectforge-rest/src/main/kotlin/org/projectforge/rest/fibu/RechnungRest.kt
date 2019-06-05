package org.projectforge.rest.fibu

import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/invoice")
class RechnungRest() : AbstractDORest<RechnungDO, RechnungDao, BaseSearchFilter>(RechnungDao::class.java, BaseSearchFilter::class.java, "fibu.rechnung.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        // TODO:
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "nummer", "kunde", "projekt", "account", "betreff", "datum", "faelligkeit",
                                "bezahlDatum", "periodOfPerformanceBegin", "periodOfPerformanceEnd")
                        .add(UITableColumn("netSum", title = translate("fibu.common.netto"), dataType = UIDataType.DECIMAL))
                        .add(UITableColumn("grossSum", title = translate("fibu.rechnung.bruttoBetrag"), dataType = UIDataType.DECIMAL))
                        .add(lc, "orders", "bemerkung", "status"))
        layout.getTableColumnById("kunde").formatter = Formatter.CUSTOMER
        layout.getTableColumnById("projekt").formatter = Formatter.PROJECT
        layout.getTableColumnById("datum").formatter = Formatter.DATE
        layout.getTableColumnById("faelligkeit").formatter = Formatter.DATE
        layout.getTableColumnById("bezahlDatum").formatter = Formatter.DATE
        layout.getTableColumnById("periodOfPerformanceBegin").formatter = Formatter.DATE
        layout.getTableColumnById("periodOfPerformanceEnd").formatter = Formatter.DATE
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: RechnungDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "betreff")
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "typ"))
                        .add(UICol()
                                .add(lc, "status", "account"))
                        .add(UICol()
                                .add(lc, "datum", "vatAmountSum", "bezahlDatum", "faelligkeit"))
                        .add(UICol()
                                .add(lc, "netSum", "grossSum")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "projekt", "kunde", "customerAddress", "customerref1", "attachment")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "bemerkung"))
                        .add(UICol()
                                .add(lc, "besonderheiten")))
                .add(UILabel("TODO: Customized element for Pos"))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}