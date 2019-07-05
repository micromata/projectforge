package org.projectforge.rest

import org.projectforge.business.humanresources.HRPlanningDO
import org.projectforge.business.humanresources.HRPlanningDao
import org.projectforge.business.humanresources.HRPlanningFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILabel
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/hrPlanning")
class HRPlanningRest: AbstractDORest<HRPlanningDO, HRPlanningDao, HRPlanningFilter>(HRPlanningDao::class.java, HRPlanningFilter::class.java, "hr.planning.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "user", "sum", "rest"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: HRPlanningDO): UILayout {
        val layout = super.createEditLayout(dto)
                .add(UILabel("TODO"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}