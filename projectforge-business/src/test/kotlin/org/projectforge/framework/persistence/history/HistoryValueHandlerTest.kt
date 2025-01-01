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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.business.test.TestSetup
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

class HistoryValueHandlerTest {
    @Test
    fun testHandler() {
        BooleanHistoryValueHandler().let { handler ->
            Assertions.assertNull(handler.deserialize("NULL"))
            Assertions.assertEquals(false, handler.deserialize("dsfa"))
            Assertions.assertEquals("true", handler.serialize(true))
            Assertions.assertEquals("false", handler.serialize(false))
            Assertions.assertEquals(true, handler.deserialize("true"))
            Assertions.assertEquals(false, handler.deserialize("false"))
            Assertions.assertNull(handler.deserialize("NULL"))
            Assertions.assertEquals("Ja", handler.format(true))
            deserializeAndSerializeTest("true", handler)
            deserializeAndSerializeTest("false", handler)
        }
        // date is CEST, so UTC is 2 hours behind.
        val date = PFDateTime.withDate(2024, Month.OCTOBER, 5, 10, 27, 13)
        DateHistoryValueHandler().let { handler ->
            Assertions.assertEquals("2024-10-05 08:27:13", handler.serialize(date.utilDate))
            Assertions.assertEquals("05.10.2024 10:27", handler.format(date.utilDate)) // CEST
            deserializeAndSerializeTest("2006-11-02 14:59:59.000", handler, "2006-11-02 14:59:59")
        }
        TimestampHistoryValueHandler().let { handler ->
            Assertions.assertEquals("2024-10-05 08:27:13.000", handler.serialize(date.sqlTimestamp))
            deserializeAndSerializeTest("2008-03-19 10:53:55.481", handler)
            deserializeAndSerializeTest("2007-04-22 17:00:00.0", handler, "2007-04-22 17:00:00.000")
            deserializeAndSerializeTest("2005-04-13 13:30:00.0", handler, "2005-04-13 13:30:00.000")
        }
        LocalDateHistoryValueHandler().let { handler ->
            val localDate = LocalDate.of(2024, Month.OCTOBER, 5)
            Assertions.assertEquals("2024-10-05", handler.serialize(localDate))
            Assertions.assertEquals("05.10.2024", handler.format(localDate)) // CEST
            deserializeAndSerializeTest("2017-03-16 23:00:00:000", handler, "2017-03-16")
            deserializeAndSerializeTest("2014-10-16 10:48:41:358", handler, "2014-10-16")
            deserializeAndSerializeTest("2005-04-13 13:30:00.0", handler, "2005-04-13")
        }
        SqlDateHistoryValueHandler().let { handler ->
            val sqlDate = PFDay.withDate(2024, Month.OCTOBER, 5).sqlDate
            Assertions.assertEquals("2024-10-05", handler.serialize(sqlDate))
            Assertions.assertEquals("05.10.2024", handler.format(sqlDate)) // CEST
            deserializeAndSerializeTest("2017-03-16 23:00:00:000", handler, "2017-03-16")
            deserializeAndSerializeTest("2014-10-16 10:48:41:358", handler, "2014-10-16")
            deserializeAndSerializeTest("2005-04-13 13:30:00.0", handler, "2005-04-13")
        }
        IntHistoryValueHandler().let { handler ->
            Assertions.assertEquals("42", handler.serialize(42))
            Assertions.assertEquals(42, handler.deserialize("42"))
            deserializeAndSerializeTest("42", handler)
            deserializeAndSerializeTest("-10", handler)
            deserializeAndSerializeTest("0", handler)
            Assertions.assertEquals("10.000", handler.format(10000))
        }
        ShortHistoryValueHandler().let { handler ->
            Assertions.assertEquals("42", handler.serialize(42))
            Assertions.assertEquals(42, handler.deserialize("42"))
            deserializeAndSerializeTest("42", handler)
            deserializeAndSerializeTest("-10", handler)
            deserializeAndSerializeTest("0", handler)
            Assertions.assertEquals("10.000", handler.format(10000))
        }
        LongHistoryValueHandler().let { handler ->
            Assertions.assertEquals("42", handler.serialize(42))
            Assertions.assertEquals(42, handler.deserialize("42"))
            deserializeAndSerializeTest("42", handler)
            deserializeAndSerializeTest("-10", handler)
            deserializeAndSerializeTest("0", handler)
            Assertions.assertEquals("10.000", handler.format(10000))
        }
        BigDecimalHistoryValueHandler().let { handler ->
            Assertions.assertEquals("42", handler.serialize(BigDecimal("42")))
            Assertions.assertEquals(BigDecimal("42"), handler.deserialize("42"))
            deserializeAndSerializeTest("42.5", handler)
            deserializeAndSerializeTest("-10.98", handler)
            deserializeAndSerializeTest("0", handler)
            deserializeAndSerializeTest("0.00", handler)
            Assertions.assertEquals("10.000,17", handler.format(BigDecimal("10000.17")))
        }
        ByteArrayHistoryValueHandler().let { handler ->
            Assertions.assertEquals("byte[42]", handler.serialize(ByteArray(42)))
            Assertions.assertNull(handler.deserialize("42"))
            Assertions.assertEquals("[...]", handler.format(ByteArray(42)))
        }
        LocaleHistoryValueHandler().let { handler ->
            Assertions.assertEquals("de", handler.serialize(Locale.GERMAN))
            deserializeAndSerializeTest("de_AT", handler, "de_at")
            deserializeAndSerializeTest("de_DE", handler, "de_de")
            deserializeAndSerializeTest("default", handler)
            deserializeAndSerializeTest("de_LU", handler, "de_lu")
            deserializeAndSerializeTest("en", handler)
            deserializeAndSerializeTest("en_GB", handler, "en_gb")
            deserializeAndSerializeTest("en_US", handler, "en_us")
        }
    }

    private fun <T> deserializeAndSerializeTest(
        value: String, handler: HistoryValueHandler<T>, result: String? = null
    ) {
        val deserialized = handler.deserialize(value)
        val serialized = handler.serialize(deserialized!!)
        Assertions.assertEquals(result ?: value, serialized)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
