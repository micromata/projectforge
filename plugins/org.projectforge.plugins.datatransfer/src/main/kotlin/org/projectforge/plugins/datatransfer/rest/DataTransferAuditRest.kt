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

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferAuditDO
import org.projectforge.plugins.datatransfer.DataTransferAuditDao
import org.projectforge.plugins.datatransfer.DataTransferUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The activities of a data transfer area (events and downloads), shown by the activities dialog of the
 * projectforge-next file view. Replaces the retired dynamic `DataTransferAuditPageRest` (`UILayout`).
 */
@RestController
@RequestMapping("${Rest.URL}/datatransferaudit")
class DataTransferAuditRest {
    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var dataTransferAuditDao: DataTransferAuditDao

    class AreaAuditData(
        val areaName: String?,
        val events: List<DataTransferAuditDO>,
        val downloadEvents: List<DataTransferAuditDO>,
    )

    /**
     * @param id The id of the area, or [DataTransferUtils.PERSONAL_BOX_ID] for the personal box of the
     * logged-in user.
     */
    @AccessChecked("DAO: select access (find with access check); -1 = own personal box")
    @GetMapping("{id}")
    fun getAudit(@PathVariable("id") id: Long): AreaAuditData {
        val areaId = if (id == DataTransferUtils.PERSONAL_BOX_ID) {
            dataTransferAreaDao.ensurePersonalBox(ThreadLocalUserContext.loggedInUserId!!)?.id
                ?: throw IllegalStateException("No personal box found for the logged-in user.")
        } else {
            id
        }
        val areaDO = dataTransferAreaDao.find(areaId)!!
        val area = DataTransferArea()
        area.copyFrom(areaDO)
        val events = dataTransferAuditDao.getEntriesByAreaId(areaId) ?: emptyList()
        return AreaAuditData(
            areaName = area.areaName,
            events = events.filter { !DataTransferAuditDao.downloadEventTypes.contains(it.eventType) },
            downloadEvents = events.filter { DataTransferAuditDao.downloadEventTypes.contains(it.eventType) },
        )
    }
}
