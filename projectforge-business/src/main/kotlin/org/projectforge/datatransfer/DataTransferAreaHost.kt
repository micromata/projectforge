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

import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * A module storing its files in synthetic data transfer areas (e.g. the chat, one area per conversation), created
 * by [DataTransferBridge.saveSyntheticArea]. The module decides who sees such an area and its files, the data transfer
 * asks it ([DataTransferBridge.getAreaHost], registered by [DataTransferBridge.registerAreaHost]); nobody administers it in the data transfer and nobody uploads there.
 */
interface DataTransferAreaHost {
    /** Is the area one of this module? */
    fun owns(areaId: Long): Boolean

    /** May the user see the area and download its (accessible) files? */
    fun hasReadAccess(areaId: Long, user: PFUserDO): Boolean

    /** May the user see and download this file of the area (e.g. not the one of a hidden message)? */
    fun isFileAccessible(areaId: Long, fileId: String, user: PFUserDO): Boolean

    /** May the user delete this file of the area? */
    fun hasDeleteAccess(areaId: Long, fileId: String, user: PFUserDO): Boolean
}
