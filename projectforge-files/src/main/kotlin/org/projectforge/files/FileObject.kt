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

package org.projectforge.files

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.common.FormatterUtils
import org.projectforge.common.ZipMode

/**
 * Files in the file store may be addressed by location (parent node) and id or location and filename.
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
   * A unique random alphanumeric string. Leave this id null for new files to store.
   */
  var fileId: String? = null

  /**
   * The location (as path) of the file in the file store, built of [parentNodePath] and [relPath].
   */
  val location: String
    get() = "${getLocation(parentNodePath, relPath)}"

  /**
   * The location is built of parentNodePath and relPath.
   */
  var parentNodePath: String? = null

  /**
   * The location is built of parentNodePath and relPath.
   */
  var relPath: String? = null

  override fun toString(): String {
    return "location=[$location],id=[$fileId],fileName=[$fileName],size=[${FormatterUtils.formatBytes(size)}],passwordEncrypted=[${aesEncrypted == true}]"
  }

  companion object {
    internal fun getLocation(parentPath: String?, relPath: String?): String? {
      if (parentPath == null && relPath == null) {
        return null
      }
      parentPath ?: return relPath
      relPath ?: return parentPath
      return if (parentPath.endsWith("/")) "$parentPath$relPath" else "$parentPath/$relPath"
    }
  }
}
