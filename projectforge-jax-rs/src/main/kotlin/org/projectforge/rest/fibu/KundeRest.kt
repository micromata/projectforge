package org.projectforge.rest.fibu

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/customer")
class KundeRest() : AbstractStandardRest<KundeDO, KundeDao, BaseSearchFilter>(KundeDao::class.java, BaseSearchFilter::class.java, "fibu.kunde.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "bereich", "identifier", "name", "division", "konto", "status", "description"))
        //layout.getTableColumnById("konto").formatter = Formatter.USER
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: KundeDO): UILayout {
        val konto = UIInput("konto", lc, tooltip = "fibu.kunde.konto.tooltip")

        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "bereich", "name")
                                .add(konto)
                                .add(lc, "identifier", "division", "description", "status")))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}