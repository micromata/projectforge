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

package org.projectforge.jcr

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.common.ZipMode
import javax.jcr.Node

private val log = KotlinLogging.logger {}

/**
 * Files in the content repository may addressed by location (parent node) and id or location and filename.
 */
class FileObject() : FileInfo() {
  @JvmOverloads
  constructor(
    parentNodePath: String?,
    relPath: String? = null,
    fileId: String? = null,
    fileInfo: FileInfo? = null,
    encryptionInProgress: Boolean? = null,
  ) : this() {
    this.parentNodePath = parentNodePath
    this.relPath = relPath
    this.fileId = fileId
    this.encryptionInProgress = encryptionInProgress
    copyFrom(fileInfo)
  }

  internal constructor(node: Node, parentNodePath: String? = null, relPath: String? = null) : this() {
    this.copyFrom(node)
    this.parentNodePath = parentNodePath ?: node.path
    this.relPath = relPath
  }

  internal constructor(nodeInfo: NodeInfo) : this() {
    fileName = nodeInfo.getProperty(OakStorage.PROPERTY_FILENAME)?.value?.string
    description = nodeInfo.getProperty(OakStorage.PROPERTY_FILEDESC)?.value?.string
    created = PFJcrUtils.convertToDate(nodeInfo.getProperty(OakStorage.PROPERTY_CREATED)?.value?.string)
    createdByUser = nodeInfo.getProperty(OakStorage.PROPERTY_CREATED_BY_USER)?.value?.string
    lastUpdate = PFJcrUtils.convertToDate(nodeInfo.getProperty(OakStorage.PROPERTY_LAST_UPDATE)?.value?.string)
    lastUpdateByUser = nodeInfo.getProperty(OakStorage.PROPERTY_LAST_UPDATE_BY_USER)?.value?.string
    fileId = nodeInfo.name
    size = nodeInfo.getProperty(OakStorage.PROPERTY_FILESIZE)?.value?.long
    if (log.isDebugEnabled) {
      log.debug { "Restoring: ${PFJcrUtils.toJson(this)}" }
    }
  }

  /**
   * Copies all fields from node to this, excluding content and path setting (parent path as well as relative path).
   */
  internal fun copyFrom(node: Node) {
    fileName = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_FILENAME)?.string
    description = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_FILEDESC)?.string
    created = PFJcrUtils.getPropertyAsDate(node, OakStorage.PROPERTY_CREATED)
    createdByUser = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_CREATED_BY_USER)?.string
    lastUpdate = PFJcrUtils.getPropertyAsDate(node, OakStorage.PROPERTY_LAST_UPDATE)
    lastUpdateByUser = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_LAST_UPDATE_BY_USER)?.string
    fileId = node.name
    size = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_FILESIZE)?.long
    checksum = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_CHECKSUM)?.string
    aesEncrypted = PFJcrUtils.getProperty(node, OakStorage.PROPERTY_AES_ENCRYPTED)?.boolean == true
    PFJcrUtils.getProperty(node, OakStorage.PROPERTY_ZIP_MODE)?.string?.let {
      zipMode = ZipMode.valueOf(it)
    }
    if (log.isDebugEnabled) {
      log.debug { "Restoring: ${PFJcrUtils.toJson(this)}" }
    }
  }

  /**
   * Copies all fields from this to node, excluding content and id/name.
   */
  internal fun copyTo(node: Node) {
    node.setProperty(OakStorage.PROPERTY_FILENAME, fileName)
    node.setProperty(OakStorage.PROPERTY_FILEDESC, description ?: "")
    node.setProperty(OakStorage.PROPERTY_CREATED, PFJcrUtils.convertToString(created) ?: "")
    node.setProperty(OakStorage.PROPERTY_CREATED_BY_USER, createdByUser ?: "")
    node.setProperty(OakStorage.PROPERTY_LAST_UPDATE, PFJcrUtils.convertToString(lastUpdate) ?: "")
    node.setProperty(OakStorage.PROPERTY_LAST_UPDATE_BY_USER, lastUpdateByUser ?: "")
    node.setProperty(OakStorage.PROPERTY_AES_ENCRYPTED, aesEncrypted == true)
    zipMode?.let {
      node.setProperty(OakStorage.PROPERTY_ZIP_MODE, it.name)
    }
    setChecksum(node, checksum)
    size?.let { node.setProperty(OakStorage.PROPERTY_FILESIZE, it) }
    log.info { "Storing file info: ${PFJcrUtils.toJson(this)}" }
  }

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
   * A unique random alphanumeric string. This id will internally also used as child node name of [RepoService.NODENAME_FILES].
   * Leave this id null for new files to store.
   */
  var fileId: String? = null

  /**
   * The location (as path) of the file in the content repository. The location is relative to main node.
   *
   * This node specified by this location contains a child node named [RepoService.NODENAME_FILES], where
   * the file node with id as name resists.
   */
  val location: String
    get() = "${OakStorage.getAbsolutePath(parentNodePath, relPath)}"

  /**
   * The location is built of parentNodePath and relPath.
   */
  var parentNodePath: String? = null

  /**
   * The location is built of parentNodePath and relPath.
   */
  var relPath: String? = null

  override fun toString(): String {
    return "location=[$location],id=[$fileId],fileName=[$fileName],size=[${FormatterUtils.formatBytes(size)}],isCypted=[${aesEncrypted == true}]"
  }

  companion object {
    internal fun setChecksum(node: Node, checksum: String?) {
      node.setProperty(OakStorage.PROPERTY_CHECKSUM, checksum ?: "")
    }
  }
}
