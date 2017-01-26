package org.projectforge.plugins.ffp.repository;

import static org.testng.AssertJUnit.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FFPEventServiceImplDebtCalculationTest
{
  private FFPEventServiceImpl ffpEventServiceImpl;

  @BeforeMethod
  public void setup()
  {
    ffpEventServiceImpl = new FFPEventServiceImpl();
  }

  @Test
  public void testDebtCalculation() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ZERO));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(2), new BigDecimal(2)));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 0, calculateDebt.size());

  }

  @Test
  public void testDebtCalculation1() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, new BigDecimal(0.75)));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(1.21), new BigDecimal(1.5)));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(2.68), new BigDecimal(2)));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(1, 3, new BigDecimal(0.69));
    TestPredicate pre2 = new TestPredicate(2, 3, new BigDecimal(0.16));

    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre2));
  }

  @Test
  public void testDebtCalculation2() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, new BigDecimal(456.90), new BigDecimal(1.23)));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(123.45), new BigDecimal(5.67)));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(567.89), new BigDecimal(55.50)));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(3, 1, new BigDecimal(434.27));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(3, 2, new BigDecimal(19.11));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test(expectedExceptions = ArithmeticException.class)
  public void testDebtCalculation3() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ZERO));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ZERO, BigDecimal.ZERO));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ZERO, BigDecimal.ZERO));
    event.setAccountingList(accountingDOs);
    ffpEventServiceImpl.calculateDebt(event);
  }

  @Test
  public void testDebtCalculation4() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ZERO));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(2), BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ONE, new BigDecimal(2)));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 1, calculateDebt.size());
    TestPredicate pre = new TestPredicate(3, 2, BigDecimal.ONE);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
  }

  @Test
  public void testDebtCalculation5() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(2), BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(4, new BigDecimal(4), BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(1, 4, BigDecimal.ONE);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(2, 4, BigDecimal.ONE);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test
  public void testDebtCalculation7() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ZERO, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(4, BigDecimal.ZERO, BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(2, 1, new BigDecimal(0.5));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(4, 3, new BigDecimal(0.5));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test
  public void testDebtCalculation8() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, BigDecimal.ZERO, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, BigDecimal.ONE, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(4, BigDecimal.ZERO, BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(2, 1, new BigDecimal(0.5));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(4, 3, new BigDecimal(0.5));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test
  public void testDebtCalculation9() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    Set<FFPAccountingDO> accountingDOs = new HashSet<>();
    accountingDOs.add(createFfpAccounting(1, BigDecimal.ZERO, BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(2, new BigDecimal(10), BigDecimal.ONE));
    accountingDOs.add(createFfpAccounting(3, new BigDecimal(5), BigDecimal.ONE));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 1, calculateDebt.size());
    TestPredicate pre = new TestPredicate(1, 2, new BigDecimal(5));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
  }

  private FFPAccountingDO createFfpAccounting(Integer pk, BigDecimal value, BigDecimal weighting)
  {
    FFPAccountingDO ffpAccountingDO1 = new FFPAccountingDO();
    EmployeeDO attendee = new EmployeeDO();
    attendee.setPk(pk);
    ffpAccountingDO1.setAttendee(attendee);
    ffpAccountingDO1.setWeighting(weighting);
    ffpAccountingDO1.setValue(value);
    return ffpAccountingDO1;
  }

  private class TestPredicate implements Predicate<FFPDebtDO>
  {
    private int fromPk;
    private int toPk;
    private BigDecimal value;

    private TestPredicate(int fromPk, int toPk, BigDecimal value)
    {
      this.fromPk = fromPk;
      this.toPk = toPk;
      this.value = value;
    }

    @Override
    public boolean test(FFPDebtDO t)
    {
      return t.getFrom().getPk() == fromPk && //
          t.getTo().getPk() == toPk && //
          t.getValue().equals(value.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

  }

}
