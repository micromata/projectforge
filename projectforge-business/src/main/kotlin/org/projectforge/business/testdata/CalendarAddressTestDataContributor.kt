/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////


package org.projectforge.business.testdata

import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.FormOfAddress
import org.projectforge.business.calendar.CalendarFilter
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.user.UserPrefCacheDataKey
import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.Favorites
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Date

/**
 * The team calendar with appointments of this and the next week (and the leave days of the team), a calendar of
 * the administrator, and the contacts at the customers. The administrator's calendar filter shows both calendars and
 * the own time sheets, so the calendar isn't empty after the first login.
 */
@Service
open class CalendarAddressTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    override val order = 60

    private class Event(val dayOffset: Long, val from: String, val to: String, val subject: String, val location: String)

    private class Contact(
        val form: FormOfAddress,
        val firstName: String,
        val name: String,
        val position: String,
        val organization: String,
        val city: String,
        val zipCode: String,
    )

    override fun seed(ctx: TestDataContext) {
        val laura = ctx.user("laura")
        val admin = ctx.user("admin")
        val calendar = TeamCalDO().also {
            it.title = "Delivery team"
            it.owner = laura
            it.description = "Workshops, reviews and customer appointments"
            it.fullAccessGroupIds = ctx.groups[UserTestDataContributor.ORGANIZATION]?.id?.toString()
            it.fullAccessUserIds = admin.id.toString()
            it.includeLeaveDaysForUsers = UserTestDataContributor.EMPLOYEES.map { (username, _) -> ctx.user(username).id }
                .joinToString(",")
        }
        teamCalDao.insert(calendar, checkAccess = false)
        insertEvents(ctx, calendar, laura, EVENTS)
        val adminCalendar = TeamCalDO().also {
            it.title = "Administration"
            it.owner = admin
            it.description = "Maintenance windows and operations"
        }
        teamCalDao.insert(adminCalendar, checkAccess = false)
        insertEvents(ctx, adminCalendar, admin, ADMIN_EVENTS)
        val filter = CalendarFilter(defaultCalendarId = adminCalendar.id, timesheetUserId = admin.id).also {
            it.addCalendarId(adminCalendar.id!!)
            it.addCalendarId(calendar.id!!)
        }
        // Same key as CalendarFilterServicesRest.getCurrentFilter (area "calendar"), written directly, because the
        // administrator isn't logged in.
        userPrefDao.insertOrUpdate(
            admin.id!!, UserPrefCacheDataKey("calendar", Favorites.PREF_NAME_CURRENT), filter, checkAccess = false,
        )
        CONTACTS.forEach { c ->
            val address = AddressDO().also {
                it.form = c.form
                it.firstName = c.firstName
                it.name = c.name
                it.positionText = c.position
                it.organization = c.organization
                it.city = c.city
                it.zipCode = c.zipCode
                it.country = "Germany"
                it.email = "${c.firstName.lowercase()}.${c.name.lowercase()}@example.org"
                it.businessPhone = "+49 561 ${100000 + ctx.random.nextInt(900000)}"
            }
            addressDao.insert(address, checkAccess = false)
        }
    }

    /** The events relative to the Monday of this week. */
    private fun insertEvents(ctx: TestDataContext, calendar: TeamCalDO, creator: PFUserDO, events: List<Event>) {
        val monday = ctx.today.with(DayOfWeek.MONDAY)
        events.forEach { e ->
            val day = monday.plusDays(e.dayOffset)
            val event = TeamEventDO().also {
                it.calendar = calendar
                it.subject = e.subject
                it.location = e.location
                it.creator = creator
                it.startDate = Date.from(day.atTime(LocalTime.parse(e.from)).atZone(ctx.zone).toInstant())
                it.endDate = Date.from(day.atTime(LocalTime.parse(e.to)).atZone(ctx.zone).toInstant())
            }
            teamEventDao.insert(event, checkAccess = false)
        }
    }

    companion object {
        private val EVENTS = listOf(
            Event(0, "09:00", "09:30", "Weekly planning", "Office"),
            Event(1, "10:00", "12:00", "Claims Portal: sprint review", "Alpine Insurance, Innsbruck"),
            Event(1, "14:00", "15:00", "1:1 Alex", "Office"),
            Event(2, "09:30", "11:00", "Shop Relaunch: design walkthrough", "Remote"),
            Event(2, "13:00", "14:30", "Steering committee Fleet Tracking", "Bluefin Logistics, Hamburg"),
            Event(3, "10:00", "16:00", "Smart Meter workshop", "Greenfield Energy, Freiburg"),
            Event(4, "11:00", "12:00", "Forecast review with finance", "Office"),
            Event(4, "16:00", "17:30", "Team retrospective", "Office"),
            Event(7, "09:00", "09:30", "Weekly planning", "Office"),
            Event(8, "13:00", "15:00", "Patient Scheduling: acceptance test", "Harbor Medical"),
            Event(9, "10:00", "11:30", "Customer App: kick-off", "Alpine Insurance, Innsbruck"),
            Event(11, "15:00", "16:00", "Release planning Fleet Tracking", "Remote"),
        )

        private val ADMIN_EVENTS = listOf(
            Event(0, "07:00", "08:00", "Maintenance window: database update", "Remote"),
            Event(2, "16:00", "17:00", "Access review with the team leads", "Office"),
            Event(3, "08:00", "08:30", "Backup restore test", "Remote"),
            Event(8, "07:00", "08:30", "Security updates rollout", "Remote"),
            Event(10, "14:00", "15:00", "Onboarding of new employees", "Office"),
        )

        private val CONTACTS = listOf(
            Contact(FormOfAddress.MISS, "Sabine", "Hartmann", "CIO", "Bluefin Logistics GmbH", "Hamburg", "20457"),
            Contact(FormOfAddress.MISTER, "Thomas", "Gruber", "Head of Claims", "Alpine Insurance AG", "Innsbruck", "6020"),
            Contact(FormOfAddress.MISS, "Lena", "Moser", "Product Owner", "Alpine Insurance AG", "Innsbruck", "6020"),
            Contact(FormOfAddress.MISTER, "Daniel", "Weber", "Head of IT", "Greenfield Energy GmbH", "Freiburg", "79098"),
            Contact(FormOfAddress.MISS, "Emily", "Carter", "Project Manager", "Harbor Medical Systems", "Bremen", "28195"),
            Contact(FormOfAddress.MISTER, "Jonas", "Keller", "E-Commerce Lead", "Lumen Retail Group", "Cologne", "50667"),
            Contact(FormOfAddress.MISS, "Clara", "Neumann", "Purchasing", "Lumen Retail Group", "Cologne", "50667"),
            Contact(FormOfAddress.MISTER, "Peter", "Brandt", "Account Manager", "Nimbus Cloud Services", "Frankfurt", "60311"),
        )
    }
}
