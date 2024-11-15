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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.test.TestSetup
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

class HistoryValueHandlerRegistryTest {
    @Test
    fun testRegistry() {
        HistoryValueHandlerRegistry.getHandler("boolean").let {
            Assertions.assertTrue(it is BooleanHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("int").let {
            Assertions.assertTrue(it is IntHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.lang.Integer").let {
            Assertions.assertTrue(it is IntHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("long").let {
            Assertions.assertTrue(it is LongHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.lang.Long").let {
            Assertions.assertTrue(it is LongHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.util.Date").let {
            Assertions.assertTrue(it is DateHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.sql.Date").let {
            Assertions.assertTrue(it is SqlDateHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.time.LocalDate").let {
            Assertions.assertTrue(it is LocalDateHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.sql.Timestamp").let {
            Assertions.assertTrue(it is TimestampHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("[B").let {
            Assertions.assertTrue(it is ByteArrayHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.util.Locale").let {
            Assertions.assertTrue(it is LocaleHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("void").let {
            Assertions.assertTrue(it is VoidHistoryValueHandler)
        }
        HistoryValueHandlerRegistry.getHandler("java.lang.String").let {
            Assertions.assertTrue(it is DefaultHistoryValueHandler)
        }
    }
}
