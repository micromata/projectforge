/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.spi.FilterReply
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MassIndexingLeakWarningFilterTest {
    private val context = LoggerContext()
    private val filter = MassIndexingLeakWarningFilter()
    private val hikariLogger = context.getLogger(MassIndexingLeakWarningFilter.HIKARI_LEAK_LOGGER)

    @Test
    fun `leak warnings of the mass indexer threads are dropped`() {
        assertEquals(
            FilterReply.DENY,
            filter.decide(
                null, hikariLogger, Level.WARN, LEAK_FORMAT,
                arrayOf("conn1", "Hibernate Search - Mass indexing - TimesheetDO - Entity loading - 2", Exception()), null,
            ),
        )
        assertEquals(
            FilterReply.DENY,
            filter.decide(
                null, hikariLogger, Level.INFO, RETURNED_FORMAT,
                arrayOf("conn1", "Hibernate Search - Mass indexing - TimesheetDO - ID loading - 0"), null,
            ),
        )
    }

    @Test
    fun `leak warnings of other threads are kept`() {
        assertEquals(
            FilterReply.NEUTRAL,
            filter.decide(null, hikariLogger, Level.WARN, LEAK_FORMAT, arrayOf("conn1", "http-nio-8080-exec-1", Exception()), null),
        )
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, hikariLogger, Level.WARN, "Without params", null, null))
    }

    @Test
    fun `other loggers are not affected`() {
        assertEquals(
            FilterReply.NEUTRAL,
            filter.decide(
                null, context.getLogger("org.projectforge.Test"), Level.WARN, "{}",
                arrayOf("Hibernate Search - Mass indexing - TimesheetDO"), null,
            ),
        )
    }

    companion object {
        private const val LEAK_FORMAT = "Connection leak detection triggered for {} on thread {}, stack trace follows"
        private const val RETURNED_FORMAT = "Previously reported leaked connection {} on thread {} was returned to the pool (unleaked)"
    }
}
