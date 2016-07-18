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

package org.projectforge.core;

import static org.testng.AssertJUnit.assertEquals;

import java.math.BigDecimal;
import java.util.Locale;

import org.projectforge.common.TestHelper;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class NumberFormatterTest extends AbstractTestBase
{
  @Test
  public void formatPercentage()
  {
    TestHelper.setContextUser(userCache, Locale.ENGLISH);
    assertEquals("", NumberFormatter.formatPercent(null));
    assertEquals("19%", NumberFormatter.formatPercent(new BigDecimal("0.19000")));
    assertEquals("19.2%", NumberFormatter.formatPercent(new BigDecimal("0.19200")));
  }
}
