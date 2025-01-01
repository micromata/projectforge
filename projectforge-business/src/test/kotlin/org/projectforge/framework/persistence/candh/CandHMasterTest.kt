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

package org.projectforge.framework.persistence.candh

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.user.entities.Gender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimeNotation
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.TestSetup
import java.time.DayOfWeek

class CandHMasterTest: AbstractTestBase() {
    @Test
    fun baseTests() {
        var src = newSrcUser()
        src.gender = Gender.FEMALE
        src.email = "abc@acme.com"
        src.timeNotation = TimeNotation.H24
        src.firstDayOfWeek = DayOfWeek.MONDAY

        Assertions.assertEquals(EntityCopyStatus.NONE, CandHMaster.copyValues(src, newDestUser()).currentCopyStatus)
        src.gender = Gender.MALE
        Assertions.assertEquals(EntityCopyStatus.MAJOR, CandHMaster.copyValues(src, newDestUser()).currentCopyStatus)
        src = newSrcUser()
        src.isMinorChange = true
        Assertions.assertEquals(EntityCopyStatus.MINOR, CandHMaster.copyValues(src, newDestUser()).currentCopyStatus, "Field 'isMinorChange' isn't persisted.")
    }

    private fun newSrcUser(): PFUserDO {
        val src = PFUserDO()
        src.gender = Gender.FEMALE
        src.email = "abc@acme.com"
        src.timeNotation = TimeNotation.H24
        src.firstDayOfWeek = DayOfWeek.MONDAY
        return src
    }

    // must be created always new, because copyValues will modify the object.
    private fun newDestUser(): PFUserDO {
        val dest = PFUserDO()
        dest.gender = Gender.FEMALE
        dest.email = "abc@acme.com"
        dest.timeNotation = TimeNotation.H24
        dest.firstDayOfWeek = DayOfWeek.MONDAY
        return dest
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
