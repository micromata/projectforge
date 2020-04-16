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

package org.projectforge.framework

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.Hibernate
import org.hibernate.proxy.AbstractLazyInitializer
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.json.*
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


/**
 * Helper method to serialize objects as json strings and to use it in toString method.
 * @param obj Object to serialize as json string.
 * @param ignoreEmbeddedSerializers Most embedded objects of type [DefaultBaseDO] are serialized in short form (id and short info field).
 *        If this param constains a class of a [DefaultBaseDO], this object will be serialized with all fields.
 */
fun toJsonString(obj: Any, vararg ignoreEmbeddedSerializers: Class<out Any>): String {
    return ToStringUtil.toJsonString(obj, ignoreEmbeddedSerializers, null)
}

class ToStringUtil {
    class Serializer<T>(val clazz: Class<T>, val serializer: JsonSerializer<T>)
    /**
     * Helper class for having data classes with to json functionality (e. g. for logging).
     */
    open class ToJsonStringObject {
        override fun toString(): String {
            return toJsonString(this)
        }
    }
    companion object {
        private val log = LoggerFactory.getLogger(ToStringUtil::class.java)

        private val embeddedSerializerClasses = listOf(GroupDO::class.java, Kost1DO::class.java, Kost2DO::class.java, KundeDO::class.java, PFUserDO::class.java, ProjektDO::class.java, TaskDO::class.java)

        private val mapperMap = mutableMapOf<ObjectMapperKey, ObjectMapper>()

        /**
         * Helper method to serialize objects as json strings and to use it in toString method.
         * @param obj Object to serialize as json string.
         * @param ignoreEmbeddedSerializers Most embedded objects of type [DefaultBaseDO] are serialized in short form (id and short info field).
         *        If this param constains a class of a [DefaultBaseDO], this object will be serialized with all fields.
         */
        @JvmStatic
        fun toJsonString(obj: Any, vararg ignoreEmbeddedSerializers: Class<out Any>): String {
            return toJsonString(obj, ignoreEmbeddedSerializers, null)
        }

        /**
         * Helper method to serialize objects as json strings and to use it in toString method.
         * @param obj Object to serialize as json string.
         * @param ignoreEmbeddedSerializers Most embedded objects of type [DefaultBaseDO] are serialized in short form (id and short info field).
         *        If this param constains a class of a [DefaultBaseDO], this object will be serialized with all fields.
         */
        @JvmStatic
        fun toJsonStringExtended(obj: Any, vararg additionalSerializers: Serializer<Any>): String {
            return toJsonString(obj, null, additionalSerializers = additionalSerializers)
        }

        internal fun toJsonString(obj: Any, ignoreEmbeddedSerializers: Array<out Class<out Any>>?,
                                  additionalSerializers: Array<out Serializer<Any>>?): String {
            try {
                val mapper = getObjectMapper(obj::class.java, ignoreEmbeddedSerializers, additionalSerializers)
                return mapper.writeValueAsString(obj)
            } catch(ex: Exception) {
                val id = System.currentTimeMillis()
                log.error("Exception while serializing object of type '${obj::class.java.simpleName}' #$id: ${ex.message}", ex)
                return "[*** Exception while serializing object of type '${obj::class.java.simpleName}', see log files #$id for more details.]"
            }
        }

        private fun <T> register(module: SimpleModule, clazz: Class<T>, serializer: EmbeddedDOSerializer<T>, objClass: Class<*>, ignoreEmbeddedSerializers: Array<out Class<out Any>>?) {
            if (objClass.equals(clazz)) {
                return // Don't use embedded serializer for current object itself.
            }
            if (!ignoreEmbeddedSerializers.isNullOrEmpty()) {
                ignoreEmbeddedSerializers.forEach {
                    if (it == clazz) return
                }
            }
            module.addSerializer(clazz, serializer)
        }


        internal fun writeFields(jgen: JsonGenerator, id: Int?, fieldName: String, value: String?) {
            if (id == null) {
                jgen.writeNullField("id")
            } else {
                jgen.writeNumberField("id", id)
            }
            if (value != null) {
                jgen.writeStringField(fieldName, value)
            }
        }

        private fun getObjectMapper(objClass: Class<*>?, ignoreEmbeddedSerializers: Array<out Class<out Any>>?,
                                    additionalSerializers: Array<out Serializer<Any>>?): ObjectMapper {
            val key = if (objClass != null && embeddedSerializerClasses.any { it.isAssignableFrom(objClass) }) {
                ObjectMapperKey(objClass, ignoreEmbeddedSerializers, additionalSerializers)
            } else {
                ObjectMapperKey(null, ignoreEmbeddedSerializers, additionalSerializers)
            }
            var mapper = mapperMap[key]
            if (mapper != null) {
                return mapper
            }
            mapper = ObjectMapper()
            mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            // mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
            val module = SimpleModule()
            module.addSerializer(java.util.Date::class.java, UtilDateSerializer(UtilDateFormat.ISO_DATE_TIME_SECONDS))
            module.addSerializer(Timestamp::class.java, TimestampSerializer(UtilDateFormat.ISO_DATE_TIME_MILLIS))
            module.addSerializer(java.sql.Date::class.java, SqlDateSerializer())
            module.addSerializer(LocalDate::class.java, LocalDateSerializer())
            module.addSerializer(LocalTime::class.java, LocalTimeSerializer())
            module.addSerializer(TenantDO::class.java, TenantSerializer())
            module.addSerializer(AddressbookDO::class.java, AddressbookSerializer())
            module.addSerializer(AbstractLazyInitializer::class.java, HibernateProxySerializer())

            additionalSerializers?.forEach {
                module.addSerializer(it.clazz, it.serializer)
            }
            if (objClass != null) {
                register(module, GroupDO::class.java, GroupSerializer(), objClass, ignoreEmbeddedSerializers)
                register(module, Kost1DO::class.java, Kost1Serializer(), objClass, ignoreEmbeddedSerializers)
                register(module, Kost2DO::class.java, Kost2Serializer(), objClass, ignoreEmbeddedSerializers)
                register(module, KundeDO::class.java, KundeSerializer(), objClass, ignoreEmbeddedSerializers)
                register(module, PFUserDO::class.java, UserSerializer(), objClass, ignoreEmbeddedSerializers)
                register(module, EmployeeDO::class.java, EmployeeSerializer(), objClass, ignoreEmbeddedSerializers)
                register(module, ProjektDO::class.java, ProjektSerializer(), objClass, ignoreEmbeddedSerializers)
                register(module, TaskDO::class.java, TaskSerializer(), objClass, ignoreEmbeddedSerializers)
            }
            mapper.registerModule(module)
            mapper.registerModule(KotlinModule())
            mapperMap[key] = mapper
            return mapper
        }
    }

