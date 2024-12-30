/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import mu.KotlinLogging
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.proxy.AbstractLazyInitializer
import org.projectforge.business.PfCaches
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.json.*
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import java.io.IOException
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


/**
 * Helper method to serialize objects as json strings and to use it in toString method.
 * @param obj Object to serialize as json string.
 * @param preferEmbeddedSerializers If true [IdOnlySerializer] and [IdsOnlySerializer] uses configured serializers instead of writing id only. Default is false.
 * @param ignoreIdOnlySerializers If true [IdOnlySerializer] and [IdsOnlySerializer] are ignored. Default is false.
 */
fun toJsonString(
    obj: Any,
    preferEmbeddedSerializers: Boolean = true,
    ignoreIdOnlySerializers: Boolean = false,
): String {
    return ToStringUtil.toJsonString(
        obj,
        ToStringUtil.Configuration(
            preferEmbeddedSerializers = preferEmbeddedSerializers,
            ignoreIdOnlySerializers = ignoreIdOnlySerializers,
        )
    )
}

private val log = KotlinLogging.logger {}

class ToStringUtil {
    class Serializer<T>(val clazz: Class<T>, val serializer: JsonSerializer<T>)

    class Configuration(
        var preferEmbeddedSerializers: Boolean = false,
        var ignoreIdOnlySerializers: Boolean = false,
        val additionalSerializers: Array<out Serializer<Any>>? = null
    )

    /**
     * Helper class for having data classes with to json functionality (e. g. for logging).
     */
    open class ToJsonStringObject {
        override fun toString(): String {
            return toJsonString(this)
        }
    }

    companion object {
        private val embeddedSerializerClasses = listOf(
            GroupDO::class.java,
            Kost1DO::class.java,
            Kost2DO::class.java,
            KundeDO::class.java,
            PFUserDO::class.java,
            ProjektDO::class.java,
            TaskDO::class.java
        )

        private val mapperMap = mutableMapOf<ObjectMapperKey, ObjectMapper>()


        /**
         * Helper method to serialize objects as json strings and to use it in toString method.
         * @param obj Object to serialize as json string.
         */
        @JvmStatic
        fun toJsonString(obj: Any): String {
            return toJsonString(obj, Configuration())
        }

        /**
         * Helper method to serialize objects as json strings and to use it in toString method.
         * @param obj Object to serialize as json string.
         * @param ignoreEmbeddedSerializers Most embedded objects of type [DefaultBaseDO] are serialized in short form (id and short info field).
         *        If this param contains a class of a [DefaultBaseDO], this object will be serialized with all fields.
         */
        @JvmStatic
        fun toJsonStringExtended(obj: Any, vararg additionalSerializers: Serializer<Any>): String {
            return toJsonString(obj, Configuration(additionalSerializers = additionalSerializers))
        }

        internal fun toJsonString(
            obj: Any,
            configuration: Configuration,
        ): String {
            try {
                val mapper = getObjectMapper(obj::class.java, configuration)
                if (configuration.preferEmbeddedSerializers || configuration.ignoreIdOnlySerializers) {
                    JsonThreadLocalContext.set(
                        preferEmbeddedSerializers = configuration.preferEmbeddedSerializers,
                        ignoreIdOnlySerializers = configuration.ignoreIdOnlySerializers,
                    )
                }
                return mapper.writeValueAsString(obj)
            } catch (ex: Exception) {
                val id = System.currentTimeMillis()
                log.error(
                    "Exception while serializing object of type '${obj::class.java.simpleName}' #$id: ${ex.message}",
                    ex
                )
                return "[*** Exception while serializing object of type '${obj::class.java.simpleName}', see log files #$id for more details.]"
            } finally {
                JsonThreadLocalContext.clear()
            }
        }

        private fun <T> register(
            module: SimpleModule,
            clazz: Class<T>,
            serializer: EmbeddedDOSerializer<T>,
            objClass: Class<*>,
        ) {
            if (objClass.equals(clazz)) {
                return // Don't use embedded serializer for current object itself.
            }
            module.addSerializer(clazz, serializer)
        }


        internal fun writeFields(jgen: JsonGenerator, id: Long?, fieldName: String, value: String?) {
            if (id == null) {
                jgen.writeNullField("id")
            } else {
                jgen.writeNumberField("id", id)
            }
            if (value != null) {
                jgen.writeStringField(fieldName, value)
            }
        }

        private fun getObjectMapper(
            objClass: Class<*>?,
            configuration: Configuration,
        ): ObjectMapper {
            val key = if (objClass != null && embeddedSerializerClasses.any { it.isAssignableFrom(objClass) }) {
                ObjectMapperKey(objClass, configuration)
            } else {
                ObjectMapperKey(null, configuration)
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
            module.addSerializer(PFDateTime::class.java, PFDateTimeSerializer())
            module.addDeserializer(PFDateTime::class.java, PFDateTimeDeserializer())
            module.addSerializer(AddressbookDO::class.java, AddressbookSerializer())
            module.addSerializer(AbstractLazyInitializer::class.java, HibernateProxySerializer())

            configuration.additionalSerializers?.forEach {
                module.addSerializer(it.clazz, it.serializer)
            }
            if (objClass != null) {
                register(module, GroupDO::class.java, GroupSerializer(), objClass)
                register(module, Kost1DO::class.java, Kost1Serializer(), objClass)
                register(module, Kost2DO::class.java, Kost2Serializer(), objClass)
                register(module, KundeDO::class.java, KundeSerializer(), objClass)
                register(module, PFUserDO::class.java, UserSerializer(), objClass)
                register(module, EmployeeDO::class.java, EmployeeSerializer(), objClass)
                register(module, ProjektDO::class.java, ProjektSerializer(), objClass)
                register(module, TaskDO::class.java, TaskSerializer(), objClass)
            }
            mapper.registerModule(module)
            mapper.registerModule(KotlinModule.Builder().build())
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
            writeFields(jgen, value)
            jgen.writeEndObject()
        }

        abstract fun writeFields(jgen: JsonGenerator, value: T)
    }

    class UserSerializer : EmbeddedDOSerializer<PFUserDO>(PFUserDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: PFUserDO) {
            val user = PfCaches.instance.getUserIfNotInitialized(value)
            writeFields(jgen, value.id, "username", user?.username ?: "???")
        }
    }

