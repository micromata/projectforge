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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.framework.persistence.history.HistoryService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Appends user comments to history entries (history tab of the next edit pages).
 */
@RestController
@RequestMapping("${Rest.URL}/historyEntries")
class HistoryEntryUserCommentRest {
    class HistoryData {
        var id: Long? = null
        var appendComment: String? = null
    }

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Autowired
    private lateinit var historyService: HistoryService

    /**
     * Appends [HistoryData.appendComment] to the comment of the history entry [HistoryData.id].
     */
    @AccessChecked("DAO: history and select access (HistoryService.findEntryAndEntityById with checkAccess)")
    @PutMapping("append")
    fun append(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<HistoryData>,
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Upsert")?.let { return it }
        val dto = postData.data
        if (dto.appendComment.isNullOrBlank()) {
            return ResponseEntity.ok().body(ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true))
        }
        val id = dto.id ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        historyService.appendUserComment(id, dto.appendComment)
        return ResponseEntity.ok().body(ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true))
    }
}
