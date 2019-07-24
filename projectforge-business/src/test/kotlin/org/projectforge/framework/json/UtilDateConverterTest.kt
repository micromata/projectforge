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

package org.projectforge.framework.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import java.time.LocalDateTime
import java.time.Month
import java.util.*

class UtilDateConverterTest {

    @Test
    fun convertJson() {
        val date = PFDateTime.from(LocalDateTime.of(2019, Month.JUNE, 26, 0, 44))!!.utilDate
        assertEquals("\"2019-06-26 00:44:00\"", createMapper(UtilDateFormat.ISO_DATE_TIME_SECONDS).writeValueAsString(date))
        assertEquals("\"2019-06-26 00:44:00.000\"", createMapper(UtilDateFormat.ISO_DATE_TIME_MILLIS).writeValueAsString(date))
        assertEquals("\"2019-06-26T00:44:00.000Z\"", createMapper(UtilDateFormat.JS_DATE_TIME_MILLIS).writeValueAsString(date))

        assertEquals(date.time, createMapper(UtilDateFormat.ISO_DATE_TIME_SECONDS).readValue<Date>("\"2019-06-26 00:44:00\"", Date::class.java).time)
        assertEquals(date.time, createMapper(UtilDateFormat.ISO_DATE_TIME_MILLIS).readValue<Date>("\"2019-06-26 00:44:00.000\"", Date::class.java).time)
        assertEquals(date.time, createMapper(UtilDateFormat.JS_DATE_TIME_MILLIS).readValue<Date>("\"2019-06-26T00:44:00.000Z\"", Date::class.java).time)
    }

    fun createMapper(format : UtilDateFormat): ObjectMapper {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addSerializer(java.util.Date::class.java, UtilDateSerializer(format))
        module.addDeserializer(java.util.Date::class.java, UtilDateDeserializer(format))
        mapper.registerModule(module)
        return mapper
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigXml.createForJunitTests()
            val user = PFUserDO()
            user.setTimeZone(TimeZone.getTimeZone("UTC"))
            ThreadLocalUserContext.setUserContext(UserContext(user, null))
        }
    }
}
