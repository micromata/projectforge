/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.jcr

import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable

/**
 * Checks access to attachments.
 */
open class AttachmentsDaoAccessChecker<O : ExtendedBaseDO<Int>>(
        val baseDao: BaseDao<O>,
        val jcrPath: String?,
        supportedListIds: Array<String>?)
    : AttachmentsAccessChecker {

    val supportedListIds: Array<String> = if (supportedListIds == null) {
        DEFAULT_LIST_OF_ATTACHMENTS
    } else {
        DEFAULT_LIST_OF_ATTACHMENTS.plus(supportedListIds)
    }

    /**
     * @param subPath Equals to listId.
     */
    override fun checkSelectAccess(user: PFUserDO, path: String, id: Any, subPath: String?) {
        checkJcrActivity(subPath)
        baseDao.getById(id as Serializable)
                ?: throw TechnicalException("No access for ${paramsToString(baseDao, path, id, "*", subPath)}. Object with id not found or without user access.")
    }

    /**
     * @param subPath Equals to listId.
     */
    override fun checkUploadAccess(user: PFUserDO, path: String, id: Any, subPath: String?) {
        checkJcrActivity(subPath)
        val obj = baseDao.getById(id as Serializable)
                ?: throw IllegalArgumentException("No write access for ${paramsToString(baseDao, path, id, "*", subPath)}. Object with id not found or without user access.")
        if (!baseDao.hasUpdateAccess(user, obj, obj, false)) {
            throw TechnicalException("No write access for ${paramsToString(baseDao, path, id, "*", subPath)}.")
        }
    }

    /**
     * @param subPath Equals to listId.
     */
    override fun checkDownloadAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?) {
        checkJcrActivity(subPath)
        checkSelectAccess(user, path, id, subPath)
    }

    /**
     * @param subPath Equals to listId.
     */
    override fun checkUpdateAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?) {
        checkJcrActivity(subPath)
        checkUploadAccess(user, path, id, subPath)
    }

    /**
     * @param subPath Equals to listId.
     */
    override fun checkDeleteAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?) {
        checkJcrActivity(subPath)
        checkUploadAccess(user, path, id, subPath)
    }

    /**
     * Should be called on every check access method first!
     */
    fun checkJcrActivity(listId: String? = null) {
        jcrPath
                ?: throw TechnicalException("Attachments are not supported by this entity: ${baseDao.doClass.name}.", "You must specify jcrPath in yours *PagesRest.")
        if (listId != null && !supportedListIds.contains(listId)) {
            throw TechnicalException("Attachments are not supported by entity ${baseDao.doClass.name} for list '$listId'.", "You must add this listId to supportedAttachmentList of yours *PagesRest.")
        }
    }

    private fun paramsToString(baseDao: BaseDao<*>, path: String, id: Any, fileId: String, subPath: String?): String {
        return "dao='${baseDao::class.java.name}', path='$path', id='$id', fileId='$fileId', subPath='$subPath'"
    }

    companion object {
        private val DEFAULT_LIST_OF_ATTACHMENTS = arrayOf(AttachmentsService.DEFAULT_NODE)
    }
}

