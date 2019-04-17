package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Component
@Path("teamEvent")
class TeamEventRest() : AbstractStandardRest<TeamEventDO, TeamEventDao, TeamEventFilter>(
        TeamEventDao::class.java,
        TeamEventFilter::class.java,
        "plugins.teamcal.title") {

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * a group with a separate label and input field will be generated.
     * layout will be also included if the id is not given.
     */
    @GET
    @Path("eventEdit")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemAndLayout(@Context request: HttpServletRequest,
                                  @QueryParam("id") id: Int?,
                                  @QueryParam("recurrentDate") recurrentDate : Long?): Response {
        val item = getById(id)
        if (item == null)
            return restHelper.buildResponseItemNotFound()
        val layout = createEditLayout(item)
        layout.addTranslations("changes")
        layout.postProcessPageMenu()
        val result = EditLayoutData(item, layout)
        return restHelper.buildResponse(result)
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
    override fun createEditLayout(dataObject: TeamEventDO?): UILayout {
        val calendars = teamCalDao.getAllCalendarsWithFullAccess()
        val calendarSelectValues = calendars.map { it ->
            UISelectValue<Int>(it.id, it.title)
        }
        val subject = UIInput("subject", lc)
        subject.focus = true
        val layout = super.createEditLayout(dataObject)
                .add(UISelect<Int>("calendar", values = calendarSelectValues.toMutableList()))
                .add(subject)
                .add(lc, "location", "note", "startDate", "endDate", "allDay")

        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
