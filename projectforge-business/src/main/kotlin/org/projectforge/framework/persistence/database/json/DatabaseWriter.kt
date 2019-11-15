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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.projectforge.ProjectForgeVersion
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.json.TimestampSerializer
import org.projectforge.framework.json.UtilDateFormat
import org.projectforge.framework.json.UtilDateSerializer
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.framework.time.PFDateTime
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import java.sql.Timestamp

/**
 * WIP: Trying to export and import whole database as json (for db migration and for creating test data).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class DatabaseWriter(val emf: PfEmgrFactory,
                     val sessionFactory: SessionFactory) {
    private val log = LoggerFactory.getLogger(DatabaseWriter::class.java)

    private lateinit var writer: Writer

    /**
     * @param out
     */
    fun dump(out: OutputStream?) {
        val jfactory = JsonFactory()
        val jgen = jfactory.createGenerator(out, JsonEncoding.UTF8)
        jgen.codec = getObjectMapper()
        jgen.writeStartObject();
        jgen.writeStringField("date", PFDateTime.now().isoString)
        jgen.writeStringField("app", ProjectForgeVersion.APP_ID)
        jgen.writeStringField("version", ProjectForgeVersion.VERSION_STRING)
        jgen.writeStringField("user", ThreadLocalUserContext.getUser().username)
        jgen.writeArrayFieldStart("database")
        val session = sessionFactory.openSession()
        session.use {
            dump(TenantDO::class.java, session, jgen)
            dump(PFUserDO::class.java, session, jgen)
            val entities = emf.metadataRepository.tableEntities
            for (entity in entities) {
                val entityClass = entity.javaType
                //val entitySimpleName = entityClass.simpleName
                //val entityType = entityClass.name
                log.info("entityClass: ${entityClass.name}")
            }
        }
        jgen.writeEndArray()
        jgen.writeEndObject()
        jgen.flush()
    }

    @Suppress("UNUSED_PARAMETER")
    fun restore(inputStream: InputStream) {
        //createObjectMapper().readValue(in, )
    }

    private fun getObjectMapper(): ObjectMapper {
        if (objectMapper != null) {
            return objectMapper!!
        }
        val mapper = ObjectMapper()
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
        val module = MyJacksonModule()
        module.addSerializer(java.util.Date::class.java, UtilDateSerializer(UtilDateFormat.ISO_DATE_TIME_SECONDS))
        module.addSerializer(Timestamp::class.java, TimestampSerializer(UtilDateFormat.ISO_DATE_TIME_MILLIS))
        module.addSerializer(java.sql.Date::class.java, ToStringUtil.SqlDateSerializer())
        mapper.registerModule(module)
        mapper.registerModule(KotlinModule())
        objectMapper = mapper
        return mapper
    }

    private fun dump(entityClass: Class<*>, session: Session, jgen: JsonGenerator) {
        val sql = "select o from ${entityClass.name} o"
        val list = session.createQuery(sql).setReadOnly(true).list()
        if (list.isEmpty()) return
        jgen.writeStartObject();
        jgen.writeArrayFieldStart("${entityClass.simpleName}")
        list.forEach {
            var obj = it!!
            //var type = it::class.java
            //if (it is TenantDO) {
            //    obj = Tenant(it.pk, it.created, it.lastUpdate, it.shortName, it.name, it.description, it.defaultTenant)
            //}
            jgen.writeObject(obj)
        }
        jgen.writeEndArray()
        jgen.writeEndObject()
    }

    companion object {
        private var objectMapper: ObjectMapper? = null
    }
}
