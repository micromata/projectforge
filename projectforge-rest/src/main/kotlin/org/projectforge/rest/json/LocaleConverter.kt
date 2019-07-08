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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.projectforge.rest.core.LanguageService
import java.io.IOException
import java.util.*


/**
 * Serialization for Locale.
 */
class LocaleSerializer : StdSerializer<Locale>(Locale::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Locale?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val user = LanguageService.Language(value)
        jgen.writeObject(user)
    }
}

/**
 * Deserialization of Locale.
 */
class LocaleDeserializer : StdDeserializer<Locale>(Locale::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Locale? {
        val node: JsonNode = p.getCodec().readTree(p) ?: return null
        val lang = node.get("lang").asText()
        return Locale(lang)
    }
}
