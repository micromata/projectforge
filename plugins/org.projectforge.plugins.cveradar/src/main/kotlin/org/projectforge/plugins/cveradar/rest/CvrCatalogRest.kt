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

package org.projectforge.plugins.cveradar.rest

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.cveradar.CveRadarAccess
import org.projectforge.plugins.cveradar.CvrCatalogService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The technology catalogue (INV-28). Everybody logged in reads it and picks from it; changing it is for the security
 * team, which [CvrCatalogService] checks.
 */
@RestController
@RequestMapping("${Rest.URL}/cveradar/catalog")
class CvrCatalogRest {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var service: CvrCatalogService

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    private val user get() = ThreadLocalUserContext.requiredLoggedInUser

    @AccessChecked("Everybody logged in reads the catalogue; it holds no inventory data")
    @GetMapping("tree")
    fun tree(): CatalogTree = service.tree(user)

    @AccessChecked("Everybody logged in picks from the catalogue; it holds no inventory data")
    @GetMapping("autosearch")
    fun autosearch(
        @RequestParam("search", required = false) search: String?,
        @RequestParam("maxResults", required = false, defaultValue = "30") maxResults: Int,
    ): List<CatalogRef> = service.autosearch(search, maxResults)

    @AccessChecked("CvrCatalogService.create: security team")
    @PostMapping("node")
    fun create(@RequestBody request: CatalogNodeRequest): Long = service.create(user, request)

    @AccessChecked("CvrCatalogService.update: security team")
    @PostMapping("node/{id}/update")
    fun update(@PathVariable id: Long, @RequestBody request: CatalogNodeRequest) = service.update(user, id, request)

    @AccessChecked("CvrCatalogService.merge: security team")
    @PostMapping("node/{id}/merge")
    fun merge(@PathVariable id: Long, @RequestBody request: CatalogMergeRequest) = service.merge(user, id, request.targetId)

    @AccessChecked("CvrCatalogService.delete: security team")
    @PostMapping("node/{id}/delete")
    fun delete(@PathVariable id: Long) = service.delete(user, id)

    @ExceptionHandler(AccessException::class)
    fun handleAccessException(ex: AccessException): ResponseEntity<RestError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(RestError(status = HttpStatus.FORBIDDEN.value(), message = translateMsg(ex)))
    }

    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException): ResponseEntity<CveRadarError> {
        return ResponseEntity.badRequest().body(CveRadarError(translateMsg(ex)))
    }
}
