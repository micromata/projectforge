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


package org.projectforge.rest.birthdaybutler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.birthdaybutler.BirthdayButlerConfiguration
import org.projectforge.birthdaybutler.BirthdayButlerService
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.Month

private val log = KotlinLogging.logger {}

/**
 * The birthday butler of projectforge-next (`/next/birthdayButler`, formerly the dynamic React page
 * `react/birthdayButler/dynamic`): the Word document with the birthdays of the organization's staff in a month
 * ([BirthdayButlerService]).
 *
 * For the ORGA team and the HR group only (as the menu entry, `MenuCreator`). Every endpoint checks this itself:
 * there is no DAO behind this page that could do it, and a hidden menu entry keeps nobody out.
 */
@RestController
@RequestMapping("${Rest.URL}/birthdayButler")
class BirthdayButlerRest {
    /**
     * What the page starts with: the current month as preselection, and whether an organization is configured
     * (`projectforge.birthdaybutler.organization`) — without one there is no list to create.
     */
    class Init(val month: Int, val configured: Boolean)

    /** The month of the list, 1 (January) to 12. */
    class Request(var month: Int? = null)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var birthdayButlerConfiguration: BirthdayButlerConfiguration

    @Autowired
    private lateinit var birthdayButlerService: BirthdayButlerService

    private fun checkAccess() {
        accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.ORGA_TEAM, ProjectForgeGroup.HR_GROUP)
    }

    @AccessChecked("ORGA team or HR group (checkAccess)")
    @GetMapping("init")
    fun getInit(): Init {
        checkAccess()
        return Init(month = LocalDate.now().monthValue, configured = birthdayButlerConfiguration.isConfigured())
    }

    /**
     * The Word document of the month. If there is none to create (no organization configured, nobody of it
     * with a birthday in the month, …), the answer is a 400 with the translated reason as plain text, which the
     * page shows as it is.
     */
    @AccessChecked("ORGA team or HR group (checkAccess)")
    @PostMapping("download")
    fun download(@RequestBody request: Request): ResponseEntity<*> {
        checkAccess()
        val monthValue = request.month
        if (monthValue == null || monthValue !in 1..12) {
            return badRequest("birthdayButler.month.response.nothingSelected")
        }
        val month = Month.of(monthValue)
        val response = birthdayButlerService.createWord(month)
        response.wordDocument?.let { wordDocument ->
            log.info { "Birthday list for month $month downloaded." }
            return RestUtils.downloadFile(birthdayButlerService.createFilename(month), wordDocument)
        }
        return badRequest(response.errorMessage ?: "birthdayButler.wordDocument.error")
    }

    private fun badRequest(i18nKey: String): ResponseEntity<String> {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(translate(i18nKey))
    }
}
