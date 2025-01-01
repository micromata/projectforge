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

package org.projectforge.framework.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import org.projectforge.framework.time.PFDateTime
import java.io.IOException
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalTime

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
        objectMapper.registerModule(KotlinModule.Builder().build())
        objectMapperIgnoreNullableProps.registerModule(KotlinModule.Builder().build())
        val module = SimpleModule()
        initializeMapper(module)
        objectMapper.registerModule(module)
        objectMapperIgnoreNullableProps.registerModule(module)
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

    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun <T> fromJson(json: String?, typeReference: TypeReference<T>?, failOnUnknownProps: Boolean = true): T? {
        return if (failOnUnknownProps) {
            objectMapper.readValue(json, typeReference)
        } else {
            objectMapperIgnoreUnknownProps.readValue(json, typeReference)
        }
    }

    fun initializeMapper(module: SimpleModule) {
        module.addSerializer(LocalDate::class.java, LocalDateSerializer())
        module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())

        module.addSerializer(LocalTime::class.java, LocalTimeSerializer())
        module.addDeserializer(LocalTime::class.java, LocalTimeDeserializer())

        module.addSerializer(PFDateTime::class.java, PFDateTimeSerializer())
        module.addDeserializer(PFDateTime::class.java, PFDateTimeDeserializer())

        module.addSerializer(java.util.Date::class.java, UtilDateSerializer(UtilDateFormat.JS_DATE_TIME_MILLIS))
        module.addDeserializer(java.util.Date::class.java, UtilDateDeserializer())

        module.addSerializer(Timestamp::class.java, TimestampSerializer(UtilDateFormat.JS_DATE_TIME_MILLIS))
        module.addDeserializer(Timestamp::class.java, TimestampDeserializer())

        module.addSerializer(java.sql.Date::class.java, SqlDateSerializer())
        module.addDeserializer(java.sql.Date::class.java, SqlDateDeserializer())
    }

    /**
     * Writes the id by using methods [JsonGenerator.writeNullField], [JsonGenerator.writeNumberField] or
     * [JsonGenerator.writeString] dependent on type of id.
     * @param gen the json generator
     * @param field the field name.
     * @param value the id to write.
     */
    fun writeField(gen: JsonGenerator, field: String, value: Any?) {
        if (value == null) {
            gen.writeNullField(field)
        } else if (value is Long) {
            gen.writeNumberField(field, value)
        } else if (value is Int) {
            gen.writeNumberField(field, value)
        } else if (value is String) {
            gen.writeStringField(field, value)
        } else {
            gen.writeStringField(field, "$value")
        }
    }
}
