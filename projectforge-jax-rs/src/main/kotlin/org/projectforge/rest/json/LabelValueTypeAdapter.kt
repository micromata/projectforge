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
import org.apache.commons.beanutils.PropertyUtils
import java.lang.reflect.Type

/**
 * Serialization and deserialization for label value objects.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LabelValueTypeAdapter<T>(
        val valueProperty: String?,
        /**
         * Should be a valid string property of the target class for deserialization in jax ws.
         */
        val labelProperty: String?,
        /**
         * If given this function will be used for generating the label, otherwise the field value of titleProperty is used.
         */
        val asLabel: ((obj: T) -> String)? = null)
    : JsonSerializer<T> {

    @Synchronized
    public override fun serialize(obj: T, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val result = JsonObject()
        val id = PropertyUtils.getProperty(obj, valueProperty) ?: ""
        result.add(valueProperty,
                when (id) {
                    is String -> JsonPrimitive(id)
                    is Integer -> JsonPrimitive(id)
                    else -> JsonPrimitive(id.toString())
                })
        val title = if (asLabel != null) asLabel.invoke(obj) else PropertyUtils.getProperty(obj, labelProperty) as String
        result.add(labelProperty, JsonPrimitive(title))
        return result
    }
}
