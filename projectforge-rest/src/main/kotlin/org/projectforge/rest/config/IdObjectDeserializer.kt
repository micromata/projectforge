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
import org.projectforge.common.BeanHelper
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.rest.dto.BaseDTO

/**
 * DTO objects or DO's may be deserialized only by id or as full objects. This deserializer handles both:
 * * ```{'project': 863228,...}```, or
 * * ```{'project': {'id': 863228, 'name':{'ProjectForge'}...}```
 *
 * @see [create] for further requirements.
 * @author Kai Reinhard
 */
class IdObjectDeserializer<T>
@JvmOverloads
constructor(private val defaultDeserialize: JsonDeserializer<*>, private var beanCls: Class<T>? = null)
    : DelegatingDeserializer(defaultDeserialize) {

    override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>): JsonDeserializer<*> {
        val instance = this::class.java.getDeclaredConstructor(JsonDeserializer::class.java).newInstance(defaultDeserialize)
        instance.beanCls = beanCls
        return instance
    }

    /**
     * Creation of instance works if:
     * * [beanCls] provides a constructor with exactly one int parameter, or
     * * [beanCls] provides a default constructor and has a setter method 'id' of type [Int].
     */
    fun create(id: Int): T {
        beanCls?.let { cls ->
            cls.declaredConstructors.forEach { constructor ->
                if (constructor.parameterCount == 1 && constructor.parameterTypes == Int::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return constructor.newInstance(id) as T
                }
            }
            val instance = cls.getDeclaredConstructor().newInstance()
            BeanHelper.setProperty(instance, "id", id)
            return instance
        }
        throw UnsupportedOperationException("Can't create instance of ${beanCls?.name} with id $id")
    }

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
