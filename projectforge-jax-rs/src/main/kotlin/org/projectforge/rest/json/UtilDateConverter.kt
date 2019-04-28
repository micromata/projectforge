/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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
        val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern).withZone(ZoneOffset.UTC)
        val dateFormatAsString = formatter.format(value.toInstant())
        jgen.writeString(dateFormatAsString)
    }
}

/**
 * Deserialization of dates in ISO format and UTC time-zone.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UtilDateDeserializer : StdDeserializer<java.util.Date>(java.util.Date::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): java.util.Date? {
        val dateString = p.getText()
        if (StringUtils.isBlank(dateString)) {
            return null
        }
        if (StringUtils.isNumeric(dateString) == true) {
            val date = java.sql.Date(dateString.toLong())
            return date;
        }
        try {
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern).withZone(ZoneOffset.UTC)
            val date = LocalDateTime.parse(dateString, formatter);
            return java.sql.Date.valueOf(date.toLocalDate())
        } catch (e: ParseException) {
            throw JsonParseException(p, dateString, e);
        }
    }
}
