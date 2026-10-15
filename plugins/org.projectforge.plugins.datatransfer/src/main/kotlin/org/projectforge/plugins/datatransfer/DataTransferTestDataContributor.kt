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


package org.projectforge.plugins.datatransfer

import org.projectforge.business.testdata.TestDataContext
import org.projectforge.business.testdata.TestDataContributor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Data transfer areas shared with customers and within the team.
 */
@Service
open class DataTransferTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    override val order = 110

    override val pluginId = DataTransferPlugin.ID

    override fun seed(ctx: TestDataContext) {
        listOf(
            Triple(
                "Alpine Insurance – Claims Portal",
                "Specifications, test reports and release documents shared with the customer",
                listOf("laura", "chris", "joe"),
            ),
            Triple(
                "Bluefin Logistics – Fleet Tracking",
                "Exchange of route data exports and acceptance protocols",
                listOf("joe", "alex"),
            ),
            Triple("Design assets", "Logos, mockups and style guides for all projects", listOf("ann", "max", "mona")),
        ).forEach { (name, description, users) ->
            // External access needs a password: the one of the sample users, if any.
            val external = !name.startsWith("Design") && ctx.password != null
            val area = DataTransferAreaDO().also {
                it.areaName = name
                it.description = description
                it.adminIds = "${ctx.user("laura").id}"
                it.accessUserIds = users.map { username -> ctx.user(username).id }.joinToString(",")
                it.expiryDays = 30
                it.maxUploadSizeKB = 500 * 1024
                it.externalDownloadEnabled = external
                it.externalUploadEnabled = external && name.startsWith("Alpine")
                it.externalPassword = if (external) ctx.password else null
            }
            dataTransferAreaDao.insert(area, checkAccess = false)
        }
    }
}
