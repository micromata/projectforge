package org.projectforge.rest

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.plugins.memo.MemoDO
import org.projectforge.plugins.memo.MemoDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/memo")
class MemoRest() : AbstractDORest<MemoDO, MemoDao, BaseSearchFilter>(MemoDao::class.java, BaseSearchFilter::class.java, "memo.title") {
    /**
     * Initializes new memos for adding.
     */
    override fun newBaseDO(request: HttpServletRequest): MemoDO {
        val memo = super.newBaseDO(request)
        return memo
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: MemoDO) {

    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "created", "modified", "subject", "memo"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: MemoDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "subject", "memo")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
