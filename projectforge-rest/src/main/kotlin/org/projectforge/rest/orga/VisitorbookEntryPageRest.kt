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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.orga.VisitorbookEntryDO
import org.projectforge.business.orga.VisitorbookService
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.VisitorbookEntry
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * Dialog for registering a new token or modifying/deleting an existing one.
 */
@RestController
@RequestMapping("${Rest.URL}/visitorbookEntry")
class VisitorbookEntryPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var visitorbookService: VisitorbookService

    @Autowired
    private lateinit var visitorbookPagesRest: VisitorbookPagesRest

    class ResponseData(
        var entries: List<VisitorbookEntry>? = null,
    )

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("id") idString: String?,
        @RequestParam("visitorbookId") visitorbookId: Long?,
    ): FormLayoutData {
        val id = idString?.toLongOrNull() ?: -1
        requiredFields(id, visitorbookId)
        val data = if (id > 0) {
            VisitorbookEntry(visitorbookService.findVisitorbookEntry(id))
        } else {
            VisitorbookEntry(visitorbookId = visitorbookId).also {
                it.dateOfVisit = LocalDate.now()
                it.arrived = PFDateTime.now().localTimeString
            }
        }
        val lc = LayoutContext(VisitorbookEntryDO::class.java)
        val layout = UILayout("orga.visitorbook.timeofvisit")
        layout.add(lc, "dateOfVisit", "arrived", "departed", "comment")
        layout.addAction(UIButton.createCancelButton(responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL)))
        if (id < 0) {
            // New entry
            layout.addAction(
                UIButton.createAddButton(
                    responseAction = ResponseAction(
                        url = RestResolver.getRestUrl(this::class.java, "insert"),
                        targetType = TargetType.POST,
                    )
                )
            )
        } else {
            layout.addAction(
                UIButton.createDeleteButton(
                    layout,
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "delete"),
                        targetType = TargetType.POST,
                    ),
                )
            )
            layout.addAction(
                UIButton.createUpdateButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "update"),
                        targetType = TargetType.POST
                    ),
                )
            )
        }

        layout.addTranslations("cancel", "yes")
        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

    @PostMapping("delete")
    fun delete(@RequestBody postData: PostData<VisitorbookEntry>): ResponseEntity<*> {
        requiredFields(postData.data)
        val dto = postData.data
        visitorbookService.markVisitorbookEntryAsDeleted(visitorbookId = dto.visitorbookId, entryId = dto.id)
        return closeModal(dto)
    }

    /**
     * Updates only the displayName.
     */
    @PostMapping("update")
    fun update(@RequestBody postData: PostData<VisitorbookEntry>): ResponseEntity<ResponseAction> {
        requiredFields(postData.data)
        val dto = postData.data
        validate(dto)?.let { return it }
        val entry = dto.cloneAsDO()
        visitorbookService.updateVisitorbookEntry(visitorbookId = dto.visitorbookId, entry = entry)
        return closeModal(dto)
    }

    /**
     * Updates only the displayName.
     */
    @PostMapping("insert")
    fun insert(@RequestBody postData: PostData<VisitorbookEntry>): ResponseEntity<ResponseAction> {
        requiredFields(postData.data, idNull = true)
        validate(postData.data)?.let { return it }
        val dto = postData.data
        val entry = dto.cloneAsDO()
        visitorbookService.insert(visitorbookId = dto.visitorbookId!!, entry = entry)
        return visitorbookPagesRest.getListPageResponseEntity(
            absolute = true,
            forceAGGridReload = true,
            highlightedObjectId = dto.visitorbookId,
        )
    }

    private fun closeModal(dto: VisitorbookEntry): ResponseEntity<ResponseAction> {
        val responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
        val entries = visitorbookService.selectAllVisitorbookEntries(dto.visitorbookId!!, deleted = false)
            .map { VisitorbookEntry(it) }
        responseAction.addVariable("data", ResponseData(entries = entries))
        return ResponseEntity.ok(responseAction)
    }

    private fun requiredFields(dto: VisitorbookEntry, idNull: Boolean = false) {
        requiredFields(dto.id, dto.visitorbookId, idNull)
    }

    private fun requiredFields(
        id: Long?,
        visitorbookId: Long?,
        idNull: Boolean = false,
    ) {
        if (idNull) {
            require(id == null) { "Can't insert VisitorbookEntry entry with given id." }
        } else {
            requireNotNull(id) { "Can't update/delete VisitorbookEntry entry without id." }
        }
        requireNotNull(visitorbookId) { "Can't update VisitorbookEntry entry without visitorbookId." }
    }

    private fun validate(dto: VisitorbookEntry): ResponseEntity<ResponseAction>? {
        val validationErrors = mutableListOf<ValidationError>()
        if (dto.dateOfVisit == null) {
            validationErrors.add(
                ValidationError.createFieldRequired(
                    VisitorbookEntryDO::class.java,
                    fieldId = "dateOfVisit",
                )
            )
        }
        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(
                ResponseAction(validationErrors = validationErrors),
                HttpStatus.NOT_ACCEPTABLE
            )
        }
        return null
    }
}
