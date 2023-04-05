package org.projectforge.rest.poll

import org.checkerframework.checker.guieffect.qual.UIType
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.scripting.I18n
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.VacationExportPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    @Autowired
    private lateinit var pollDao: PollDao

    override fun transformForDB(dto: Poll): PollDO {
        val pollDO = PollDO()
        dto.copyTo(pollDO)
        return pollDO
    }

    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
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

        val layout = super.createEditLayout(dto, userAccess).add(UILabel("poll.create"))

    /*   // dto.inputFields?.forEachIndexed { field, index ->
            if (field.type == UIDataType.BOOLEAN) {
 // layout.add() // ID: type[]
                // "type[$index]"
                // Id: name
            }
        }*/
        /*layout.add(UIRow().add(UIFieldset(UILength(md = 6, lg = 4))
            .add(lc, "name")))
        */

        layout.add(
            UIRow()
                .add(
                    UIFieldset(UILength(md = 6, lg = 4))
                        .add(lc, "title", "description", "location", "owner", "deadline")
                ))

        layout.watchFields.addAll(
            arrayOf(
                "title",
                "description",
                "location",
                "owner",
                "deadline"
            )
        )
        // layout.addAction() // Button mit Rest-Endpunkt add
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    // PostMapping add
}