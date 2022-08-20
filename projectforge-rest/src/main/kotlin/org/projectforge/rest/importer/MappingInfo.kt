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
import java.util.*


class MappingInfo(
) {
  val entries = mutableListOf<MappingInfoEntry>()

  /**
   * @param name is the name of the property or the column head of the data table matching any alias.
   */
  fun getMapping(header: String): MappingInfoEntry? {
    return entries.find { it.matches(header) }
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
    fun parseMappingInfo(str: String?): MappingInfo {
      val result = MappingInfo()
      str ?: return result
      val props = Properties()
      props.load(StringReader(str))
      props.keys.forEach { key ->
        if (key != null) {
          key as String
          val entry = MappingInfoEntry(key)
          val value = props[key]
          if (value != null) {
            entry.setValues(value as String)
          }
          result.entries.add(entry)
        }
      }
      return result
    }
  }
}
