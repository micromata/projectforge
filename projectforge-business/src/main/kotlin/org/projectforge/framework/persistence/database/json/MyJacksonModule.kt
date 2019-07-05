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

package org.projectforge.framework.persistence.database.json

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import org.projectforge.framework.persistence.entities.AbstractBaseDO

/**
 * WIP: Trying to export and import whole database as json (for db migration and for creating test data).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MyJacksonModule: SimpleModule() {
    private val objectIdRegistry = ObjectIdRegistry()

    override fun setupModule(context: Module.SetupContext) {
        super.setupModule(context)
        context.addBeanSerializerModifier(object : BeanSerializerModifier() {
            override fun modifySerializer(
                    config: SerializationConfig, desc: BeanDescription, serializer: JsonSerializer<*>): JsonSerializer<*> {
                return if (AbstractBaseDO::class.java.isAssignableFrom(desc.getBeanClass())) {
                    AbstractBaseDOSerializer(serializer as JsonSerializer<Any>, objectIdRegistry)
                } else serializer
            }
        })
        context.addBeanDeserializerModifier(object : BeanDeserializerModifier() {
            override fun modifyDeserializer(config: DeserializationConfig?, beanDesc: BeanDescription?, deserializer: JsonDeserializer<*>?): JsonDeserializer<*> {
                return super.modifyDeserializer(config, beanDesc, deserializer)
            }
        })
    }
}
