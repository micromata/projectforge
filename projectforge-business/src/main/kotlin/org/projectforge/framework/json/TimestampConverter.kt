/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.time.ZoneOffset
import java.util.*

/**
 * Serialization of dates in ISO format and UTC time-zone.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TimestampSerializer(private val format: UtilDateFormat) : StdSerializer<Date>(Date::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Date?, jgen: JsonGenerator, provider: SerializerProvider) {
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
 * @param format If given, only the given format will be tried for deserialization. If null, all formats will be tried (recommended).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TimestampDeserializer(private val format: UtilDateFormat? = null) : StdDeserializer<Date>(Date::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Date? {
        val dateString = p.text
        if (StringUtils.isBlank(dateString)) {
            return null
        }
        if (StringUtils.isNumeric(dateString)) {
            return Date(dateString.toLong())
        }
        val date = UtilDateDeserializer.parseDate(format, dateString, p)
        return Date.from(date.toInstant(ZoneOffset.UTC))
    }
}
