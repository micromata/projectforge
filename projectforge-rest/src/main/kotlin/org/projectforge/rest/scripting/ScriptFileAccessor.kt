/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.scripting

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.rest.core.AbstractPagesRest

/**
 * Will be accessible by Groovy and Kotlin scripts for loading files.
 */
class ScriptFileAccessor(
  private val attachmentsService: AttachmentsService,
  private val scriptPagesRest: AbstractPagesRest<*, *, *>,
  private val scriptDO: ScriptDO,
) {
  val attachments =
    attachmentsService.getAttachments(scriptPagesRest.jcrPath!!, scriptDO.id, scriptPagesRest.attachmentsAccessChecker)

  /**
   * Backward compability.
   * @return file of db script entry or newest attachment, if db entry doesn't contain a file.
   */
  val file: ByteArray
    get() {
      scriptDO.file?.let { return it }
      val newestAttachment =
        attachments?.sortedBy { it.created }?.get(0)?.name ?: throw IllegalArgumentException("No file found.")
      return getFile(newestAttachment)
    }

  /**
   * Backward compability.
   * @return file of db script entry or newest attachment, if db entry doesn't contain a file.
   */
  val filename: String
    get() {
      return scriptDO.filename ?: attachments?.sortedBy { it.created }?.get(0)?.name
      ?: throw IllegalArgumentException("No file found for getting filename.")
    }

  fun getFile(name: String): ByteArray {
    val attachment = attachments?.find { it.name == name }
    val fileId = attachment?.fileId
    if (fileId == null) {
      throw IllegalArgumentException("File not found under name '$name'.")
    }
    val istream = attachmentsService.getAttachmentInputStream(
      scriptPagesRest.jcrPath!!,
      scriptDO.id,
      fileId,
      scriptPagesRest.attachmentsAccessChecker
    )?.second
    if (istream == null) {
      throw IllegalArgumentException("File inputstream not found under name '$name'.")
    }
    istream.use {
      return istream.readAllBytes()
    }
  }
}