    class CalendarSerializer : EmbeddedDOSerializer<TeamCalDO>(TeamCalDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: TeamCalDO) {
            val cal = PfCaches.instance.getTeamCalIfNotInitialized(value)
            writeFields(jgen, value.id, "title", cal?.title ?: "???")
        }
    }

    class GroupSerializer : EmbeddedDOSerializer<GroupDO>(GroupDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: GroupDO) {
            val group = PfCaches.instance.getGroupIfNotInitialized(value)
            writeFields(jgen, value.id, "name", group?.name ?: "???")
        }
    }

    class TaskSerializer : EmbeddedDOSerializer<TaskDO>(TaskDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: TaskDO) {
            writeFields(jgen, value.id, "path", TaskTree.instance.getTaskNodeById(value.id)?.pathAsString)
        }
    }

    class Kost1Serializer : EmbeddedDOSerializer<Kost1DO>(Kost1DO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: Kost1DO) {
            val kost1 = PfCaches.instance.getKost1IfNotInitialized(value)
            writeFields(jgen, value.id, "number", kost1?.formattedNumber ?: "???")
        }
    }

    class Kost2Serializer : EmbeddedDOSerializer<Kost2DO>(Kost2DO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: Kost2DO) {
            val kost2 = PfCaches.instance.getKost2IfNotInitialized(value)
            writeFields(jgen, value.id, "number", kost2?.formattedNumber ?: "???")
        }
    }

    class EmployeeSerializer : EmbeddedDOSerializer<EmployeeDO>(EmployeeDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: EmployeeDO) {
            val user = PfCaches.instance.getUserIfNotInitialized(value.user)
            writeFields(jgen, value.id, "name", user?.getFullname() ?: "???")
        }
    }

    class ProjektSerializer : EmbeddedDOSerializer<ProjektDO>(ProjektDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: ProjektDO) {
            val projekt = PfCaches.instance.getProjektIfNotInitialized(value)
            writeFields(jgen, value.id, "name", projekt?.name ?: "???")
        }
    }

    class KundeSerializer : EmbeddedDOSerializer<KundeDO>(KundeDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: KundeDO) {
            val kunde = PfCaches.instance.getKundeIfNotInitialized(value)
            writeFields(jgen, value.nummer, "name", kunde?.name ?: "???")
        }
    }

    class AddressbookSerializer : EmbeddedDOSerializer<AddressbookDO>(AddressbookDO::class.java) {
        override fun writeFields(jgen: JsonGenerator, value: AddressbookDO) {
            val ab = PfCaches.instance.getAddressbookIfNotInitialized(value)
            writeFields(jgen, value.id, "title", ab?.title ?: "???")
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

    private class ObjectMapperKey(
        var objClass: Class<*>?,
        val configuration: Configuration,
    ) {
        override fun equals(other: Any?): Boolean {
            other as ObjectMapperKey
            return EqualsBuilder()
                .append(this.objClass, other.objClass)
                //.append(this.configuration.preferEmbeddedSerializers, other.configuration.preferEmbeddedSerializers)
                .append(this.configuration.additionalSerializers, other.configuration.additionalSerializers)
                .isEquals
        }

        override fun hashCode(): Int {
            return HashCodeBuilder()
                .append(objClass)
                //.append(configuration.preferEmbeddedSerializers)
                .append(configuration.additionalSerializers)
                .toHashCode()
        }
    }
}
