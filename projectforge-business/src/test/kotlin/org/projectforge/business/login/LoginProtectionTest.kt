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
package org.projectforge.business.login

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.login.LoginProtection.Companion.instance

class LoginProtectionTest {
    @Test
    @Throws(InterruptedException::class)
    fun testLoginProtection() {
        val current = System.currentTimeMillis()
        val lp = instance().mapByUserString
        lp.clearAll()
        Assertions.assertEquals(0, lp.sizeOfLastFailedLoginMap, "Maps should be empty.")
        Assertions.assertEquals(0, lp.sizeOfLoginFailedAttemptsMap, "Maps should be empty.")
        lp.incrementFailedLoginTimeOffset("kai")
        lp.incrementFailedLoginTimeOffset("kai")
        lp.incrementFailedLoginTimeOffset("kai")
        Assertions.assertTrue(lp.getFailedLoginTimeOffsetIfExists("kai") > 0,
                "Time offset due to 3 failed logins expected.")
        Assertions.assertTrue(lp.getFailedLoginTimeOffsetIfExists("kai") < 3001,
                "Time offset due to 3 failed logins not more than 3 seconds expected.")
        Assertions.assertEquals(0, lp.getFailedLoginTimeOffsetIfExists("horst").toInt(),
                "No offset for new user 'horst' expected.")
        lp.setEntry("horst", 10, current - DURATION_48_HOURS) // Expired.
        lp.incrementFailedLoginTimeOffset("kai") // 10 failed login attempts should be deleted now:
        Assertions.assertEquals(1, lp.sizeOfLastFailedLoginMap,
                "Penalty for 'horst' should be deleted, because it's expired.")
        Assertions.assertEquals(1, lp.sizeOfLoginFailedAttemptsMap,
                "Penalty for 'horst' should be deleted, because it's expired.")
        Assertions.assertEquals(0, lp.getNumberOfFailedLoginAttempts("horst"),
                "Penalty for 'horst' should be deleted, because it's expired.")
        Assertions.assertEquals(0, lp.getFailedLoginTimeOffsetIfExists("horst").toInt(),
                "Penalty for 'horst' should be deleted, because it's expired.")
        lp.setEntry("horst", 10, current - DURATION_4_HOURS) // Not expired.
        lp.incrementFailedLoginTimeOffset("kai")
        Assertions.assertEquals(0, lp.getFailedLoginTimeOffsetIfExists("horst").toInt(),
                "No time offset for 'horst' expected because last login was 4 hours ago.")
        lp.incrementFailedLoginTimeOffset("horst")
        Assertions.assertEquals(11, lp.getNumberOfFailedLoginAttempts("horst"),
                "11 failed login attempts expected.")
        Assertions.assertTrue(lp.getFailedLoginTimeOffsetIfExists("horst") > 0,
                "Time offset due to 11 failed logins expected.")
        Assertions.assertTrue(lp.getFailedLoginTimeOffsetIfExists("horst") < 11001,
                "Time offset due to 11 failed logins not more than 11 seconds expected.")
        lp.clearLoginTimeOffset("horst")
        Assertions.assertEquals(0, lp.getFailedLoginTimeOffsetIfExists("horst").toInt(),
                "Penalty for 'horst' should be deleted.")
        lp.incrementFailedLoginTimeOffset("horst")
        val offset = lp.getFailedLoginTimeOffsetIfExists("horst")
        Assertions.assertTrue(offset > 0 && offset < 1001,
                "Time offset between 0 and 1 second expected due to 1 failed login attempt.")
        Thread.sleep(offset + 1)
        Assertions.assertEquals(0, lp.getFailedLoginTimeOffsetIfExists("horst").toInt(),
                "No time offset for 'horst' expected, because time offest was run down.")
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLoginProtectionByIp() {
        val lp = instance()
        lp.clearAll()
        Assertions.assertEquals(0, lp.mapByIpAddress.sizeOfLastFailedLoginMap,
                "Maps should be empty.")
        Assertions.assertEquals(0, lp.mapByIpAddress.sizeOfLoginFailedAttemptsMap,
                "Maps should be empty.")
        val ip = "192.168.76.1"
        for (i in 0..3000) {
            lp.incrementFailedLoginTimeOffset(i.toString(), ip)
        }
        Assertions.assertTrue(lp.getFailedLoginTimeOffsetIfExists("kai", ip) > 0,
                "Time offset due to 3 failed logins expected.")
        Assertions.assertTrue(lp.getFailedLoginTimeOffsetIfExists("kai", ip) < 3001,
                "Time offset due to 3 failed logins not more than 3 seconds expected.")
        Assertions.assertEquals(0, lp.getFailedLoginTimeOffsetIfExists("horst", "192.168.76.2").toInt(),
                "No offset for new ip address expected.")
    }

    companion object {
        private const val DURATION_48_HOURS = 48 * 60 * 60 * 1000.toLong()
        private const val DURATION_4_HOURS = 4 * 60 * 60 * 1000.toLong()
    }
}
