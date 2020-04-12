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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.FileObject


/**
 * Represents a file object of jcr (including meta data as well as location in jcr).
 */
class Attachment() {
    /**
     * Unique id, set by jcr
     */
    var id: String? = null
    var name: String? = null
    var size: Int? = null
    @get:JsonProperty
    val sizeHumanReadable: String
        get() = NumberHelper.formatBytes(size)

    /**
     * Location of file as path to node in JCR.
     */
    var location: String? = null

    constructor(fileObject: FileObject) : this() {
        this.name = fileObject.fileName
        this.size = fileObject.size
        this.location = fileObject.location
    }
}
