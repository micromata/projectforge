/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.io.IOException

private val log = KotlinLogging.logger {}

/**
 * Serialization and deserialization for rest calls.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object JsonUtils {
  private val typeAdapterMap: MutableMap<Class<*>, Any> = HashMap()
  private val objectMapper: ObjectMapper = ObjectMapper()
  private val objectMapperIgnoreNullableProps: ObjectMapper = ObjectMapper()
  private val objectMapperIgnoreUnknownProps: ObjectMapper = ObjectMapper()

  init {
    objectMapper.registerModule(KotlinModule())
    objectMapperIgnoreNullableProps.registerModule(KotlinModule())
    objectMapperIgnoreNullableProps.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    objectMapperIgnoreUnknownProps.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  fun add(cls: Class<*>, typeAdapter: Any) {
    typeAdapterMap[cls] = typeAdapter
  }

  @JvmStatic
  @JvmOverloads
  fun toJson(obj: Any?, ignoreNullableProps: Boolean = false): String {
    return try {
      if (ignoreNullableProps) {
        objectMapperIgnoreNullableProps.writeValueAsString(obj)
      } else {
        objectMapper.writeValueAsString(obj)
      }
    } catch (ex: JsonProcessingException) {
      log.error(ex.message, ex)
      ""
    }
  }

  @JvmStatic
  @JvmOverloads
  @Throws(IOException::class)
  fun <T> fromJson(json: String?, classOfT: Class<T>?, failOnUnknownProps: Boolean = true): T? {
    return if (failOnUnknownProps) {
      objectMapper.readValue(json, classOfT)
    } else {
      objectMapperIgnoreUnknownProps.readValue(json, classOfT)
    }
  }
}
