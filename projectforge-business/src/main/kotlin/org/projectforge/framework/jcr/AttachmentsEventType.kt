/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.common.i18n.I18nEnum

enum class AttachmentsEventType(
  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  val key: String
) : I18nEnum {
  DOWNLOAD("download"),
  DOWNLOAD_MULTI("downloadMulti"),
  DOWNLOAD_ALL("downloadAll"),
  UPLOAD("upload"),
  DELETE("delete"),
  MODIFICATION("modification");

  open fun isIn(vararg types: AttachmentsEventType): Boolean {
    return types.contains(this)
  }

  /**
   * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
   */
  override val i18nKey: String
    get() = "file.upload.audit.action.$key"
}
