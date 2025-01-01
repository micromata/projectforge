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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.projectforge.framework.time.PFDayUtils
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val jsonDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Serialization of dates in ISO format and UTC time-zone.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LocalDateSerializer : StdSerializer<LocalDate>(LocalDate::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: LocalDate?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val dateFormatAsString = jsonDateFormatter.format(value)
        jgen.writeString(dateFormatAsString)
    }
}

/**
 * Deserialization for dates in ISO format as well as from users date format.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LocalDateDeserializer : StdDeserializer<LocalDate>(LocalDate::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): LocalDate? {
        return PFDayUtils.parseDate(p.text)
    }
}

