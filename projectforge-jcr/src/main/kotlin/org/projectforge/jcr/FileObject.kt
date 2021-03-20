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

package org.projectforge.jcr

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import java.util.*
import javax.jcr.Node

private val log = KotlinLogging.logger {}

/**
 * Files in the content repository may addressed by location (parent node) and id or location and filename.
 */
class FileObject() {
    @JvmOverloads
    constructor(parentNodePath: String?, relPath: String? = null, fileId: String? = null, fileName: String? = null) : this() {
        this.parentNodePath = parentNodePath
        this.relPath = relPath
        this.fileId = fileId
        this.fileName = fileName
    }

    internal constructor(node: Node, parentNodePath: String? = null, relPath: String? = null) : this() {
        this.copyFrom(node)
        this.parentNodePath = parentNodePath ?: node.path
        this.relPath = relPath
    }

    /**
     * Copies all fields from node to this, excluding content and path setting (parent path as well as relative path).
     */
    internal fun copyFrom(node: Node) {
        fileName = node.getProperty(RepoService.PROPERTY_FILENAME)?.string
        description = node.getProperty(RepoService.PROPERTY_FILEDESC)?.string
        created = PFJcrUtils.convertToDate(node.getProperty(RepoService.PROPERTY_CREATED)?.string)
        createdByUser = node.getProperty(RepoService.PROPERTY_CREATED_BY_USER)?.string
        lastUpdate = PFJcrUtils.convertToDate(node.getProperty(RepoService.PROPERTY_LAST_UPDATE)?.string)
        lastUpdateByUser = node.getProperty(RepoService.PROPERTY_LAST_UPDATE_BY_USER)?.string
        fileId = node.name
        size = node.getProperty(RepoService.PROPERTY_FILESIZE)?.long
        if (log.isDebugEnabled) {
            log.debug { "Restoring: ${PFJcrUtils.toJson(this)}" }
        }
    }

    /**
     * Copies all fields from this to node, excluding content and id/name.
     */
    internal fun copyTo(node: Node) {
        node.setProperty(RepoService.PROPERTY_FILENAME, fileName)
        node.setProperty(RepoService.PROPERTY_FILEDESC, description ?: "")
        node.setProperty(RepoService.PROPERTY_CREATED, PFJcrUtils.convertToString(created) ?: "")
        node.setProperty(RepoService.PROPERTY_CREATED_BY_USER, createdByUser ?: "")
        node.setProperty(RepoService.PROPERTY_LAST_UPDATE, PFJcrUtils.convertToString(lastUpdate) ?: "")
        node.setProperty(RepoService.PROPERTY_LAST_UPDATE_BY_USER, lastUpdateByUser ?: "")
        size?.let { node.setProperty(RepoService.PROPERTY_FILESIZE, it) }
        log.info { "Storing file info: ${PFJcrUtils.toJson(this)}" }
    }

    /**
     * The UTF-8 filename.
     */
    var fileName: String? = null

    /**
     * The content of the file.
     */
    @JsonIgnore
    var content: ByteArray? = null
        set(value) {
            field = value
            size = value?.size?.toLong()
        }

    /**
     * An unique random alpha-numeric string. This id will internally also used as child node name of [RepoService.NODENAME_FILES].
     * Leave this id null for new files to store.
     */
    var fileId: String? = null

    /**
     * The file size if known (length of content).
     */
    var size: Long? = null
        internal set

    /**
     * Optional description.
     */
    var description: String? = null

    var created: Date? = null

    var lastUpdate: Date? = null

    var createdByUser: String? = null

    var lastUpdateByUser: String? = null

    /**
     * The location (as path) of the file in the content repository. The location is relative to main node.
     *
     * This node specified by this location contains a child node named [RepoService.NODENAME_FILES], where
     * the file node with id as name resists.
     */
    val location: String
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
        return "location=[$location],id=[$fileId],fileName=[$fileName],size=[${FormatterUtils.formatBytes(size)}]"
    }
}
