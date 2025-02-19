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
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.api.IdObject
import java.io.IOException

class IdsOnlySerializer : JsonSerializer<Collection<*>>() {
    @Throws(IOException::class)
    override fun serialize(value: Collection<*>?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
        } else if (HibernateUtils.isFullyInitialized(value)) {
            gen.writeStartArray()
            value.forEach { item ->
                if (item is IdObject<*>) {
                    IdOnlySerializer.writeObject(item, gen, serializers)
                } else {
                    // Serialize individual items explicitly:
                    serializers.defaultSerializeValue(item, gen)
                }
            }
            gen.writeEndArray()
        } else {
            // Write a null if the collection is not initialized (to avoid fetching it)
            gen.writeNull()
        }
    }
}
