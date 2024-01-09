/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import java.util.*

/**
 * For setting some attributes for storing or changing files.
 */
open class FileInfo(
  /**
   * The UTF-8 filename.
   */
  var fileName: String? = null,
  /**
   * Optional description.
   */
  var description: String? = null,
  /**
   * Should only be set in test cases. Will be set automatically.
   */
  var created: Date? = null,
  /**
   * Should only be set in test cases. Will be set automatically.
   */
  var lastUpdate: Date? = null,
  var createdByUser: String? = null,
  var lastUpdateByUser: String? = null,
  /**
   * The checksum of the file, e. g.: (SHA256).
   */
  var checksum: String? = null,
  /**
   * The password isn't stored anywhere. If true, a password to decrypt is required for download.
   * An encrypted file is encrypted in the storage itself and has to be encrypted server-side before download.
   * After download the user gets the file decrypted.
   */
  var aesEncrypted: Boolean? = false,
  /**
   * The password isn't stored anywhere. If zip file is encrypted, the algorithm is stored (if encrypted by ProjectForge).
   */
  var zipMode: ZipMode? = null,

  /**
   * Indicates, that an encryption process in in progress. Used by DataTransferDao to suppress several e-mail notifications
   * caused by adding and deleting current file.
   */
  var encryptionInProgress: Boolean? = null,
  fileSize: Long? = null
) {
  /**
   * The file size if known (length of content).
   */
  var size: Long? = fileSize
    internal set

  /**
   * extension after last '.' (zip, pdf etc.)
   */
  val fileExtension: String
    get() = fileName?.substringAfterLast('.', "") ?: ""

  fun copyFrom(other: FileInfo?) {
    other ?: return
    this.fileName = other.fileName
    this.description = other.description
    this.created = other.created
    this.lastUpdate = other.lastUpdate
    this.createdByUser = other.createdByUser
    this.lastUpdateByUser = other.lastUpdateByUser
    this.size = other.size
    this.checksum = other.checksum
    this.aesEncrypted = other.aesEncrypted
    this.zipMode = other.zipMode
  }
}
