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

package org.projectforge.rest.orga

import jakarta.annotation.PostConstruct
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookDao
import org.projectforge.business.orga.VisitorbookEntryDO
import org.projectforge.business.orga.VisitorbookService
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ValidationUtils
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Visitorbook
import org.projectforge.rest.dto.VisitorbookEntry
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The visitor book for the hand built pages of projectforge-next (components/features/visitorbook): the visitors
 * through the base endpoints, their visits through `entry/insert|update|delete`, written right away from the
 * dialog of the edit page.
 */
@RestController
@RequestMapping("${Rest.URL}/visitorbook")
class VisitorbookEntityRest : AbstractDTOEntityRest<VisitorbookDO, Visitorbook, VisitorbookDao>(
    VisitorbookDao::class.java,
    "orga.visitorbook.title",
) {
    /**
     * Answer of the visit endpoints: all visits of the visitor, as stored after the change.
     */
    class Entries(val entries: List<VisitorbookEntry>)

    @Autowired
    private lateinit var visitorbookService: VisitorbookService

    @PostConstruct
    private fun postConstruct() {
        JacksonConfiguration.registerAllowedUnknownProperties(Visitorbook::class.java, "visitortypeAsString")
    }

    /**
     * The columns of the last visit come from [VisitorbookService.getVisitorbookInfo], not from a column of the
     * table, and the visitor type is sorted as displayed.
     */
    override val computedSortProperties: Map<String, (VisitorbookDO) -> Comparable<*>?>
        get() = mapOf(
            "visitortype" to { obj -> obj.visitortype?.let { translate(it.i18nKey) } },
            "lastDateOfVisit" to { obj -> visitorbookService.getVisitorbookInfo(obj.id)?.lastDateOfVisit },
            "latestArrived" to { obj -> visitorbookService.getVisitorbookInfo(obj.id)?.latestArrived },
            "latestDeparted" to { obj -> visitorbookService.getVisitorbookInfo(obj.id)?.latestDeparted },
            "numberOfVisits" to { obj -> visitorbookService.getVisitorbookInfo(obj.id)?.numberOfVisits ?: 0 },
        )

    override fun transformForDB(dto: Visitorbook): VisitorbookDO {
        val visitorbookDO = VisitorbookDO()
        dto.copyTo(visitorbookDO)
        return visitorbookDO
    }

    override fun transformFromDB(obj: VisitorbookDO, editMode: Boolean): Visitorbook {
        val visitorbook = Visitorbook()
        visitorbook.copyFrom(obj)
        obj.id?.let { id ->
            visitorbookService.getVisitorbookInfo(id)?.let { info ->
                visitorbook.lastDateOfVisit = info.lastDateOfVisit
                visitorbook.latestArrived = info.latestArrived
                visitorbook.latestDeparted = info.latestDeparted
                visitorbook.numberOfVisits = info.numberOfVisits
            }
            if (editMode) {
                visitorbook.entries = selectEntries(id)
            }
        }
        Employee.restoreDisplayNames(visitorbook.contactPersons)
        return visitorbook
    }

    /**
     * Adds a visit to a visitor.
     */
    @AccessChecked("VisitorbookService: insert access to the visitor (VisitorbookDao), checked before the validation")
    @PostMapping("entry/insert")
    fun insertEntry(@RequestBody postData: PostData<VisitorbookEntry>): ResponseEntity<*> {
        val dto = postData.data
        require(dto.id == null) { "Can't insert VisitorbookEntry entry with given id." }
        val visitorbookId = requireNotNull(dto.visitorbookId) {
            "Can't insert VisitorbookEntry entry without visitorbookId."
        }
        checkWriteAccess(visitorbookId, insert = true)
        val entryDO = dto.cloneAsDO(visitorbookId)
        validate(entryDO)?.let { return it }
        visitorbookService.insert(visitorbookId = visitorbookId, entry = entryDO)
        return entriesResponse(visitorbookId)
    }

    /**
     * Updates a visit. The visitor is the one of the stored visit: a visit is never moved to another visitor.
     */
    @AccessChecked("VisitorbookService: update access to the visitor (VisitorbookDao), checked before the validation")
    @PostMapping("entry/update")
    fun updateEntry(@RequestBody postData: PostData<VisitorbookEntry>): ResponseEntity<*> {
        val dto = postData.data
        val visitorbookId = storedVisitorbookId(dto)
        checkWriteAccess(visitorbookId)
        val entryDO = dto.cloneAsDO(visitorbookId)
        validate(entryDO)?.let { return it }
        visitorbookService.updateVisitorbookEntry(visitorbookId = visitorbookId, entry = entryDO)
        return entriesResponse(visitorbookId)
    }

    /**
     * Marks a visit as deleted (kept for the history).
     */
    @AccessChecked("VisitorbookService: update access to the visitor (VisitorbookDao)")
    @PostMapping("entry/delete")
    fun deleteEntry(@RequestBody postData: PostData<VisitorbookEntry>): ResponseEntity<*> {
        val dto = postData.data
        val visitorbookId = storedVisitorbookId(dto)
        checkWriteAccess(visitorbookId)
        visitorbookService.markVisitorbookEntryAsDeleted(visitorbookId = visitorbookId, entryId = dto.id)
        return entriesResponse(visitorbookId)
    }

    private fun selectEntries(visitorbookId: Long): List<VisitorbookEntry> {
        return visitorbookService.selectAllVisitorbookEntries(visitorbookId, deleted = false)
            .map { VisitorbookEntry(it) }
    }

    private fun entriesResponse(visitorbookId: Long): ResponseEntity<Entries> {
        return ResponseEntity.ok(Entries(selectEntries(visitorbookId)))
    }

    /**
     * The visitor of the stored visit, never the posted one (which would let an update move the visit to another
     * visitor).
     */
    private fun storedVisitorbookId(dto: VisitorbookEntry): Long {
        val id = requireNotNull(dto.id) { "Can't update/delete VisitorbookEntry entry without id." }
        val stored = visitorbookService.findVisitorbookEntry(id) // Checks select access.
            ?: throw IllegalArgumentException("VisitorbookEntry with id $id not found.")
        return requireNotNull(stored.visitorbook?.id) { "VisitorbookEntry with id $id has no visitor." }
    }

    /**
     * The access check of [VisitorbookService] again, but before [validate], so a user without access learns
     * nothing from the validation.
     */
    private fun checkWriteAccess(visitorbookId: Long, insert: Boolean = false) {
        val visitorbook = baseDao.find(visitorbookId)
            ?: throw IllegalArgumentException("Visitorbook with id $visitorbookId not found.")
        if (insert) {
            baseDao.checkLoggedInUserInsertAccess(visitorbook)
        } else {
            baseDao.checkLoggedInUserUpdateAccess(visitorbook, visitorbook)
        }
    }

    private fun validate(entryDO: VisitorbookEntryDO): ResponseEntity<ResponseAction>? {
        // Required fields (the date of the visit) and lengths, from the annotations of the entity.
        val validationErrors = ValidationUtils.validateFields(entryDO)
        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }
        return null
    }
}
