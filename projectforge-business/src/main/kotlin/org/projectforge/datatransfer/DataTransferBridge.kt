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

package org.projectforge.datatransfer

import jakarta.annotation.PostConstruct
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service

/**
 * Bridge for data transfer, if available as plugin. DataTransfer itself isn't accessible from this module.
 */
@Service
class DataTransferBridge {
    private var dataTransferInterface: DataTransferInterface? = null

    val available: Boolean by lazy { dataTransferInterface != null }

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * The DataTransferService of the plugin datatransfer must call this method to register itself.
     */
    fun register(dataTransferInterface: DataTransferInterface) {
        this.dataTransferInterface = dataTransferInterface
    }

    fun getPersonalBoxOfUserLink(userId: Long = ThreadLocalUserContext.requiredLoggedInUserId): String? {
        return dataTransferInterface?.getPersonalBoxOfUserLink(userId)
    }

    fun putFileInUsersInBox(
        filename: String,
        content: ByteArray,
        receiver: PFUserDO = ThreadLocalUserContext.requiredLoggedInUser,
        description: String? = null,
    ): Boolean {
        return dataTransferInterface?.putFileInUsersInBox(
            receiver = receiver,
            filename = filename,
            content = content,
            description = description
        ) ?: false
    }

    fun putFileInUsersInBox(
        filename: String,
        content: String,
        receiver: PFUserDO = ThreadLocalUserContext.requiredLoggedInUser,
        description: String? = null,
    ): Boolean {
        return dataTransferInterface?.putFileInUsersInBox(
            receiver = receiver,
            filename = filename,
            content = content.toByteArray(),
            description = description
        ) ?: false
    }

    companion object {
        lateinit var instance: DataTransferBridge
            private set

        val available: Boolean by lazy { instance.available }
    }
}
