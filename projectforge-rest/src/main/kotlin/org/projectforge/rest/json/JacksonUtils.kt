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
import java.math.BigDecimal

class JacksonUtils {
    companion object {
        fun writeField(jgen: JsonGenerator, field: String, value: Any?) {
            if (value == null) return
            when (value) {
                is String -> jgen.writeStringField(field, value)
                is Boolean -> jgen.writeBooleanField(field, value)
                is Int -> jgen.writeNumberField(field, value)
                is Double -> jgen.writeNumberField(field, value)
                is Float -> jgen.writeNumberField(field, value)
                is Long -> jgen.writeNumberField(field, value)
                is BigDecimal -> jgen.writeNumberField(field, value)
                else -> jgen.writeStringField(field, value.toString())
            }
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Boolean?) {
            if (value == null) return
            jgen.writeBooleanField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: String?) {
            if (value == null) return
            jgen.writeStringField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Int?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Double?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Float?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Long?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: BigDecimal?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }
    }
}
