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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

object FilesUtils {

  private val mapper = ObjectMapper()

  fun toJson(obj: Any): String {
    return try {
      mapper.writeValueAsString(obj)
    } catch (ex: Exception) {
      val id = System.currentTimeMillis()
      log.error(ex) { "Exception while serializing object of type '${obj::class.java.simpleName}' #$id: ${ex.message}" }
      "[*** Exception while serializing object of type '${obj::class.java.simpleName}', see log files #$id for more details.]"
    }
  }

  init {
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
    mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
  }
}
