package org.projectforge.plugins.ffp.repository;

import static org.testng.AssertJUnit.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 0D, 0D));
    accountingDOs.add(createFfpAccounting(2, 1D, 1D));
    accountingDOs.add(createFfpAccounting(3, 2D, 2D));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 0, calculateDebt.size());

  }

  @Test
  public void testDebtCalculation1() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 0D, 0.75D));
    accountingDOs.add(createFfpAccounting(2, 1.21D, 1.5D));
    accountingDOs.add(createFfpAccounting(3, 2.68D, 2D));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(1, 3, 0.69D);
    TestPredicate pre2 = new TestPredicate(2, 3, 0.16D);

    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre2));
  }

  @Test
  public void testDebtCalculation2() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 456.90D, 1.23D));
    accountingDOs.add(createFfpAccounting(2, 123.45D, 5.67D));
    accountingDOs.add(createFfpAccounting(3, 567.89D, 55.50D));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(3, 1, 434.27D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(3, 2, 19.11D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test(expectedExceptions = ArithmeticException.class)
  public void testDebtCalculation3() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 0D, 0D));
    accountingDOs.add(createFfpAccounting(2, 0D, 0D));
    accountingDOs.add(createFfpAccounting(3, 0D, 0D));
    event.setAccountingList(accountingDOs);
    ffpEventServiceImpl.calculateDebt(event);
  }

  @Test
  public void testDebtCalculation4() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 0D, 0D));
    accountingDOs.add(createFfpAccounting(2, 2D, 1D));
    accountingDOs.add(createFfpAccounting(3, 1D, 2D));
    event.setAccountingList(accountingDOs);
    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 1, calculateDebt.size());
    TestPredicate pre = new TestPredicate(3, 2, 1D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
  }

  @Test
  public void testDebtCalculation5() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 1D, 1D));
    accountingDOs.add(createFfpAccounting(2, 1D, 1D));
    accountingDOs.add(createFfpAccounting(3, 2D, 1D));
    accountingDOs.add(createFfpAccounting(4, 4D, 1D));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(1, 4, 1D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(2, 4, 1D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test
  public void testDebtCalculation7() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 1D, 1D));
    accountingDOs.add(createFfpAccounting(2, 0D, 1D));
    accountingDOs.add(createFfpAccounting(3, 1D, 1D));
    accountingDOs.add(createFfpAccounting(4, 0D, 1D));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(2, 1, 0.5D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(4, 3, 0.5D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  @Test
  public void testDebtCalculation8() throws Exception
  {
    FFPEventDO event = new FFPEventDO();
    List<FFPAccountingDO> accountingDOs = new ArrayList<>();
    accountingDOs.add(createFfpAccounting(1, 1D, 1D));
    accountingDOs.add(createFfpAccounting(2, 0D, 1D));
    accountingDOs.add(createFfpAccounting(3, 1D, 1D));
    accountingDOs.add(createFfpAccounting(4, 0D, 1D));
    event.setAccountingList(accountingDOs);

    List<FFPDebtDO> calculateDebt = ffpEventServiceImpl.calculateDebt(event);
    assertNotNull("ffpEventServiceImpl.calculateDebt returned null object", calculateDebt);
    assertEquals("wrong count of debts", 2, calculateDebt.size());
    TestPredicate pre = new TestPredicate(2, 1, 0.5D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre));
    TestPredicate pre1 = new TestPredicate(4, 3, 0.5D);
    assertTrue("calculated wrong debt", calculateDebt.stream().anyMatch(pre1));
  }

  private FFPAccountingDO createFfpAccounting(Integer pk, double value, double weighting)
  {
    FFPAccountingDO ffpAccountingDO1 = new FFPAccountingDO();
    EmployeeDO attendee = new EmployeeDO();
    attendee.setPk(pk);
    ffpAccountingDO1.setAttendee(attendee);
    ffpAccountingDO1.setWeighting(BigDecimal.valueOf(weighting));
    ffpAccountingDO1.setValue(BigDecimal.valueOf(value));
    return ffpAccountingDO1;
  }

  private class TestPredicate implements Predicate<FFPDebtDO>
  {
    private int fromPk;
    private int toPk;
    private double value;

    private TestPredicate(int fromPk, int toPk, double value)
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
          t.getValue().equals(new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP));
    }

  }

}
