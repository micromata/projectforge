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

package org.projectforge.common;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.utils.CurrencyHelper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.fail;

public class CurrencyHelperTest
{
  @Test
  public void grossAmount()
  {
    myAssertEquals("0", CurrencyHelper.getGrossAmount(null, null));
    myAssertEquals("0", CurrencyHelper.getGrossAmount(null, BigDecimal.ZERO));
    myAssertEquals("0", CurrencyHelper.getGrossAmount(null, BigDecimal.ONE));
    myAssertEquals("0", CurrencyHelper.getGrossAmount(BigDecimal.ZERO, null));
    myAssertEquals("0", CurrencyHelper.getGrossAmount(BigDecimal.ZERO, BigDecimal.ZERO));
    myAssertEquals("1", CurrencyHelper.getGrossAmount(BigDecimal.ONE, null));
    myAssertEquals("1", CurrencyHelper.getGrossAmount(BigDecimal.ONE, BigDecimal.ZERO));
    myAssertEquals("1.19", CurrencyHelper.getGrossAmount(BigDecimal.ONE, new BigDecimal("0.19")));
  }

  private void myAssertEquals(final BigDecimal expected, final BigDecimal actual)  {
    if (expected.compareTo(actual) != 0) {
      fail("BigDecimals not equal: expected=" + expected + ", actual=" + actual);
    }
  }

  private void myAssertEquals(final String expected, final BigDecimal actual)  {
    myAssertEquals(new BigDecimal(expected), actual);  }
}
