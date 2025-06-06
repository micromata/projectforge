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

package org.projectforge.framework.persistence.database.timeable;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of persistence of attributes.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
public class EmployeeAttrTest extends AbstractTestBase {
    @Autowired
    private EmployeeDao employeeDao;

    private static String longValue;

    static {
        final StringBuilder sb = new StringBuilder(6000);
        while (sb.length() < 5500) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        longValue = sb.toString();
    }

    @Test
    public void persistTimeAttr() {
        logon(AbstractTestBase.TEST_FULL_ACCESS_USER);
/*        final PFUserDO user = initTestDB.addUser("EmployeeAttrTest");
        final EmployeeDO e = employeeDao.newInstance();
        e.setUser(user);
        e.setComment("EmployeeAttrTest");
    /*
    final EmployeeTimedDO et = employeeDao.newEmployeeTimeAttrRow(e);
    et.setGroupName("PERIOD");
    et.setStartTime(new Date());
    et.putAttribute("hollydays", 42);
    et.putAttribute("longValue", longValue);
    et.putAttribute("longValue2", longValue);
    et.putAttribute("shortValue", "bla");
    employeeDao.save(e);

    EmployeeDO luser = employeeDao.getById(e.getId());
    final String comment = luser.getComment();
    Assertions.assertTrue(!luser.getTimeableAttributes().isEmpty());
    Assertions.assertTrue(luser.getTimeableAttributes().size() == 1);

    EmployeeTimedDO row = luser.getTimeableAttributes().get(0);
    Integer rhol = row.getAttribute("hollydays", Integer.class);
    Assertions.assertEquals(42, rhol.intValue());
    String rlongVal = row.getAttribute("longValue", String.class);
    Assertions.assertEquals(longValue, rlongVal);
    row.putAttribute("hollydays", 43);
    employeeDao.update(luser);
    luser = employeeDao.getById(e.getId());
    row = luser.getTimeableAttributes().get(0);
    rhol = row.getAttribute("hollydays", Integer.class);
    Assertions.assertEquals(43, rhol.intValue());
    rlongVal = row.getAttribute("longValue", String.class);
    Assertions.assertEquals(longValue, rlongVal);

    final String nlongValue = longValue + "X";
    row.putAttribute("longValue", nlongValue);
    row.putAttribute("longValue2", "XSHORT");
    //    employeeDao.update(luser);
    //    luser = employeeDao.getById(e.getId());
    //    row = luser.getTimeableAttributes().get(0);
    //    rlongVal = row.getAttribute("longValue", String.class);
    //    Assertions.assertEquals(nlongValue, rlongVal);
    //    rlongVal = row.getAttribute("longValue2", String.class);
    //    Assertions.assertEquals("XSHORT", rlongVal);
*/
    }

}
