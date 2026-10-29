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
import org.projectforge.plugins.cveradar.CvrInventoryService
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
import org.springframework.web.bind.annotation.RestController

/**
 * The inventory (areas, systems, instances, components). Everybody logged in may use it: what a user sees and changes
 * is decided by [CvrInventoryService] from the groups along the tree.
 */
@RestController
@RequestMapping("${Rest.URL}/cveradar/inventory")
class CvrInventoryRest {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var service: CvrInventoryService

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    private val user get() = ThreadLocalUserContext.requiredLoggedInUser

    @AccessChecked("CvrInventoryService.tree: the nodes of the user's groups, internal systems for the security team")
    @GetMapping("tree")
    fun tree(): InventoryTree = service.tree(user)

    @AccessChecked("CvrInventoryService.createArea: a group of the creator for a root, write access to the parent below")
    @PostMapping("area")
    fun createArea(@RequestBody request: AreaRequest): Long = service.createArea(user, request)

    @AccessChecked("CvrInventoryService.updateArea: write access to the stored area")
    @PostMapping("area/{id}/update")
    fun updateArea(@PathVariable id: Long, @RequestBody request: AreaRequest) = service.updateArea(user, id, request)

    @AccessChecked("CvrInventoryService.createSystem: write access to the stored area")
    @PostMapping("system")
    fun createSystem(@RequestBody request: SystemRequest): Long = service.createSystem(user, request)

    @AccessChecked("CvrInventoryService.updateSystem: write access to the stored system")
    @PostMapping("system/{id}/update")
    fun updateSystem(@PathVariable id: Long, @RequestBody request: SystemRequest) =
        service.updateSystem(user, id, request)

    @AccessChecked("CvrInventoryService.createInstance: write access to the stored system")
    @PostMapping("instance")
    fun createInstance(@RequestBody request: InstanceRequest): Long = service.createInstance(user, request)

    @AccessChecked("CvrInventoryService.updateInstance: write access to the stored system")
    @PostMapping("instance/{id}/update")
    fun updateInstance(@PathVariable id: Long, @RequestBody request: InstanceRequest) =
        service.updateInstance(user, id, request)

    @AccessChecked("CvrInventoryService.createComponent: write access to the stored system")
    @PostMapping("component")
    fun createComponent(@RequestBody request: ComponentRequest): Long = service.createComponent(user, request)

    @AccessChecked("CvrInventoryService.updateComponent: write access to the stored system")
    @PostMapping("component/{id}/update")
    fun updateComponent(@PathVariable id: Long, @RequestBody request: ComponentRequest) =
        service.updateComponent(user, id, request)

    @AccessChecked("CvrInventoryService.previewLibraries: write access to the stored component's system")
    @PostMapping("component/{id}/libraries/preview")
    fun previewLibraries(@PathVariable id: Long, @RequestBody request: LibraryRequest): LibraryDiff =
        service.previewLibraries(user, id, request)

    @AccessChecked("CvrInventoryService.applyLibraries: write access to the stored component's system")
    @PostMapping("component/{id}/libraries/apply")
    fun applyLibraries(@PathVariable id: Long, @RequestBody request: LibraryRequest): LibraryDiff =
        service.applyLibraries(user, id, request)

    @AccessChecked("CvrInventoryService.move: write access to the node and the target, the officer anywhere")
    @PostMapping("{level}/{id}/move")
    fun move(@PathVariable level: InventoryLevel, @PathVariable id: Long, @RequestBody request: MoveRequest) =
        service.move(user, level, id, request.targetAreaId)

    @AccessChecked("CvrInventoryService.delete: write access to the stored node, empty nodes only")
    @PostMapping("{level}/{id}/delete")
    fun delete(@PathVariable level: InventoryLevel, @PathVariable id: Long) = service.delete(user, level, id)

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
