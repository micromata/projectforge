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

package org.projectforge.framework.jcr

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.common.DateFormatType
import org.projectforge.common.ZipMode
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

  /**
   * Filename without extension (before last '.')
   */
  val nameWithoutExtension: String
    get() = name?.substringBeforeLast('.', "") ?: ""

  /**
   * extension after last '.' (zip, pdf etc.)
   */
  val fileExtension: String
    get() = name?.substringAfterLast('.', "") ?: ""

  /**
   * Size of file in bytes.
   */
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
  var createdByUserId: Long? = null

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

  /**
   * The password isn't stored anywhere. If true, a password to decrypt is required for download.
   * An encrypted file is encrypted in the storage itself and has to be encrypted server-side before download.
   * After download the user gets the file decrypted.
   */
  var aesEncrypted: Boolean? = false

  /**
   * Info fields (used e. g. by DataTransferTool). You may add entries via [addInfo].
   */
  var info: MutableMap<String, Any?>? = null

  /**
   * If zip file is encrypted, the algorithm is stored (if encrypted by ProjectForge)
   */
  var zipMode: ZipMode? = null

  /**
   * If zip file isn't encrypted, this algorithm is the desired one, if the user presses "encrypt" button.
   */
  @PropertyInfo(i18nKey = "attachment.zip.encryptionAlgorithm")
  var newZipMode: ZipMode? = null

  /**
   * Optional password to encrypt file or to test encryption.
   */
  @PropertyInfo(i18nKey = "password")
  var password: String? = null

  @get:JsonProperty
  val encrypted: Boolean
    get() = zipMode?.isEncrpyted == true || aesEncrypted == true

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
    this.aesEncrypted = fileObject.aesEncrypted
    this.zipMode = fileObject.zipMode
  }

  /**
   * Appends entries to [info] map (map will be created if null).
   */
  fun addInfo(key: String, value: Any?) {
    info?.let {
      it[key] = value
    } ?: run {
      info = mutableMapOf(key to value)
    }
  }

  /**
   * Adds expiry info to map [info], used by DynamicAttachmentList.jsx.
   */
  fun addExpiryInfo(value: String) {
    addInfo(INFO_EXPIRY_KEY, value)
  }

  override fun toString(): String {
    return ToStringUtil.toJsonString(this)
  }

  companion object {
    const val INFO_EXPIRY_KEY = "expiryInfo"
  }
}
