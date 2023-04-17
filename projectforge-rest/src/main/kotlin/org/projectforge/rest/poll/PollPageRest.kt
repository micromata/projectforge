package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.VacationExportPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    @Autowired
    private lateinit var userService: UserService;

    override fun transformForDB(dto: Poll): PollDO {
        val pollDO = PollDO()
        dto.copyTo(pollDO)
        return pollDO
    }

    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        User.restoreDisplayNames(poll.fullAccessUsers, userService)
        return poll
    }

    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            .add(lc, "title", "description", "location")
            .add(lc, "owner")
            .add(lc, "deadline")
            .add(lc, "date")
        layout.add(
            MenuItem(
                "export",
                i18nKey = "poll.export.title",
                url = PagesResolver.getDynamicPageUrl(VacationExportPageRest::class.java),
                type = MenuItemTargetType.REDIRECT,
            )
        )
    }

    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val lc = LayoutContext(PollDO::class.java)
        val obj = PollDO()
        dto.copyTo(obj)
        val layout = super.createEditLayout(dto, userAccess)
        layout.add(
            UIRow()
                .add(
                    UIFieldset(UILength(md = 6, lg = 4))
                        .add(lc, "title", "description", "location", "owner", "deadline", "date")
                        .add(UISelect.createUserSelect(lc, "fullAccessUsers", true, "poll.fullAccessUsers"))
                        .add(UISelect.createGroupSelect(lc, "fullAccessGroups", true, "poll.fullAccessGroups"))
                /*.add(
        UIFieldset(UILength(md = 12, lg = 12), title = "access.title.heading")
          .add(
            UIRow()
              .add(
                UIFieldset(6, title = "access.users")
                  .add(UISelect.createUserSelect(lc, "fullAccessUsers", true, "plugins.banking.account.fullAccess"))
                  .add(
                    UISelect.createUserSelect(
                      lc,
                      "readonlyAccessUsers",
                      true,
                      "plugins.banking.account.readonlyAccess"
                    )
                  )
              )
              .add(
                UIFieldset(6, title = "access.groups")
                  .add(UISelect.createGroupSelect(lc, "fullAccessGroups", true, "plugins.banking.account.fullAccess"))
                  .add(
                    UISelect.createGroupSelect(
                      lc,
                      "readonlyAccessGroups",
                      true,
                      "plugins.banking.account.readonlyAccess"
                    )
                  )
              )*/
                ))
            .add(UIButton.createAddButton(responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST)))
                layout.watchFields.addAll(
            arrayOf(
                "title",
                "description",
                "location",
                "deadline",
                "date"
            )
        )
        updateStats(dto)
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onWatchFieldsUpdate(
        request: HttpServletRequest,
        dto: Poll,
        watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        val title = dto.title
        val description = dto.description
        val location = dto.location
        val deadline = dto.deadline
        val date = dto.date

        updateStats(dto)
        val userAccess = UILayout.UserAccess()
        val poll = PollDO()
        dto.copyTo(poll)
        checkUserAccess(poll, userAccess)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, userAccess))
        )
    }

    private fun updateStats(dto: Poll) {

        val title = dto.title
        val description = dto.description
        val location = dto.location
        val deadline = dto.deadline
        val date = dto.date

        val pollDO = PollDO()
        dto.copyTo(pollDO)
    }


    /*dto.inputFields?.forEachIndexed { field, index ->
            if (field.type == msc) {
             layout.add() //
             "type[$index]"
              Id: name
            }
        }
        layout.add(UIRow().add(UIFieldset(UILength(md = 6, lg = 4))
            .add(lc, "name")))
    */
}