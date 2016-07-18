/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.projectforge.business.login.LoginProtection;
import org.projectforge.business.login.LoginProtectionMap;
import org.testng.annotations.Test;

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
    assertEquals("Maps should be empty.", 0, lp.getSizeOfLastFailedLoginMap());
    assertEquals("Maps should be empty.", 0, lp.getSizeOfLoginFailedAttemptsMap());
    lp.incrementFailedLoginTimeOffset("kai");
    lp.incrementFailedLoginTimeOffset("kai");
    lp.incrementFailedLoginTimeOffset("kai");
    assertTrue("Time offset due to 3 failed logins expected.", lp.getFailedLoginTimeOffsetIfExists("kai") > 0);
    assertTrue("Time offset due to 3 failed logins not more than 3 seconds expected.",
        lp.getFailedLoginTimeOffsetIfExists("kai") < 3001);
    assertEquals("No offset for new user 'horst' expected.", 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"));
    lp.setEntry("horst", 10, current - DURATION_48_HOURS); // Expired.
    lp.incrementFailedLoginTimeOffset("kai"); // 10 failed login attempts should be deleted now:
    assertEquals("Penalty for 'horst' should be deleted, because it's expired.", 1, lp.getSizeOfLastFailedLoginMap());
    assertEquals("Penalty for 'horst' should be deleted, because it's expired.", 1,
        lp.getSizeOfLoginFailedAttemptsMap());
    assertEquals("Penalty for 'horst' should be deleted, because it's expired.", 0,
        lp.getNumberOfFailedLoginAttempts("horst"));
    assertEquals("Penalty for 'horst' should be deleted, because it's expired.", 0,
        (int) lp.getFailedLoginTimeOffsetIfExists("horst"));
    lp.setEntry("horst", 10, current - DURATION_4_HOURS); // Not expired.
    lp.incrementFailedLoginTimeOffset("kai");
    assertEquals("No time offset for 'horst' expected because last login was 4 hours ago.", 0,
        (int) lp.getFailedLoginTimeOffsetIfExists("horst"));
    lp.incrementFailedLoginTimeOffset("horst");
    assertEquals("11 failed login attempts expected.", 11, lp.getNumberOfFailedLoginAttempts("horst"));
    assertTrue("Time offset due to 11 failed logins expected.", lp.getFailedLoginTimeOffsetIfExists("horst") > 0);
    assertTrue("Time offset due to 11 failed logins not more than 11 seconds expected.",
        lp.getFailedLoginTimeOffsetIfExists("horst") < 11001);
    lp.clearLoginTimeOffset("horst");
    assertEquals("Penalty for 'horst' should be deleted.", 0, (int) lp.getFailedLoginTimeOffsetIfExists("horst"));
    lp.incrementFailedLoginTimeOffset("horst");
    final long offset = lp.getFailedLoginTimeOffsetIfExists("horst");
    assertTrue("Time offset between 0 and 1 second expected due to 1 failed login attempt.",
        offset > 0 && offset < 1001);
    Thread.sleep(offset + 1);
    assertEquals("No time offset for 'horst' expected, because time offest was run down.", 0,
        (int) lp.getFailedLoginTimeOffsetIfExists("horst"));
  }

  @Test
  public void testLoginProtectionByIp() throws InterruptedException
  {
    final LoginProtection lp = LoginProtection.instance();
    lp.clearAll();
    assertEquals("Maps should be empty.", 0, lp.getMapByIpAddress().getSizeOfLastFailedLoginMap());
    assertEquals("Maps should be empty.", 0, lp.getMapByIpAddress().getSizeOfLoginFailedAttemptsMap());
    final String ip = "192.168.76.1";
    for (int i = 0; i < 3001; i++) {
      lp.incrementFailedLoginTimeOffset(String.valueOf(i), ip);
    }
    assertTrue("Time offset due to 3 failed logins expected.", lp.getFailedLoginTimeOffsetIfExists("kai", ip) > 0);
    assertTrue("Time offset due to 3 failed logins not more than 3 seconds expected.",
        lp.getFailedLoginTimeOffsetIfExists("kai", ip) < 3001);
    assertEquals("No offset for new ip address expected.", 0,
        (int) lp.getFailedLoginTimeOffsetIfExists("horst", "192.168.76.2"));
  }
}
