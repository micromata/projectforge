package org.projectforge.rest

import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.Formatter
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/vacation")
class VacationRest : AbstractDORest<VacationDO, VacationDao, BaseSearchFilter>(VacationDao::class.java, BaseSearchFilter::class.java, "vacation.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "employee", "startDate", "endDate", "assignment", "status", "workingDays",
                                "specialLeave", "manager", "substitution"))
        layout.getTableColumnById("startDate").formatter = Formatter.DATE
        layout.getTableColumnById("endDate").formatter = Formatter.DATE
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: VacationDO): UILayout {
        val layout = super.createEditLayout(dto)
                .add(lc, "TODO")
        return LayoutUtils.processEditPage(layout, dto, this)
    }


}