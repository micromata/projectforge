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

package org.projectforge.framework.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.sql.Timestamp
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TimestampFormat(val pattern: String) {
    /** yyyy-MM-dd HH:mm:ss.SSS  */
    ISO_DATE_TIME_MILLIS("yyyy-MM-dd HH:mm:ss.SSS");

    internal val formatter: DateTimeFormatter =  DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC)
}

/**
 * Serialization of dates in ISO format and UTC time-zone.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TimestampSerializer(private val format: TimestampFormat) : StdSerializer<Timestamp>(Timestamp::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Timestamp?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val dateFormatAsString = format.formatter.format(value.toInstant())
        jgen.writeString(dateFormatAsString)
    }
}

/**
 * Deserialization of dates in ISO format and UTC time-zone.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TimestampDeserializer(private val format: TimestampFormat) : StdDeserializer<Timestamp>(Timestamp::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Timestamp? {
        val dateString = p.text
        if (StringUtils.isBlank(dateString)) {
            return null
        }
        if (StringUtils.isNumeric(dateString)) {
            return Timestamp(dateString.toLong())
        }
        try {
            val date = LocalDateTime.parse(dateString, format.formatter)
            return Timestamp.from(date.toInstant(ZoneOffset.UTC))
        } catch (e: ParseException) {
            throw JsonParseException(p, dateString, e)
        }
    }
}
