/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.common.DateFormatType
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.FileObject
import java.util.*

/**
 * Represents a file object of jcr (including meta data as well as location in jcr).
 */
class Attachment() {
    /**
     * Unique id, set by jcr
     */
    @PropertyInfo(i18nKey = "attachment.fileId")
    var fileId: String? = null

    /**
     * Filename.
     */
    @PropertyInfo(i18nKey = "attachment.fileName")
    var name: String? = null
    var size: Long? = null

    @get:JsonProperty
    val sizeHumanReadable: String
        get() = NumberHelper.formatBytes(size)

    @PropertyInfo(i18nKey = "description")
    var description: String? = null

    var created: Date? = null

    /**
     * Date of creation in user's timezone and date format.
     */
    @get:JsonProperty
    val createdFormatted: String
        get() = PFDateTime.fromOrNull(created)?.format(DateFormatType.DATE_TIME_MINUTES) ?: ""

    /**
     * Id or full name or external user info.
     */
    var createdByUser: String? = null

    /**
     * Id of internal user or null, if no internal user.
     */
    var createdByUserId: Int? = null

    var lastUpdate: Date? = null
    /**
     * Date of last update in user's timezone and date format.
     */
    @get:JsonProperty
    val lastUpdateFormatted: String
        get() = PFDateTime.fromOrNull(lastUpdate)?.format(DateFormatType.DATE_TIME_MINUTES) ?: ""
    /**
     * Date of last update as time-ago in user's locale.
     */
    @get:JsonProperty
    val lastUpdateTimeAgo: String
        get() = TimeAgo.getMessage(lastUpdate)
    var lastUpdateByUser: String? = null

    /**
     * Location of file as path to node in JCR.
     */
    var location: String? = null

    /**
     * If true, the user has no access to modify or delete this attachment.
     */
    var readonly: Boolean? = null

    /**
     * The checksum of the file, e. g.: (SHA256).
     */
    var checksum: String? = null

    constructor(fileObject: FileObject) : this() {
        this.fileId = fileObject.fileId
        this.name = fileObject.fileName
        this.size = fileObject.size
        this.description = fileObject.description
        this.created = fileObject.created
        this.createdByUser = fileObject.createdByUser
        this.lastUpdate = fileObject.lastUpdate
        this.lastUpdateByUser = fileObject.lastUpdateByUser
        this.checksum = fileObject.checksum
    }

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }
}
