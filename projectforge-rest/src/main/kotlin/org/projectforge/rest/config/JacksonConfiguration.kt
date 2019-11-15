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

package org.projectforge.rest.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.hibernate.proxy.AbstractLazyInitializer
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.json.*
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.calendar.ICalendarEventDeserializer
import org.projectforge.rest.calendar.TeamCalDOSerializer
import org.projectforge.rest.config.JacksonConfiguration.Companion.registerAllowedUnknownProperties
import org.projectforge.rest.json.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

/**
 * Base configuration of all Spring rest calls. Unknown properties not avoidable by the client might be registered through
 * [registerAllowedUnknownProperties]. For example PFUserDO.fullname is provided as service for the clients, but is
 * an unknown field by PFUserDO.
 */
@Configuration
open class JacksonConfiguration {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(JacksonConfiguration::class.java)

        private val allowedUnknownProperties = mutableMapOf<Class<*>, MutableSet<String>>()

        /**
         * Properties (field) sent by any client and unknown by the server will result in an exception and BAD_REQUEST.
         * In special cases you may add properties, which should be simply ignored.
         */
        fun registerAllowedUnknownProperties(clazz: Class<*>, vararg properties: String) {
            synchronized(allowedUnknownProperties) {
                val set = allowedUnknownProperties[clazz]
                if (set == null) {
                    allowedUnknownProperties[clazz] = mutableSetOf(*properties)
                } else {
                    set.addAll(properties)
                }
            }
        }

        init {
            registerAllowedUnknownProperties(PFUserDO::class.java, "fullname")
            registerAllowedUnknownProperties(KundeDO::class.java, "id")
            // reminderDuration* will be there after function switchToTimesheet is used:
            registerAllowedUnknownProperties(TimesheetDO::class.java, "reminderDuration", "reminderDurationUnit")
        }
    }

    @Value("\${projectforge.rest.json.allowUnkownProperties:true}")
    private var allowUnknownJsonProperties: Boolean = true

    private var objectMapper: ObjectMapper? = null

    @Bean
    open fun objectMapper(): ObjectMapper {
        if (objectMapper != null) {
            return objectMapper!!
        }
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule())
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        if (!allowUnknownJsonProperties) {
            log.info("Unknown JSON properties are not allowed in REST call, due to configuration in projectforge.properties:projectforge.rest.json.allowUnkownProperties (OK, but Rest calls may fail).")
        }
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, allowUnknownJsonProperties) // Should be true in development mode!
        //mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        val module = object : SimpleModule() {
            override fun setupModule(context: SetupContext) {
                super.setupModule(context)
                context.addDeserializationProblemHandler(object : DeserializationProblemHandler() {
                    override fun handleUnknownProperty(ctxt: DeserializationContext?, p: JsonParser?, deserializer: JsonDeserializer<*>?, beanOrClass: Any?, propertyName: String?): Boolean {
                        if (beanOrClass == null)
                            return false
                        val clazz = if (beanOrClass is Class<*>) beanOrClass else beanOrClass.javaClass
                        return allowedUnknownProperties[clazz]?.contains(propertyName) ?: false
                    }
                })
            }
        }
        module.addSerializer(LocalDate::class.java, LocalDateSerializer())
        module.addDeserializer(LocalDate::class.java, LocalDateDeserializer())

        module.addSerializer(PFDateTime::class.java, PFDateTimeSerializer())
        module.addDeserializer(PFDateTime::class.java, PFDateTimeDeserializer())

        module.addSerializer(java.util.Date::class.java, UtilDateSerializer(UtilDateFormat.JS_DATE_TIME_MILLIS))
        module.addDeserializer(java.util.Date::class.java, UtilDateDeserializer())

        module.addSerializer(Timestamp::class.java, TimestampSerializer(UtilDateFormat.JS_DATE_TIME_MILLIS))
        module.addDeserializer(Timestamp::class.java, TimestampDeserializer())

        module.addSerializer(java.sql.Date::class.java, SqlDateSerializer())
        module.addDeserializer(java.sql.Date::class.java, SqlDateDeserializer())

        module.addDeserializer(String::class.java, TextDeserializer())
        module.addDeserializer(java.lang.Integer::class.java, IntDeserializer())
        module.addDeserializer(BigDecimal::class.java, BigDecimalDeserializer())

        module.addSerializer(Kost1DO::class.java, Kost1DOSerializer())
        module.addSerializer(Kost2DO::class.java, Kost2DOSerializer())
        module.addSerializer(KundeDO::class.java, KundeDOSerializer())

        module.addSerializer(PFUserDO::class.java, PFUserDOSerializer())
        module.addDeserializer(PFUserDO::class.java, PFUserDODeserializer())

        module.addSerializer(GroupDO::class.java, GroupDOSerializer())
        module.addSerializer(TaskDO::class.java, TaskDOSerializer())
        module.addSerializer(TenantDO::class.java, TenantDOSerializer())
        module.addSerializer(AddressbookDO::class.java, AddressbookDOSerializer())
        module.addSerializer(EmployeeDO::class.java, EmployeeDOSerializer())

        // Calendar serializers
        module.addSerializer(TeamCalDO::class.java, TeamCalDOSerializer())
        module.addDeserializer(ICalendarEvent::class.java, ICalendarEventDeserializer())

        module.addSerializer(AbstractLazyInitializer::class.java, HibernateProxySerializer())

        mapper.registerModule(module)
        objectMapper = mapper
        return mapper
    }
}
