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
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UISelect
import java.io.IOException

/**
 * Serialization. This serialization is needed, because the values of UI Select are serialized with a customizable
 * attribute name (labelProperty and valueProperty).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UISelectTypeSerializer : StdSerializer<UISelect<*>>(UISelect::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: UISelect<*>?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) return

        jgen.writeStartObject();
        jgen.writeStringField("id", value.id)
        jgen.writeStringField("type", value.type.name)
        jgen.writeStringField("key", value.key)
        JacksonUtils.writeField(jgen, "required", value.required)
        JacksonUtils.writeField(jgen, "multi", value.multi)
        JacksonUtils.writeField(jgen, "label", value.label)
        JacksonUtils.writeField(jgen, "additionalLabel", value.additionalLabel)
        JacksonUtils.writeField(jgen, "tooltip", value.tooltip)
        JacksonUtils.writeField(jgen, "labelProperty", value.labelProperty)
        JacksonUtils.writeField(jgen, "valueProperty", value.valueProperty)

        if (!value.values.isNullOrEmpty()) {
            jgen.writeArrayFieldStart("values")
            value.values?.forEach {
                if (it.value != null) {
                    jgen.writeStartObject();
                    JacksonUtils.writeField(jgen, value.valueProperty, it.value) // Custom serialization needed.
                    jgen.writeStringField(value.labelProperty, it.label)         // Custom serialization needed.
                    jgen.writeEndObject()
                }
            }
            jgen.writeEndArray()
        }

        if (value.autoCompletion != null) {
            jgen.writeObjectFieldStart("autoCompletion")
            val ac = value.autoCompletion
            JacksonUtils.writeField(jgen, "minChars", ac?.minChars)
            JacksonUtils.writeField(jgen, "url", ac?.url)
            writeEntries(jgen, ac?.values, "values", value.valueProperty, value.labelProperty) // See above.
            writeEntries(jgen, ac?.favorites, "favorites", value.valueProperty, value.labelProperty) // See above.
            jgen.writeEndObject()
        }
        jgen.writeEndObject()
    }

    private fun writeEntries(jgen: JsonGenerator, entries: List<AutoCompletion.Entry<out Any?>>?, property: String, valueProperty: String, labelProperty: String) {
        if (entries != null) {
            jgen.writeArrayFieldStart(property)
            entries.forEach {
                jgen.writeStartObject();
                JacksonUtils.writeField(jgen, valueProperty, it.value)
                jgen.writeStringField(labelProperty, it.label)
                JacksonUtils.writeField(jgen, "allSearchableFields", it.allSearchableFields)
                jgen.writeEndObject()
            }
            jgen.writeEndArray()
        }
    }
}
