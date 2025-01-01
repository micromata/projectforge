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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateCompatibilityUtils
import org.projectforge.business.test.TestSetup
import java.time.DayOfWeek

class ThreadLocalUserContextTest {

    @Test
    fun firstDayOfWeekTest() {
        Assertions.assertEquals(1, ThreadLocalUserContext.firstDayOfWeekValue)
        Assertions.assertEquals(DayOfWeek.MONDAY, ThreadLocalUserContext.firstDayOfWeek)
        Assertions.assertEquals(1, PFDateCompatibilityUtils.getCompatibilityDayOfWeekSunday0Value(ThreadLocalUserContext.firstDayOfWeek))
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
