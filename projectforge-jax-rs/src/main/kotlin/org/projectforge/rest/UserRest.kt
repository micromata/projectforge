package org.projectforge.rest

import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/user")
class UserRest()
    : AbstractStandardRest<PFUserDO, UserDao, BaseSearchFilter>(UserDao::class.java, BaseSearchFilter::class.java, "user.title") {

    @Autowired
    private lateinit var userDao: UserDao

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "username"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: PFUserDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "username")

        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
