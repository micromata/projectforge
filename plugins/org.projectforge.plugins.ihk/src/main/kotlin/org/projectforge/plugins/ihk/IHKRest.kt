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

package org.projectforge.plugins.ihk

import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.admin.LogViewerRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.PagesResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * The IHK training report page of projectforge-next (/next/ihk): the apprentice's settings, the time sheets of
 * a week without description and the xlsx download. Everything works on the logged-in user's own data only.
 */
@RestController
@RequestMapping("${Rest.URL}/ihk")
class IHKRest {
    @Autowired
    private lateinit var ihkService: IHKService

    @Autowired
    private lateinit var kostCache: KostCache

    class SettingsDTO(val ausbildungsbeginn: String, val ausbildungsjahr: Int, val teamname: String?)

    class SettingsErrorDTO(
        /** notFound, empty or parsing. */
        val reason: String,
        /** The parser message, only for parsing. */
        val detail: String?,
    )

    class InitData(
        /** The user's names, which the address must match exactly. */
        val firstname: String?,
        val lastname: String?,
        val settings: SettingsDTO?,
        val settingsError: SettingsErrorDTO?,
        /** The edit page of the user's address, or the new-address page if none was found. */
        val addressUrl: String,
        val logViewerUrl: String?,
    )

    class MissingDescription(val id: Long?, val label: String, val startTime: String?, val stopTime: String?)

    /** [monday] may be any day of the week, the report covers its Monday to Sunday. */
    class ExportRequest(val monday: LocalDate? = null)

    @AccessChecked("Own user only (logged-in user's address and log subscription)")
    @GetMapping("init")
    fun init(): InitData {
        val result = ihkService.loadSettings()
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return InitData(
            firstname = user.firstname,
            lastname = user.lastname,
            settings = result.settings?.let {
                SettingsDTO(it.ausbildungsbeginn.toString(), it.ausbildungsjahr, it.teamname)
            },
            settingsError = result.error?.let {
                SettingsErrorDTO(
                    reason = when (it.reason) {
                        IHKService.SettingsErrorReason.NOT_FOUND -> "notFound"
                        IHKService.SettingsErrorReason.EMPTY -> "empty"
                        IHKService.SettingsErrorReason.PARSING -> "parsing"
                    },
                    detail = it.detail,
                )
            },
            addressUrl = PagesResolver.getEditPageUrl(AddressPagesRest::class.java, result.addressId),
            logViewerUrl = IHKPlugin.ensureUserLogSubscription()?.let { LogViewerRest.viewerUrl(it.id) },
        )
    }

    /**
     * The logged-in user's time sheets of the week (Monday to Sunday) of [monday] that have no description.
     */
    @AccessChecked("Own user only (time sheets of the logged-in user)")
    @GetMapping("missingDescriptions")
    fun missingDescriptions(@RequestParam("monday") monday: LocalDate): List<MissingDescription> {
        return ihkService.findTimesheets(monday).filter { it.description == null }.map { ts ->
            val kost2 = kostCache.getKost2(ts.kost2?.id)
            MissingDescription(
                id = ts.id,
                label = kost2?.description ?: kost2?.displayName ?: translate("plugins.ihk.nodescriptionfound"),
                startTime = ts.startTime?.toInstant()?.toString(),
                stopTime = ts.stopTime?.toInstant()?.toString(),
            )
        }
    }

    /**
     * The training report of the week of [ExportRequest.monday] as xlsx download. A missing setup or an
     * empty week is answered with 400 and a plain-text message (a UserException would come back as HTTP 200 and be
     * saved as the file).
     */
    @AccessChecked("Own user only (time sheets of the logged-in user)")
    @PostMapping("export")
    fun export(@RequestBody request: ExportRequest): ResponseEntity<*> {
        val monday = request.monday ?: return RestUtils.badRequest("monday missing")
        val settings = ihkService.loadSettings().settings
            ?: return RestUtils.badRequest(translate("plugins.ihk.setup.error"))
        val timesheets = ihkService.findTimesheets(monday)
        if (timesheets.isEmpty()) {
            return RestUtils.badRequest(translate("plugins.ihk.noitemsfound"))
        }
        val report = ihkService.export(settings, monday, timesheets)
            ?: return ResponseEntity.internalServerError().body(translate("plugins.ihk.exportError"))
        return RestUtils.downloadFile(report.filename, report.content)
    }
}