    abstract class EmbeddedDOSerializer<T>(val clazz: Class<T>) : StdSerializer<T>(clazz) {
        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(value: T?, jgen: JsonGenerator, provider: SerializerProvider) {
            if (value == null) {
                jgen.writeNull()
                return
            }
            jgen.writeStartObject();
            writeFields(jgen, value, Hibernate.isInitialized(value))
            jgen.writeEndObject()
        }

        abstract fun writeFields(jgen: JsonGenerator, value: T, initialized: Boolean)
    }

    class UserSerializer : EmbeddedDOSerializer<PFUserDO>(PFUserDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: PFUserDO, initialized: Boolean) {
            val username = if (initialized) value.username else TenantRegistryMap.getInstance().tenantRegistry.userGroupCache.getUsername(value.id)
            writeFields(jgen, value.id, "username", username)
        }
    }

    class CalendarSerializer : EmbeddedDOSerializer<TeamCalDO>(TeamCalDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: TeamCalDO, initialized: Boolean) {
            writeFields(jgen, value.id, "title", value.title)
        }
    }

    class GroupSerializer : EmbeddedDOSerializer<GroupDO>(GroupDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: GroupDO, initialized: Boolean) {
            writeFields(jgen, value.id, "name", value.name)
        }
    }

    class TaskSerializer : EmbeddedDOSerializer<TaskDO>(TaskDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: TaskDO, initialized: Boolean) {
            writeFields(jgen, value.id, "path", TaskTreeHelper.getTaskTree().getTaskNodeById(value.id)?.pathAsString)
        }
    }

    class Kost1Serializer : EmbeddedDOSerializer<Kost1DO>(Kost1DO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: Kost1DO, initialized: Boolean) {
            writeFields(jgen, value.id, "number", if (initialized) value.formattedNumber else null)
        }
    }

    class Kost2Serializer : EmbeddedDOSerializer<Kost2DO>(Kost2DO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: Kost2DO, initialized: Boolean) {
            writeFields(jgen, value.id, "number", if (initialized) value.formattedNumber else null)
        }
    }

    class EmployeeSerializer : EmbeddedDOSerializer<EmployeeDO>(EmployeeDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: EmployeeDO, initialized: Boolean) {
            writeFields(jgen, value.id, "name", if (initialized) value.user?.getFullname() else null)
        }
    }

    class ProjektSerializer : EmbeddedDOSerializer<ProjektDO>(ProjektDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: ProjektDO, initialized: Boolean) {
            writeFields(jgen, value.id, "name", if (initialized) value.name else null)
        }
    }

    class KundeSerializer : EmbeddedDOSerializer<KundeDO>(KundeDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: KundeDO, initialized: Boolean) {
            writeFields(jgen, value.nummer, "name", if (initialized) value.name else null)
        }
    }

    class TenantSerializer : EmbeddedDOSerializer<TenantDO>(TenantDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: TenantDO, initialized: Boolean) {
            writeFields(jgen, value.id, "name", if (initialized) value.name else null)
        }
    }

    class AddressbookSerializer : EmbeddedDOSerializer<AddressbookDO>(AddressbookDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: AddressbookDO, initialized: Boolean) {
            writeFields(jgen, value.id, "title", if (initialized) value.title else null)
        }
    }

    class SqlDateSerializer : StdSerializer<java.sql.Date>(java.sql.Date::class.java) {
        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(value: java.sql.Date?, jgen: JsonGenerator, provider: SerializerProvider) {
            if (value == null) {
                jgen.writeNull()
                return
            }
            jgen.writeString(formatter.format(value.toLocalDate()))
        }

        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    }

    private class ObjectMapperKey(var objClass: Class<*>?,
                                  var ignoreEmbeddedSerializers: Array<out Class<out Any>>?,
                                  var additionalSerializers: Array<out Serializer<Any>>?) {
        override fun equals(other: Any?): Boolean {
            other as ObjectMapperKey
            return EqualsBuilder()
                    .append(this.objClass, other.objClass)
                    .append(this.ignoreEmbeddedSerializers, other.ignoreEmbeddedSerializers)
                    .append(this.additionalSerializers, other.additionalSerializers)
                    .isEquals
        }

        override fun hashCode(): Int {
            return HashCodeBuilder()
                    .append(objClass)
                    .append(ignoreEmbeddedSerializers)
                    .append(additionalSerializers)
                    .toHashCode()
        }
    }
}
