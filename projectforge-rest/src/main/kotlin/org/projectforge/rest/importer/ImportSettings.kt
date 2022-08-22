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

package org.projectforge.rest.importer

import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*


class ImportSettings(
) {
  val fieldSettings = mutableListOf<ImportFieldSettings>()
  var encoding: String? = null
  val charSet: Charset
    get() {
      encoding ?: return StandardCharsets.UTF_8
      return try {
        Charset.forName(encoding)
      } catch (ex: Exception) {
        StandardCharsets.UTF_8
      }
    }

  /**
   * @param name is the name of the property or the column head of the data table matching any alias.
   */
  fun getFieldSettings(header: String): ImportFieldSettings? {
    return fieldSettings.find { it.matches(header) }
  }

  companion object {
    /**
     * Will be read as key-value file. Key is the property and value is the alias-parseFormat-string.
     * Example:
     * birthday=born*|*birthday*|:MM/dd/yyyy|:MM/dd/yyy
     * firstName=first*name|sur*name*
     *
     * birthday and firstname will have two aliases (usable for column heads) and birthday supports two date
     * formats while parsing: MM/dd/yyyy and MM/dd/yy.
     */
    fun parseSettings(str: String?): ImportSettings {
      val settings = ImportSettings()
      str ?: return settings
      val props = Properties()
      props.load(StringReader(str))
      props.keys.forEach { key ->
        if (key != null) {
          key as String
          val value = props[key] as String?
          if (key == "encoding") {
            settings.encoding = value
          } else {
            val entry = ImportFieldSettings(key)
            if (value != null) {
              entry.parseSettings(value)
            }
            settings.fieldSettings.add(entry)
          }
        }
      }
      return settings
    }
  }
}
