/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/calendarSubscription")
class CalendarSubscriptionInfoPageRest {

    @Autowired
    private lateinit var calendarFeedService: CalendarFeedService

    @GetMapping("layout")
    fun getLayout(@RequestParam("type") type: String?): UILayout {
        val subscriptionInfo = CalendarSubscriptionInfo()
        if (type == "HOLIDAYS") {
            subscriptionInfo.url = calendarFeedService.fullUrl4Holidays
            subscriptionInfo.headline = translate("holidays")
        } else if (type == "WEEK_OF_YEAR") {
            subscriptionInfo.url = calendarFeedService.fullUrl4WeekOfYears
            subscriptionInfo.headline = translate("weekOfYear")
        } else {
            val timesheetUserId = ThreadLocalUserContext.getUserId()
            subscriptionInfo.url = calendarFeedService.getFullUrl4Timesheets(timesheetUserId)
            subscriptionInfo.headline = translate("timesheet.timesheets")
        }
        val layout = UILayout("plugins.teamcal.subscription")
        layout.addTranslations("username", "password", "login.stayLoggedIn", "login.stayLoggedIn.tooltip")
        layout.add(UIFieldset(mdLength = 12, lgLength = 12)
                .add(UIRow()
                        .add(UICol()
                                .add(UICustomized("calendar.subscriptionInfo",
                                        values = mutableMapOf("subscriptionInfo" to subscriptionInfo))))))
        return layout
    }

    companion object {
        fun getTimesheetUserUrl(): String {
            return PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java, mapOf("type" to "TIMESHEETS"))
        }

        fun getHolidaysUrl(): String {
            return PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java, mapOf("type" to "HOLIDAYS"))
        }

        fun getWeekOfYearUrl(): String {
            return PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java, mapOf("type" to "WEEK_OF_YEAR"))
        }
    }
}
