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

package org.projectforge.framework.persistence.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DatebaseUpdateHelperTest
{
  @Test
  public void getIdTest()
  {
    assertNull(DatabaseUpdateHelper.getId(null));
    assertNull(DatabaseUpdateHelper.getId(""));
    assertNull(DatabaseUpdateHelper.getId("id="));
    assertEquals(new Integer(5), DatabaseUpdateHelper.getId("id=5"));
    assertEquals(new Integer(5), DatabaseUpdateHelper.getId("dsfasdfds,id=5,dfsadf"));
    assertEquals(new Integer(5), DatabaseUpdateHelper.getId("dsfasdfds,[ id=5,dfsadf"));
    assertNull(DatabaseUpdateHelper.getId("dsfasdfds id=5,dfsadf"));
    assertEquals(new Integer(42),
        DatabaseUpdateHelper.getId("PFUserDO[username=kai,right=[id=5,foo=Hurzel],name=Kai,id=42,test=dfds]"));
    assertEquals(
        new Integer(2),
        DatabaseUpdateHelper
            .getId(
                "org.projectforge.business.user.PFUserDO@100fdc74[username=kai,jiraUsername=<null>,firstname=Kai,lastname=Reinhard,description=Geschäftsführer,email=k.reinhard@...,stayLoggedInKey=123456789,lastLogin=2010-10-10 21:41:12.298,loginFailures=0,locale=<null>,minorChange=false,timeZone=sun.util.calendar.ZoneInfo[id=\"Europe/Berlin\",offset=3600000,dstSavings=3600000,useDaylight=true,transitions=143,lastRule=java.util.SimpleTimeZone[id=Europe/Berlin,offset=3600000,dstSavings=3600000,useDaylight=true,startYear=0,startMode=2,startMonth=2,startDay=-1,startDayOfWeek=1,startTime=3600000,startTimeMode=2,endMode=2,endMonth=9,endDay=-1,endDayOfWeek=1,endTime=3600000,endTimeMode=2]],clientLocale=<null>,organization=Micromata GmbH,personalPhoneIdentifiers=21,25,50,personalMebMobileNumbers=0123456789,rights=[org.projectforge.business.user.UserRightDO@352834d7[id=510336,userId=2,rightId=FIBU_COST_UNIT,value=READWRITE], org.projectforge.business.user.UserRightDO@2070ad24[id=515964,userId=2,rightId=PM_ORDER_BOOK,value=READWRITE], org.projectforge.business.user.UserRightDO@62a52a84[id=510338,userId=2,rightId=PM_PROJECT,value=READWRITE], org.projectforge.business.user.UserRightDO@41f5aeac[id=442339,userId=2,rightId=FIBU_EINGANGSRECHNUNGEN,value=READWRITE], org.projectforge.business.user.UserRightDO@6c5b570b[id=471898,userId=2,rightId=FIBU_EMPLOYEE_SALARY,value=READWRITE], org.projectforge.business.user.UserRightDO@60afcac0[id=471900,userId=2,rightId=ORGA_CONTRACTS,value=READWRITE], org.projectforge.business.user.UserRightDO@967891f[id=442342,userId=2,rightId=ORGA_OUTGOING_MAIL,value=READWRITE], org.projectforge.business.user.UserRightDO@27d4fc8a[id=442338,userId=2,rightId=FIBU_AUSGANGSRECHNUNGEN,value=READWRITE], org.projectforge.business.user.UserRightDO@e5182e1[id=442341,userId=2,rightId=ORGA_INCOMING_MAIL,value=READWRITE], org.projectforge.business.user.UserRightDO@498a72b4[id=490801,userId=2,rightId=PM_HR_PLANNING,value=READWRITE], org.projectforge.business.user.UserRightDO@689ec008[id=442340,userId=2,rightId=FIBU_DATEV_IMPORT,value=TRUE]],id=2,created=2001-08-10 17:16:13.0,lastUpdate=2010-10-10 21:41:12.304,deleted=false,minorChange=false,selected=false]"));
  }

  @SuppressWarnings("unused")
  private void checkString(final String str)
  {
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < str.length(); i++) {
      final char ch = str.charAt(i);
      if (ch == '[' || ch == ']') {
        buf.append(ch);
        if (i < str.length() - 1) {
          final char next = str.charAt(i + 1);
          if (next != '[' && next != ']') {
            buf.append(str.charAt(i + 1)).append("...");
          }
        }
      }
    }
    System.out.println(buf.toString());
  }
}
