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

import com.google.gson.*
import org.projectforge.rest.converter.DateTimeFormat
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Serialization and deserialization for dates in ISO format and UTC time-zone.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LocalDateTypeAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    companion object {
        private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.ISO_DATE.pattern)
    }

    @Synchronized
    public override fun serialize(date: LocalDate, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val dateFormatAsString = jsonDateTimeFormatter.format(date)
        return JsonPrimitive(dateFormatAsString)
    }

    @Synchronized
    public override fun deserialize(jsonElement: JsonElement, type: Type,
                                    jsonDeserializationContext: JsonDeserializationContext): LocalDate? {
        try {
            val element = jsonElement.getAsString()
            if (element == null) {
                return null
            }
            return LocalDate.parse(element, jsonDateTimeFormatter)
        } catch (e: DateTimeParseException) {
            throw JsonSyntaxException(jsonElement.getAsString(), e)
        }

    }
}
