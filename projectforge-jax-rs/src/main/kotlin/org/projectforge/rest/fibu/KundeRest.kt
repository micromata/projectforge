package org.projectforge.rest.fibu

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Kunde
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/customer")
class KundeRest() : AbstractDTORest<KundeDO, Kunde, KundeDao, BaseSearchFilter>(KundeDao::class.java, BaseSearchFilter::class.java, "fibu.kunde.title") {
    override fun transformDO(obj: KundeDO): Kunde {
        val kunde = Kunde()
        kunde.copyFrom(obj)
        return kunde
    }

    override fun transformDTO(dto: Kunde): KundeDO {
        val kundeDO = KundeDO()
        dto.copyTo(kundeDO)
        return kundeDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "nummer", "identifier", "name", "division", "konto", "status", "description"))
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
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
                                .add(lc, "nummer", "name")
                                .add(konto)
                                .add(lc, "identifier", "division", "description", "status")))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
