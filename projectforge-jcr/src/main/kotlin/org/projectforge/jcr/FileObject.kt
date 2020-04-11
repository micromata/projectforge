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

package org.projectforge.jcr

import javax.jcr.Node

/**
 * Files in the content repository may addressed by location (parent node) and id or location and filename.
 */
class FileObject() {
    @JvmOverloads
    constructor(parentNodePath: String?, relPath:String? = null, id: String? = null, fileName: String? = null) : this() {
        this.parentNodePath = parentNodePath
        this.relPath = relPath
        this.id = id
        this.fileName = fileName
    }

    internal constructor(node: Node) : this() {
        fileName = node.getProperty(RepoService.PROPERTY_FILENAME)?.string
        parentNodePath = node.path
        id = node.name
        size = node.getProperty(RepoService.PROPERTY_FILESIZE)?.long?.toInt()
    }

    /**
     * The UTF-8 filename.
     */
    var fileName: String? = null

    /**
     * The content of the file.
     */
    var content: ByteArray? = null
        set(value) {
            field = value
            size = value?.size
        }

    /**
     * An unique random alpha-numeric string. This id will internally also used as child node name of [RepoService.NODENAME_FILES].
     * Leave this id null for new files to store.
     */
    var id: String? = null

    /**
     * The file size if known (length of content).
     */
    var size: Int? = null
        internal set

    /**
     * The location (as path) of the file in the content repository. The location is relative to main node.
     *
     * This node specified by this location contains a child node named [RepoService.NODENAME_FILES], where
     * the file node with id as name resists.
     */
    val location: String?
        get() = "${RepoService.getAbsolutePath(parentNodePath, relPath)}"

    /**
     * The location is built of parentNodePath and relPath.
     */
    var parentNodePath: String? = null

    /**
     * The location is built of parentNodePath and relPath.
     */
    var relPath: String? = null

    override fun toString(): String {
        return "location=[$location],id=[$id],fileName=[$fileName],size=[$size]"
    }
}
