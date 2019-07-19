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
import org.projectforge.framework.persistence.utils.SQLHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLHelperTest
{
  @Test
  public void getYears()
  {
    List<Object[]> list = new ArrayList<Object[]>();
    int[] years = SQLHelper.getYears((Date) null, null);
    assertEquals(1, years.length);
    assertEquals(Calendar.getInstance().get(Calendar.YEAR), years[0]);

    years = SQLHelper.getYears((Integer) null, null);
    assertEquals(1, years.length);
    assertEquals(Calendar.getInstance().get(Calendar.YEAR), years[0]);

    years = SQLHelper.getYears(createDate(2008), createDate(2008));
    assertEquals(1, years.length);
    assertEquals(2008, years[0]);
    years = SQLHelper.getYears(createDate(2001), createDate(2008));
    assertEquals(8, years.length);
    assertEquals(2008, years[0]);
    assertEquals(2007, years[1]);
    assertEquals(2001, years[7]);
  }

  private java.sql.Date createDate(int year)
  {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    return new java.sql.Date(cal.getTimeInMillis());
  }
}
