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
import java.time.LocalDate
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookDao
import org.projectforge.business.orga.VisitorbookEntryDO
import org.projectforge.business.orga.VisitorbookService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
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
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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

    /**
     * A stored visitor matching the name typed into a new one, with its latest visit, offered so the visit is added
     * to it rather than to a duplicate.
     */
    class ExistingVisitor(
        val id: Long?,
        val firstname: String?,
        val lastname: String?,
        val company: String?,
        val lastDateOfVisit: LocalDate?,
        val numberOfVisits: Int,
    )

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

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // Pinned: the visits still to be completed, e.g. the departure of a visitor not entered yet.
        elements.add(
            UIFilterBooleanElement(
                INCOMPLETE_VISITS_FILTER,
                label = translate("orga.visitorbook.filter.incompleteVisits"),
                defaultFilter = true,
            )
        )
    }

    /**
     * The visitors with at least one visit without arrival or departure time ([VisitorbookInfo.incompleteVisits]),
     * if [INCOMPLETE_VISITS_FILTER] is ticked.
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<VisitorbookDO>>? {
        val entry = source.entries.find { it.field == INCOMPLETE_VISITS_FILTER } ?: return null
        entry.synthetic = true
        if (entry.value.value != "true") {
            return null
        }
        return listOf(object : CustomResultFilter<VisitorbookDO> {
            override fun match(list: MutableList<VisitorbookDO>, element: VisitorbookDO): Boolean {
                return (visitorbookService.getVisitorbookInfo(element.id)?.incompleteVisits ?: 0) > 0
            }
        })
    }

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
     * The stored visitors whose first and last name start with the given ones (case-insensitive, a blank one matches
     * all), the latest visit first. Nothing before [MIN_SEARCH_CHARS] characters were typed.
     */
    @AccessChecked("DAO: select access (VisitorbookDao.select with checkAccess)")
    @GetMapping("existing")
    fun getExisting(
        @RequestParam("firstname", required = false) firstname: String?,
        @RequestParam("lastname", required = false) lastname: String?,
    ): List<ExistingVisitor> {
        val first = firstname?.trim().orEmpty()
        val last = lastname?.trim().orEmpty()
        if (first.length + last.length < MIN_SEARCH_CHARS) {
            return emptyList()
        }
        val queryFilter = QueryFilter(BaseSearchFilter().also { it.deleted = false })
        if (first.isNotEmpty()) {
            queryFilter.add(QueryFilter.like("firstname", "$first*"))
        }
        if (last.isNotEmpty()) {
            queryFilter.add(QueryFilter.like("lastname", "$last*"))
        }
        // Without a sort, VisitorbookDao.select sorts by the latest visit.
        return baseDao.select(queryFilter).take(MAX_EXISTING).map { obj ->
            val info = visitorbookService.getVisitorbookInfo(obj.id)
            ExistingVisitor(
                id = obj.id,
                firstname = obj.firstname,
                lastname = obj.lastname,
                company = obj.company,
                lastDateOfVisit = info?.lastDateOfVisit,
                numberOfVisits = info?.numberOfVisits ?: 0,
            )
        }
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

    companion object {
        /** Id of the filter of the visitors with incomplete visits, see [preProcessMagicFilter]. */
        const val INCOMPLETE_VISITS_FILTER = "incompleteVisits"
        private const val MIN_SEARCH_CHARS = 2
        private const val MAX_EXISTING = 10
    }
}
