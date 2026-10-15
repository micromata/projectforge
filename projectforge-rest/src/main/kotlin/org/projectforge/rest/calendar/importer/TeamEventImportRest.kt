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

package org.projectforge.rest.calendar.importer

import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.ical.ICalParser
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free ics import into a team calendar (`/next/teamCalImport`), the successor of the Wicket
 * `TeamCalImportPage`. The target calendar comes with the upload (field `teamCalId`) and may be changed
 * afterwards via [target]; only calendars the user may write into are accepted
 * ([TeamCalDao.writableCalendars]), the access to each event is checked again by the DAO on commit.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/teamCalImport")
class TeamEventImportRest : AbstractImportRest<TeamEventImportDTO, TeamEventImportStorage>() {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Autowired
    private lateinit var jobHandler: JobHandler

    override val fileExtensions = arrayOf("ics")

    override val maxFileUploadSizeMB = 10L

    override fun checkRight() {
        accessChecker.checkRestrictedOrDemoUser()
    }

    override fun proceedUpload(inputStream: InputStream, filename: String): TeamEventImportStorage {
        val storage = TeamEventImportStorage()
        storage.filename = filename
        val events = try {
            ICalParser().parse(inputStream)
        } catch (ex: Exception) {
            // ical4j's message names the offending line, which helps the user to find a broken entry.
            throw IllegalArgumentException("${translate("plugins.teamcal.import.ics.error")}: ${ex.message}", ex)
        }
        if (events.isEmpty()) {
            throw IllegalArgumentException(translate("plugins.teamcal.import.ics.noEventsGiven"))
        }
        events.forEach { event ->
            // An event without DTEND already has its end derived from DURATION by the parser (VEventUtils).
            storage.commitEntity(TeamEventImportDTO.read(event))
        }
        log.info { "ics file '$filename' parsed: ${events.size} events." }
        return storage
    }

    override fun proceedUpload(
        inputStream: InputStream,
        filename: String,
        request: HttpServletRequest,
    ): TeamEventImportStorage {
        val storage = proceedUpload(inputStream, filename)
        request.getParameter("teamCalId")?.toLongOrNull()?.let { setTarget(storage, it) }
        return storage
    }

    /**
     * Imports the stashed file into another calendar: re-reconciles against it and answers the [ImportView].
     * A calendar the user may not write into is a `400`.
     */
    @AccessChecked("checkRight() and TeamCalDao.writableCalendars")
    @PostMapping("target")
    fun target(request: HttpServletRequest, @RequestParam("teamCalId") teamCalId: Long): ResponseEntity<*> {
        checkRight()
        val storage = getStorage(request) ?: return ResponseEntity.ok(buildView(null))
        if (!setTarget(storage, teamCalId)) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to translate("plugins.teamcal.import.ics.error.noCalendar")))
        }
        return ResponseEntity.ok(buildView(storage))
    }

    /** Sets and reconciles the target calendar, if the user may write into it. */
    private fun setTarget(storage: TeamEventImportStorage, teamCalId: Long): Boolean {
        val calendar = teamCalDao.writableCalendars.find { it.id == teamCalId } ?: return false
        storage.teamCalId = teamCalId
        storage.teamCalTitle = calendar.title
        storage.reconcileImportStorage()
        return true
    }

    override fun import(
        storage: TeamEventImportStorage,
        selectedEntries: List<ImportPairEntry<TeamEventImportDTO>>,
    ): Int {
        log.info { "Enqueueing import of #${selectedEntries.size} events into calendar #${storage.teamCalId}." }
        return jobHandler.addJob(
            TeamEventImportJob(teamCalDao, teamEventDao, selectedEntries, importStorage = storage)
        ).id
    }

    override fun extraViewMeta(storage: TeamEventImportStorage): Map<String, Any>? {
        val teamCalId = storage.teamCalId ?: return null
        return mapOf("teamCalId" to teamCalId, "teamCalTitle" to (storage.teamCalTitle ?: ""))
    }
}
