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

package org.projectforge.i18n

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.framework.json.JsonUtils
import java.io.File

internal class I18nKeyUsageEntry(val i18nKey: String) {
  var bundleName: String? = null
  var translation: String? = null
  var translationDE: String? = null
  val usedInClasses = mutableSetOf<Class<*>>()
  val usedInFiles = mutableSetOf<File>()

  val classes: String
    @JsonIgnore
    get() = usedInClasses.joinToString { it.simpleName }

  val files: String
    @JsonIgnore
    get() = usedInFiles.joinToString { it.name }

  fun addUsage(file: File) {
    usedInFiles.add(file)
  }

  fun addUsage(clazz: Class<*>) {
    usedInClasses.add(clazz)
  }

  override fun toString(): String {
    return JsonUtils.toJson(this)
  }

  companion object {
    fun read(str: String): I18nKeyUsageEntry? {
      return JsonUtils.fromJson(str, I18nKeyUsageEntry::class.java)
    }
  }
}
