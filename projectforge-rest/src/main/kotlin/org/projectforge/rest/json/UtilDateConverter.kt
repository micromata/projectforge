/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.apache.commons.lang3.StringUtils
import org.projectforge.rest.converter.DateTimeFormat
import java.io.IOException
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Serialization of dates in ISO format and UTC time-zone.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UtilDateSerializer : StdSerializer<java.util.Date>(java.util.Date::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: java.util.Date?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val dateFormatAsString = formatter.format(value.toInstant())
        jgen.writeString(dateFormatAsString)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern).withZone(ZoneOffset.UTC)
    }
}

/**
 * Deserialization of dates in ISO format and UTC time-zone.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UtilDateDeserializer : StdDeserializer<java.util.Date>(java.util.Date::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): java.util.Date? {
        val dateString = p.text
        if (StringUtils.isBlank(dateString)) {
            return null
        }
        if (StringUtils.isNumeric(dateString)) {
            return java.util.Date(dateString.toLong())
        }
        try {
            val date = LocalDateTime.parse(dateString, formatter)
            return java.util.Date.from(date.toInstant(ZoneOffset.UTC))
        } catch (e: ParseException) {
            throw JsonParseException(p, dateString, e)
        }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern).withZone(ZoneOffset.UTC)
    }
}
