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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.business.admin.SystemsStatisticsBuilderInterface
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.web.WicketSupport

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class DataTransferStatisticsBuilder() : SystemsStatisticsBuilderInterface {
    override fun addStatisticsEntries(stats: SystemStatisticsData) {
        if (!WicketSupport.getAccessChecker().isUserMemberOfAdminGroup(ThreadLocalUserContext.loggedInUser)) {
            // Do nothing for non-admins.
            return
        }
        val dataTransferAreaDao = WicketSupport.get(DataTransferAreaDao::class.java)
        val list = dataTransferAreaDao.selectAll(checkAccess = false)
        list.sortedByDescending { it.attachmentsSize }.forEachIndexed { index, dbo ->
            if (dbo.attachmentsCounter ?: 0 == 0 || index >= 30) {
                return
            }
            val size = AttachmentsInfo.getAttachmentsSizeFormatted(dbo.attachmentsCounter, dbo.attachmentsSize)

            val admins = User.toUserNames(dbo.adminIds)
            val accessUsers = User.toUserNames(dbo.accessUserIds)
            val accessUserString = if (accessUsers.isBlank()) "" else ", access users=[$accessUsers]"
            val accessGroups = Group.toGroupNames(dbo.accessGroupIds)
            val accessGroupString = if (accessGroups.isBlank()) "" else ", access groups=[$accessGroups]"
            val externalAccess = if (dbo.externalDownloadEnabled == true || dbo.externalUploadEnabled == true) {
                ", external access=[download=${dbo.externalDownloadEnabled == true}, upload=${dbo.externalUploadEnabled == true}]"
            } else {
                ""
            }
            val expiryDays = ", expiry=[${dbo.expiryDays} days]"

            stats.add(
                "datatransfer:${dbo.id}",
                "data transfer (part of JCR)",
                "'${dbo.areaName.abbreviate(8)}",
                "$size: admins=[$admins]$accessUserString$accessGroupString$externalAccess$expiryDays"
            )
        }
    }
}
