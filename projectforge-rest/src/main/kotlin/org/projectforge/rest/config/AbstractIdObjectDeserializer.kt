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

package org.projectforge.rest.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer

/**
 * DTO objects or DO's may be deserialized only by id or as full objects. This deserializer handles both.
 * @author Kai Reinhard
 */
abstract class AbstractIdObjectDeserializer<T>(private val defaultDeserialize: JsonDeserializer<*>)
    : DelegatingDeserializer(defaultDeserialize) {

    abstract fun create(id: Int): T

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): T? {
        return if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            val node: JsonNode = p.codec.readTree(p)
            create(node.asInt())
        } else {
            @Suppress("UNCHECKED_CAST")
            defaultDeserialize.deserialize(p, ctxt) as? T
        }
    }
}
