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

package org.projectforge.framework.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyHelper
{
  /**
   * @param net If null then zero is returned.
   * @param vat
   * @return Gross amount or net if vat is null or zero.
   */
  public static final BigDecimal getGrossAmount(final BigDecimal net, final BigDecimal vat)
  {
    if (net == null) {
      return BigDecimal.ZERO;
    }
    if (NumberHelper.isZeroOrNull(vat) == true) {
      return net;
    }
    return net.multiply(BigDecimal.ONE.add(vat));
  }

  public static final BigDecimal multiply(final BigDecimal val1, final BigDecimal val2)
  {
    if (val1 == null) {
      if (val2 == null) {
        return BigDecimal.ZERO;
      } else {
        return val2;
      }
    } else if (val2 == null) {
      return val1;
    } else {
      return val1.multiply(val2);
    }
  }
}
