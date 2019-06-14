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

package org.projectforge.plugins.ffp.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FFPEventServiceImplAverageCalculationTest
{

  private static FFPEventServiceImpl ffpEventServiceImpl;

  @BeforeAll
  public static void setup()
  {
    ffpEventServiceImpl = new FFPEventServiceImpl();
  }

  @Test
  public void testAverageCalculation() throws Exception
  {
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(0D, 0D));
    accountingDOs.add(createFfpAccounting(1D, 1D));
    accountingDOs.add(createFfpAccounting(2D, 2D));
    BigDecimal calculatedAverage = ffpEventServiceImpl.calculateAverage(accountingDOs);
    assertEquals( BigDecimal.ONE.setScale(10, BigDecimal.ROUND_HALF_UP), calculatedAverage,"calculated wrong average");
  }

  @Test
  public void testAverageCalculation1() throws Exception
  {
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(0D, 0.75D));
    accountingDOs.add(createFfpAccounting(1.21D, 1.5D));
    accountingDOs.add(createFfpAccounting(2.68D, 2D));

    BigDecimal calculatedAverage = ffpEventServiceImpl.calculateAverage(accountingDOs);
    assertEquals(new BigDecimal(3.89D / 4.25D).setScale(10, BigDecimal.ROUND_HALF_UP), calculatedAverage,"calculated wrong average");
  }

  @Test
  public void testAverageCalculation2() throws Exception
  {
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(456.90D, 1.23D));
    accountingDOs.add(createFfpAccounting(123.45D, 5.67D));
    accountingDOs.add(createFfpAccounting(567.89D, 55.5D));
    BigDecimal calculatedAverage = ffpEventServiceImpl.calculateAverage(accountingDOs);
    assertEquals( new BigDecimal(1148.24D / 62.4D).setScale(10, BigDecimal.ROUND_HALF_UP), calculatedAverage,"calculated wrong average");
  }

  @Test
  public void testAverageCalculation3() throws Exception
  {
    Assertions.assertThrows(ArithmeticException.class, () -> {
      Set<FFPAccountingDO> accountingDOs = new HashSet<>();
      accountingDOs.add(createFfpAccounting(0D, 0D));
      accountingDOs.add(createFfpAccounting(0D, 0D));
      accountingDOs.add(createFfpAccounting(0D, 0D));
      ffpEventServiceImpl.calculateAverage(accountingDOs);
    });
  }

  private FFPAccountingDO createFfpAccounting(double value, double weighting)
  {
    FFPAccountingDO ffpAccountingDO1 = new FFPAccountingDO();
    ffpAccountingDO1.setWeighting(BigDecimal.valueOf(weighting));
    ffpAccountingDO1.setValue(BigDecimal.valueOf(value));
    return ffpAccountingDO1;
  }

}
