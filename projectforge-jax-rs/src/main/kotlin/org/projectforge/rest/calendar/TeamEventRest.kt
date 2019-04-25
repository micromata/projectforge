package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Path

@Component
@Path("teamEvent")
class TeamEventRest() : AbstractStandardRest<TeamEventDO, TeamEventDao, TeamEventFilter>(
        TeamEventDao::class.java,
        TeamEventFilter::class.java,
        "plugins.teamcal.event.title") {

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    override protected fun onGetItemAndLayout(request: HttpServletRequest, item: TeamEventDO, editLayoutData: EditLayoutData) {
        val recurrentDateString = request.getParameter("recurrentDate")
        println("TeamEventRest: recurrentDate=$recurrentDateString")
    }


    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "subject"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: TeamEventDO): UILayout {
        val calendars = teamCalDao.getAllCalendarsWithFullAccess()
        val calendarSelectValues = calendars.map { it ->
            UISelectValue<Int>(it.id, it.title)
        }
        val subject = UIInput("subject", lc)
        subject.focus = true
        val layout = super.createEditLayout(dataObject)
        if (dataObject.hasRecurrence()) {
            layout.add(UIFieldset(12, title = "plugins.teamcal.event.recurrence.change.text")
                    .add(UIGroup()
                            .add(UICheckbox("all", label = "plugins.teamcal.event.recurrence.change.text.all"))
                            .add(UICheckbox("future", label = "plugins.teamcal.event.recurrence.change.future"))
                            .add(UICheckbox("single", label = "plugins.teamcal.event.recurrence.change.single"))
                    ))
        }
        layout.add(UIFieldset(12)
                .add(UIRow()
                        .add(UICol(6)
                                .add(UISelect<Int>("calendar", values = calendarSelectValues.toMutableList(), label = "plugins.teamcal.event.teamCal"))
                                .add(subject)
                                .add(lc, "attendees")
                                .add(lc, "location", "note"))
                        .add(UICol(6)
                                .add(lc, "startDate", "endDate", "allDay")
                                .add(UIFieldset(12)
                                        .add(UICustomized("reminder")))
                                .add(UIFieldset(12)
                                        .add(UICustomized("recurrence"))))))

        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
