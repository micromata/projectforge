/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryFormatService
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.projectforge.framework.persistence.history.HistoryService
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest.InitialListData
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

/**
 * Under construction.
 */
@RestController
@RequestMapping("${Rest.URL}/historyEntries")
class HistoryEntriesPagesRest {
    class HistoryData(entry: DisplayHistoryEntry? = null) {
        var id: Long? = entry?.id
        var entity: String? = null
        var userComment: String? = entry?.userComment
        var timeAgo: String? = entry?.timeAgo
        var operation: String? = entry?.operation
        var modifiedByUser: String? = entry?.modifiedByUser
        var appendComment: String? = null
    }

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var historyFormatService: HistoryFormatService

    /**
     * Get the current filter from the server, all matching items and the layout of the list page.
     */
    @GetMapping("initialList")
    fun requestInitialList(request: HttpServletRequest): InitialListData? {
        log.debug { "requestInitialList" }
        return null
    }

    @RequestMapping(RestPaths.LIST)
    fun getList(request: HttpServletRequest, @RequestBody filter: MagicFilter): ResultSet<*>? {
        log.debug { "getList" }
        return null
    }

    @GetMapping("{id}")
    fun getItem(@PathVariable("id") id: Long?): ResponseEntity<Any> {
        val item = historyService.findEntryAndEntityById(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(item, HttpStatus.OK)
    }

    /**
     * Use this service for adding new items as well as updating existing items (id isn't null).
     */
    @PutMapping("append")
    fun append(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<HistoryData>
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Upsert")?.let { return it }
        throw UnsupportedOperationException("Not implemented yet.")
    }

    @GetMapping(RestPaths.EDIT)
    fun getItemAndLayout(
        request: HttpServletRequest,
        @RequestParam("id") id: String?,
    ): ResponseEntity<FormLayoutData> {
        val item = historyService.findEntryAndEntityById(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val entity = item.entity ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val historyEntry = item.entry ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val dto = HistoryData(historyFormatService.convert(entity, historyEntry, HistoryLoadContext(item.baseDao)))
        dto.entity = if (entity is DisplayNameCapable) {
            entity.displayName
        } else {
            entity.javaClass.simpleName
        }
        val titleKey = "history.entry"
        val ui = UILayout(titleKey, RestResolver.getRestUrl(this::class.java, withoutPrefix = true))
        ui.userAccess.update = item.writeAccess
        ui.userAccess.history = item.readAccess
        ui.add(UIReadOnlyField("timeAgo", label = "modified"))
        ui.add(UIReadOnlyField("entity", label = "history.userComment"))
        ui.add(UIReadOnlyField("userComment", label = "history.userComment"))
        ui.add(UIReadOnlyField("operation", label = "operation"))
        ui.add(UIReadOnlyField("modifiedByUser", label = "user"))
        ui.add(UITextArea("appendComment", label = "history.userComment"))
        ui.addAction(
            UIButton.createDefaultButton(
                "append", title = "append", responseAction = ResponseAction(
                    RestResolver.getRestUrl(this::class.java, "append"), targetType = TargetType.POST
                )
            )
        )
        LayoutUtils.process(ui)
        // ui.addTranslations("changes", "history.userComment.edit", "tooltip.selectMe")
        val serverData = sessionCsrfService.createServerData(request)
        val result = FormLayoutData(dto, ui, serverData)
        return ResponseEntity(result, HttpStatus.OK)
    }
}
