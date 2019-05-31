package org.projectforge.plugins.poll.rest

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.poll.PollDO
import org.projectforge.plugins.poll.PollDao
import org.projectforge.plugins.poll.dto.Poll
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollRest() : AbstractDTORest<PollDO, Poll, PollDao, BaseSearchFilter>(PollDao::class.java, BaseSearchFilter::class.java, "plugins.poll.title") {

    override fun transformDO(obj: PollDO, editMode: Boolean): Poll {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun transformDTO(dto: Poll): PollDO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Initializes new polls for adding.
     */
    override fun newBaseDO(request: HttpServletRequest): PollDO {
        val poll = super.newBaseDO(request)
        poll.owner = ThreadLocalUserContext.getUser()
        return poll
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title", "description", "location", "owner", "lastUpdate"))
        layout.getTableColumnById("owner").formatter = Formatter.USER
        layout.getTableColumnById("lastUpdate").formatter = Formatter.TIMESTAMP_MINUTES
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: PollDO): UILayout {
        val location = UIInput("location", lc).enableAutoCompletion(this)
        val layout = super.createEditLayout(dataObject)
                .add(lc, "title")
                .add(location)
                .add(lc, "description")
                .add(UISelect<Int>("assignedItems", lc,
                        multi = true,
                        label = "plugins.poll.attendee.users",
                        //additionalLabel = "access.users",
                        autoCompletion = AutoCompletion<Int>(url = "user/aco"),
                        labelProperty = "fullname",
                        valueProperty = "id"))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
