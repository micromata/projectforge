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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.common.extensions.formatBytes
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.datatransfer.DataTransferInterface
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.plugins.datatransfer.rest.DataTransferPageRest
import org.projectforge.rest.core.PagesResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DataTransferService : DataTransferInterface {
    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    @Autowired
    private lateinit var domainService: DomainService

    val jcrPath: String by lazy { dataTransferAreaPagesRest.jcrPath!! }

    @PostConstruct
    private fun postConstruct() {
        dataTransferBridge.register(this)
    }

    override fun getPersonalBoxOfUserLink(userId: Long): String {
        val personalBox = dataTransferAreaDao.ensurePersonalBox(userId)
            ?: throw IllegalStateException("Personal box not found for user with ID $userId.")
        return getDataTransferAreaLink(personalBox.id)
    }

    fun getDataTransferAreaLink(areaId: Long?): String {
        return domainService.getDomain(
            PagesResolver.getDynamicPageUrl(
                DataTransferPageRest::class.java,
                id = areaId ?: 0
            )
        )
    }

    override fun putFileInUsersInBox(
        receiver: PFUserDO,
        filename: String,
        content: ByteArray,
        description: String?
    ): Boolean {
        val reveiverId = receiver.id
            ?: throw IllegalStateException("User ID is null for user with display name '${receiver.userDisplayName}'.")
        val personalBox = dataTransferAreaDao.ensurePersonalBox(reveiverId)
            ?: throw IllegalStateException("Personal box not found for user with ID $reveiverId.")
        try {
            attachmentsService.addAttachment(
                dataTransferAreaPagesRest.jcrPath!!,
                fileInfo = FileInfo(
                    filename,
                    fileSize = content.size.toLong(),
                    description = description
                ),
                content = content,
                baseDao = dataTransferAreaDao,
                obj = personalBox,
                accessChecker = dataTransferAreaPagesRest.attachmentsAccessChecker,
            )
            log.info("Document '$filename' of size ${content.size.formatBytes()} put in the personal box (DataTransfer) of '${receiver.userDisplayName}' with description '$description'.")
            return true
        } catch (ex: Exception) {
            log.error(
                "Can't put document '${filename}' of size ${content.size.formatBytes()} into user '${receiver.userDisplayName}' personal box: ${ex.message}",
                ex
            )
        }
        return false
    }
}
