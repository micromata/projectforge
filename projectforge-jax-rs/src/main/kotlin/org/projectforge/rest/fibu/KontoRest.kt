package org.projectforge.rest.fibu

import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Konto
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/konto")
class KontoRest() : AbstractDTORest<KontoDO, Konto, KontoDao, BaseSearchFilter>(KontoDao::class.java, BaseSearchFilter::class.java, "fibu.konto.title") {
    override fun transformDO(obj: KontoDO): Konto {
        val konto = Konto()
        konto.copyFrom(obj)
        return konto
    }

    override fun transformDTO(dto: Konto): KontoDO {
        val kontoDO = KontoDO()
        dto.copyTo(kontoDO)
        return kontoDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "nummer", "status", "bezeichnung", "description"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: KontoDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "status", "bezeichnung", "description")))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}