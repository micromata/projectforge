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

package org.projectforge.business.login;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginProtectionTest
{
  private static final long DURATION_48_HOURS = 48 * 60 * 60 * 1000;

  private static final long DURATION_4_HOURS = 4 * 60 * 60 * 1000;

  @Test
  public void testLoginProtection() throws InterruptedException
  {
    final long current = System.currentTimeMillis();
    final LoginProtectionMap lp = LoginProtection.instance().getMapByUserId();
    lp.clearAll();
    assertEquals( 0, lp.getSizeOfLastFailedLoginMap(),"Maps should be empty.");
    assertEquals( 0, lp.getSizeOfLoginFailedAttemptsMap(),"Maps should be empty.");
    lp.incrementFailedLoginTimeOffset("kai");
    lp.incrementFailedLoginTimeOffset("kai");
    lp.incrementFailedLoginTimeOffset("kai");
    assertTrue( lp.getFailedLoginTimeOffsetIfExists("kai") > 0,
            "Time offset due to 3 failed logins expected.");
    assertTrue(lp.getFailedLoginTimeOffsetIfExists("kai") < 3001,
            "Time offset due to 3 failed logins not more than 3 seconds expected.");
    assertEquals( 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"),
            "No offset for new user 'horst' expected.");
    lp.setEntry("horst", 10, current - DURATION_48_HOURS); // Expired.
    lp.incrementFailedLoginTimeOffset("kai"); // 10 failed login attempts should be deleted now:
    assertEquals( 1, lp.getSizeOfLastFailedLoginMap(),
            "Penalty for 'horst' should be deleted, because it's expired.");
    assertEquals( 1, lp.getSizeOfLoginFailedAttemptsMap(),
            "Penalty for 'horst' should be deleted, because it's expired.");
    assertEquals( 0, lp.getNumberOfFailedLoginAttempts("horst"),
            "Penalty for 'horst' should be deleted, because it's expired.");
    assertEquals( 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"),
            "Penalty for 'horst' should be deleted, because it's expired.");
    lp.setEntry("horst", 10, current - DURATION_4_HOURS); // Not expired.
    lp.incrementFailedLoginTimeOffset("kai");
    assertEquals( 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"),
            "No time offset for 'horst' expected because last login was 4 hours ago.");
    lp.incrementFailedLoginTimeOffset("horst");
    assertEquals( 11, lp.getNumberOfFailedLoginAttempts("horst"),
            "11 failed login attempts expected.");
    assertTrue( lp.getFailedLoginTimeOffsetIfExists("horst") > 0,
            "Time offset due to 11 failed logins expected.");
    assertTrue(lp.getFailedLoginTimeOffsetIfExists("horst") < 11001,
            "Time offset due to 11 failed logins not more than 11 seconds expected.");
    lp.clearLoginTimeOffset("horst");
    assertEquals( 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"),
            "Penalty for 'horst' should be deleted.");
    lp.incrementFailedLoginTimeOffset("horst");
    final long offset = lp.getFailedLoginTimeOffsetIfExists("horst");
    assertTrue(offset > 0 && offset < 1001,
            "Time offset between 0 and 1 second expected due to 1 failed login attempt.");
    Thread.sleep(offset + 1);
    assertEquals( 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"),
            "No time offset for 'horst' expected, because time offest was run down.");
  }

  @Test
  public void testLoginProtectionByIp() throws InterruptedException
  {
    final LoginProtection lp = LoginProtection.instance();
    lp.clearAll();
    assertEquals( 0, lp.getMapByIpAddress().getSizeOfLastFailedLoginMap(),
            "Maps should be empty.");
    assertEquals( 0, lp.getMapByIpAddress().getSizeOfLoginFailedAttemptsMap(),
            "Maps should be empty.");
    final String ip = "192.168.76.1";
    for (int i = 0; i < 3001; i++) {
      lp.incrementFailedLoginTimeOffset(String.valueOf(i), ip);
    }
    assertTrue( lp.getFailedLoginTimeOffsetIfExists("kai", ip) > 0,
            "Time offset due to 3 failed logins expected.");
    assertTrue(lp.getFailedLoginTimeOffsetIfExists("kai", ip) < 3001,
            "Time offset due to 3 failed logins not more than 3 seconds expected.");
    assertEquals( 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst", "192.168.76.2"),
            "No offset for new ip address expected.");
  }
}
