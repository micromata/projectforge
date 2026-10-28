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

package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.CalendarAccessStatus
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.framework.i18n.translate

/**
 * Information for subscription of ProjectForge's calendars or time sheets in
 * external calendar apps or for downloading.
 */
class CalendarSubscriptionInfo(var headline: String? = null,
                               val accessStatus: CalendarAccessStatus? = null,
                               val securityAdviseHeadline: String? = translate("securityAdvice"),
                               val securityAdvise: String? = translate("calendar.icsExport.securityAdvice")) {
    /**
     * For owners and users with full or read-only access the url with reminders is used as default. Might be used
     * in frontend for a checkbox for switching reminders in export on and off.
     */
    val remindersExportDefaultValue: Boolean = accessStatus != null && remindersStatusList.contains(accessStatus)

    var urlWithoutExportedReminders: String? = null
        set(value) {
            field = value
            qrCodeWithoutExportedReminders = value?.let { BarcodeServicesRest.getQrCodeDataUrl(it) }
        }

    var url: String? = null
        set(value) {
            field = value
            qrCode = value?.let { BarcodeServicesRest.getQrCodeDataUrl(it) }
        }

    /**
     * The QR code of [url] as data url: the url carries the user's token, so it must not be part of an image url
     * (access logs).
     */
    var qrCode: String? = null
        private set

    /**
     * The QR code of [urlWithoutExportedReminders] as data url, see [qrCode].
     */
    var qrCodeWithoutExportedReminders: String? = null
        private set

    fun initUrls(calendarFeedService: CalendarFeedService, teamCalId: Long?) {
        teamCalId ?: return
        url = calendarFeedService.getFullUrl(teamCalId, true)
        urlWithoutExportedReminders = calendarFeedService.getFullUrl(teamCalId, false)
    }

    companion object {
        val remindersStatusList =
            listOf(CalendarAccessStatus.OWNER, CalendarAccessStatus.FULL_ACCESS, CalendarAccessStatus.READONLY_ACCESS)
    }
}
