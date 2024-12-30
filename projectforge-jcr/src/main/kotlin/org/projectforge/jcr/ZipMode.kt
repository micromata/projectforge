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

import org.projectforge.common.i18n.I18nEnum

enum class ZipMode(val key: String) : I18nEnum {
  /**
   * Not encrypted.
   */
  STANDARD("standard"),

  /**
   * Encrypted (algorithm not known, was uploaded as already encrypted file).
   */
  ENCRYPTED("encrypted"),

  /**
   * Encrypted with standard zip algorithm (most compatibility, less secure).
   */
  ENCRYPTED_STANDARD("encryptedStandard"),

  /**
   * AES-128 encrypted (not supported by all clients)
   */
  ENCRYPTED_AES128("encrytpedAes128"),

  /**
   * AES-256 encrypted (not supported by all clients, highest security)
   */
  ENCRYPTED_AES256("encrytpedAes256");

  val isEncrpyted: Boolean
    get() = this != STANDARD

  /**
   * The key will be used e. g. for i18n.
   *
   * @return
   */
  override val i18nKey: String
    get() = "attachment.zip.$key"
}
