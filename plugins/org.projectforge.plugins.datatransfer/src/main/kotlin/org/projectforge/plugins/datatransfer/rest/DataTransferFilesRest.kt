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

package org.projectforge.plugins.datatransfer.rest

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.group.service.GroupService
import org.projectforge.common.NumberOfBytes
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferUtils
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The file view of a data transfer area (attachments list including upload/download, info and the observe
 * toggle) for everybody with access to the area, serving the hand-built projectforge-next page
 * `datatransfer/{id}`. Replaces the retired dynamic `DataTransferPageRest` (`UILayout`). The attachments
 * themselves are read and written by [AttachmentsServicesRest] (category `datatransfer`).
 */
@RestController
@RequestMapping("${Rest.URL}/datatransferfiles")
class DataTransferFilesRest {
    /**
     * The file view: the area (attachments with expiry info) and what the logged-in user may do with it.
     */
    class View(
        val area: DataTransferArea,
        /** The admin form is offered (no personal box, update access). */
        val editAccess: Boolean,
        /** "Download all" is offered: there are files, and not more than 1 GB of them. */
        val downloadAllAvailable: Boolean,
        /** The maximum size of an uploaded file in KB. */
        val maxUploadSizeKB: Int,
    )

    class ObserveRequest(var observe: Boolean? = null)

    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var attachmentsServicesRest: AttachmentsServicesRest

    @Autowired
    private lateinit var dataTransferAreaEntityRest: DataTransferAreaEntityRest

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var groupService: GroupService

    @PostConstruct
    private fun postConstruct() {
        attachmentsServicesRest.register(
            dataTransferAreaEntityRest.category,
            DataTransferAttachmentsActionListener(attachmentsService, dataTransferAreaDao)
        )
    }

    @AccessChecked("DAO: select access (find with access check)")
    @GetMapping("downloadAll/{id}")
    fun downloadAll(
        @PathVariable("id", required = true) id: Long,
        response: HttpServletResponse
    ) {
        val pair = convertData(id)
        val dbObj = pair.first
        val dto = pair.second
        DataTransferRestUtils.multiDownload(
            response,
            attachmentsService,
            dataTransferAreaEntityRest.attachmentsAccessChecker,
            dbObj,
            dto.areaName,
            jcrPath = dataTransferAreaEntityRest.jcrPath!!,
            id,
            dto.attachments,
            byUser = ThreadLocalUserContext.loggedInUser
        )
    }

    /**
     * @param id The id of the area, or [DataTransferUtils.PERSONAL_BOX_ID] for the personal box of the
     * logged-in user (the client then replaces it by the real id of [View.area]).
     */
    @AccessChecked("DAO: select access (find with access check); -1 = own personal box")
    @GetMapping("{id}")
    fun getView(@PathVariable("id") id: Long): View {
        val areaId = if (id == DataTransferUtils.PERSONAL_BOX_ID) {
            dataTransferAreaDao.ensurePersonalBox(ThreadLocalUserContext.loggedInUserId!!)?.id
                ?: throw IllegalStateException("No personal box found for the logged-in user.")
        } else {
            id
        }
        val (dbObj, dto) = convertData(areaId)
        return View(
            area = dto,
            editAccess = hasEditAccess(dto, dbObj),
            downloadAllAvailable = (dto.attachmentsSize ?: 0) in 1..NumberOfBytes.GIGA_BYTES,
            maxUploadSizeKB = DataTransferAreaDao.getMaxUploadFileSizeKB(dbObj),
        )
    }

    /**
     * Adds the logged-in user to the observers of the area or removes them. Read access is sufficient: the
     * user doesn't have to be an admin to observe an area.
     */
    @AccessChecked("DAO: select access (find with access check), update of the own observer entry only")
    @PostMapping("observe/{id}")
    fun observe(@PathVariable("id") id: Long, @RequestBody request: ObserveRequest): View {
        val observe = request.observe ?: return getView(id)
        val loggedInUser = ThreadLocalUserContext.loggedInUser!!
        val dbDto = convertData(id).second // Checks the read access.
        if (observe != isLoggedInUserObserver(dbDto, loggedInUser)) {
            // Get entry including external access settings (see DataTransferDao#hasAccess).
            val dbObj = dataTransferAreaDao.find(id, checkAccess = false)!!
            val newObservers = dbDto.observers?.toMutableList() ?: mutableListOf()
            if (observe) {
                val user = User()
                user.copyFrom(loggedInUser)
                newObservers.add(user)
            } else {
                newObservers.removeIf { it.id == loggedInUser.id }
            }
            dbObj.observerIds = User.toLongList(newObservers)
            // Without access check, because the user must not be admin to observe this area. Read access is
            // given, because the area was already gotten by the user in [convertData].
            dataTransferAreaDao.update(dbObj, checkAccess = false)
        }
        return getView(id)
    }

    private fun isLoggedInUserObserver(
        dto: DataTransferArea,
        user: PFUserDO = ThreadLocalUserContext.loggedInUser!!
    ): Boolean {
        return dto.observers?.any { it.id == user.id } ?: false
    }

    /**
     * @return true, if the area isn't a personal box and the user has write access.
     */
    private fun hasEditAccess(dto: DataTransferArea, dbObj: DataTransferAreaDO): Boolean {
        return dto.personalBox != true && dataTransferAreaDao.hasLoggedInUserUpdateAccess(dbObj, dbObj, false)
    }

    private fun convertData(id: Long): Pair<DataTransferAreaDO, DataTransferArea> {
        val dbObj = dataTransferAreaDao.find(id)!!
        val dto = DataTransferArea.transformFromDB(dbObj, dataTransferAreaDao)
        if (hasEditAccess(dto, dbObj)) {
            dto.externalPassword = dbObj.externalPassword
        }
        dto.attachments = attachmentsService.getAttachments(
            dataTransferAreaEntityRest.jcrPath!!,
            id,
            dataTransferAreaEntityRest.attachmentsAccessChecker
        )
        dto.internalLink = domainService.getDomain(DataTransferUtils.areaViewPath(id))
        if (!dbObj.accessGroupIds.isNullOrBlank()) {
            // Add all users assigned to the access groups:
            val accessGroupUsers =
                groupService.getGroupUsers(User.toLongArray(dbObj.accessGroupIds)).joinToString { it.displayName }
            dto.accessGroupsAsString += ": $accessGroupUsers"
        }
        dto.userWantsToObserve = isLoggedInUserObserver(dto)
        return Pair(dbObj, dto)
    }
}
