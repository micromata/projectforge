package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.ui.UILayout
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/poll/antwort")
class AntwortSeite : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        TODO("Not yet implemented")
    }

    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        TODO("Not yet implemented")
    }

    override fun transformForDB(dto: Poll): PollDO {
        TODO("Not yet implemented")
    }


}