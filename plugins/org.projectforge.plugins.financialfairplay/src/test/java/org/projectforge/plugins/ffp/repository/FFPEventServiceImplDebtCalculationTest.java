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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;


public class FFPEventServiceImplDebtCalculationTest {
  private FFPEventServiceImpl ffpEventServiceImpl;

  @BeforeEach
  public void setup() {
    ffpEventServiceImpl = new FFPEventServiceImpl();
  }

  @Test
  public void testDebtCalculation() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ZERO));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(2), new BigDecimal(2)));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(0, calculateDebt.size(), "wrong count of debts");

  }

  @Test
  public void testDebtCalculation1() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, new BigDecimal(0.75)));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(1.21), new BigDecimal(1.5)));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(2.68), new BigDecimal(2)));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(2, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(1, 3, new BigDecimal(0.69));
    TestPredicate pre2 = new TestPredicate(2, 3, new BigDecimal(0.16));

    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
    assertTrue(calculateDebt.stream().anyMatch(pre2), "calculated wrong debt");
  }

  @Test
  public void testDebtCalculation2() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, new BigDecimal(456.90), new BigDecimal(1.23)));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(123.45), new BigDecimal(5.67)));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(567.89), new BigDecimal(55.50)));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(2, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(3, 1, new BigDecimal(434.27));
    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
    TestPredicate pre1 = new TestPredicate(3, 2, new BigDecimal(19.11));
    assertTrue(calculateDebt.stream().anyMatch(pre1), "calculated wrong debt");
  }

  public void testDebtCalculation3() throws Exception {
    Assertions.assertThrows(ArithmeticException.class, () -> {
      FFPEventDO event = new FFPEventDO();
      Set<FFPAccountingDO> accountingDOs = new HashSet<>();
      accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ZERO));
      accountingDOs.add(createFfpAccounting(2, BigDecimal.ZERO, BigDecimal.ZERO));
      accountingDOs.add(createFfpAccounting(3, BigDecimal.ZERO, BigDecimal.ZERO));
      event.setAccountingList(accountingDOs);
      ffpEventServiceImpl.calculateDebt(event);
    });
  }

  @Test
  public void testDebtCalculation4() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ZERO));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(2), BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ONE, new BigDecimal(2)));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(1, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(3, 2, BigDecimal.ONE);
    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
  }

  @Test
  public void testDebtCalculation5() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(2), BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(4, new BigDecimal(4), BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(2, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(1, 4, BigDecimal.ONE);
    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
    TestPredicate pre1 = new TestPredicate(2, 4, BigDecimal.ONE);
    assertTrue(calculateDebt.stream().anyMatch(pre1), "calculated wrong debt");
  }

  @Test
  public void testDebtCalculation7() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ZERO, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(4, BigDecimal.ZERO, BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(2, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(2, 1, new BigDecimal(0.5));
    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
    TestPredicate pre1 = new TestPredicate(4, 3, new BigDecimal(0.5));
    assertTrue(calculateDebt.stream().anyMatch(pre1), "calculated wrong debt");
  }

  @Test
  public void testDebtCalculation8() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ZERO, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(4, BigDecimal.ZERO, BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(2, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(2, 1, new BigDecimal(0.5));
    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
    TestPredicate pre1 = new TestPredicate(4, 3, new BigDecimal(0.5));
    assertTrue(calculateDebt.stream().anyMatch(pre1), "calculated wrong debt");
  }

  @Test
  public void testDebtCalculation9() throws Exception {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(10), BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(5), BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull(calculateDebt, "ffpEventServiceImpl.calculateDebt returned null object");
    assertEquals(1, calculateDebt.size(), "wrong count of debts");
    TestPredicate pre = new TestPredicate(1, 2, new BigDecimal(5));
    assertTrue(calculateDebt.stream().anyMatch(pre), "calculated wrong debt");
  }

  private FFPAccountingDO createFfpAccounting(Integer pk, BigDecimal value, BigDecimal weighting) {
    FFPAccountingDO ffpAccountingDO1 = new FFPAccountingDO();
    PFUserDO attendee = new PFUserDO();
    attendee.setPk(pk);
    ffpAccountingDO1.setAttendee(attendee);
    ffpAccountingDO1.setWeighting(weighting);
    ffpAccountingDO1.setValue(value);
    return ffpAccountingDO1;
  }

  private class TestPredicate implements Predicate<FFPDebtDO> {
    private int fromPk;
    private int toPk;
    private BigDecimal value;

    private TestPredicate(int fromPk, int toPk, BigDecimal value) {
      this.fromPk = fromPk;
      this.toPk = toPk;
      this.value = value;
    }

    @Override
    public boolean test(FFPDebtDO t) {
      return t.getFrom().getPk() == fromPk && //
              t.getTo().getPk() == toPk && //
              t.getValue().equals(value.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

  }

}
