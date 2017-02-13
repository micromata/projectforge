package org.projectforge.business.vacation.service;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by blumenstein on 27.10.16.
 */
@PrepareForTest({ ThreadLocalUserContext.class, ConfigXml.class })
public class VacationServiceTest extends PowerMockTestCase
{
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

  private final Calendar now = Calendar.getInstance();

  private final Calendar endLastYear = Calendar.getInstance();

  @InjectMocks
  private VacationService vacationService = new VacationServiceImpl();

  @Mock
  private ConfigurationService configService;

  @Mock
  private EmployeeDao employeeDao;

  @Mock
  private VacationDao vacationDao;

  @Mock
  private EmployeeDO employee;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    mockStatic(ConfigXml.class);
    Locale locale = Locale.getDefault();
    TimeZone timeZone = TimeZone.getDefault();
    sdf.setTimeZone(timeZone);
    ConfigXml configXml = new ConfigXml("./target/Projectforge");
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
    PowerMockito.when(ConfigXml.getInstance()).thenReturn(configXml);
    endLastYear.set(Calendar.YEAR, 2017);
    endLastYear.set(Calendar.MONTH, Calendar.MARCH);
    endLastYear.set(Calendar.DAY_OF_MONTH, 31);
    when(configService.getEndDateVacationFromLastYear()).thenReturn(endLastYear);
  }

  @Test
  public void testGetVacationDays()
  {
    final Calendar workingDate1 = Calendar.getInstance();
    workingDate1.set(Calendar.YEAR, 2017);
    workingDate1.set(Calendar.MONTH, Calendar.FEBRUARY);
    workingDate1.set(Calendar.DAY_OF_MONTH, 1);
    final Calendar workingDate2 = Calendar.getInstance();
    workingDate2.set(Calendar.YEAR, 2017);
    workingDate2.set(Calendar.MONTH, Calendar.FEBRUARY);
    workingDate2.set(Calendar.DAY_OF_MONTH, 2);

    assertEquals(vacationService.getVacationDays(workingDate1.getTime(), workingDate1.getTime(), false), new BigDecimal(1));
    assertEquals(vacationService.getVacationDays(workingDate1.getTime(), workingDate1.getTime(), true), new BigDecimal(0.5));
    assertEquals(vacationService.getVacationDays(workingDate1.getTime(), workingDate2.getTime(), false), new BigDecimal(2));
    assertEquals(vacationService.getVacationDays(workingDate1.getTime(), workingDate2.getTime(), true), new BigDecimal(0.5));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearNull()
  {
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(null);
    assertEquals(newValue, BigDecimal.ZERO);

    VacationDO vacationData = new VacationDO();
    newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);

    vacationData = new VacationDO();
    vacationData.setEmployee(employee);
    newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);

    vacationData = new VacationDO();
    vacationData.setEmployee(employee);
    Date startDate = new Date();
    vacationData.setStartDate(startDate);
    newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearLaterThanEndDate()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setIsSpecial(false);
    vacationData.setEmployee(employee);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.APRIL);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.APRIL);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearZero()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setIsSpecial(false);
    vacationData.setEmployee(employee);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.MARCH);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearCompletUsed()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setIsSpecial(false);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(5));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(5));
    vacationData.setEmployee(employee);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.MARCH);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, new BigDecimal(5));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearFillUpRest()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setIsSpecial(false);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(10));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(9));
    vacationData.setEmployee(employee);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.MARCH);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, new BigDecimal(10));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearSomeRest()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setIsSpecial(false);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(20));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(1));
    vacationData.setEmployee(employee);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.MARCH);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal numberOfDays = vacationService.getVacationDays(vacationData);
    BigDecimal newValue = vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ONE.add(numberOfDays));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearOverEndDate()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setIsSpecial(false);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(20));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(1));
    vacationData.setEmployee(employee);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 25);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.APRIL);
    endDate.set(Calendar.DAY_OF_MONTH, 5);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal numberOfDays = vacationService.getVacationDays(vacationData.getStartDate(), endLastYear.getTime(), vacationData.getHalfDay());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ONE.add(numberOfDays));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearIsSpecial()
  {
    VacationDO vacationData = new VacationDO();
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(20));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(1));
    vacationData.setEmployee(employee);
    vacationData.setIsSpecial(true);
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearFirstNormalAfterIsSpecial()
  {
    VacationDO vacationData = new VacationDO();
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(20));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(0));
    vacationData.setEmployee(employee);
    vacationData.setIsSpecial(false);
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.MARCH);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    BigDecimal numberOfDays = this.vacationService.getVacationDays(vacationData);
    assertEquals(newValue, BigDecimal.ZERO.add(numberOfDays));
    vacationData.setIsSpecial(true);
    BigDecimal Value = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(Value, BigDecimal.ZERO);
  }

  @Test
  public void testGetAvailableVacationdaysEmployeeNull()
  {
    BigDecimal availableVacationdays = vacationService.getAvailableVacationdaysForYear((EmployeeDO) null, 0, false);
    assertEquals(availableVacationdays, BigDecimal.ZERO);
  }

  @Test
  public void testGetAvailableVacationdaysPFUserNull()
  {
    BigDecimal availableVacationdays = vacationService.getAvailableVacationdaysForYear((PFUserDO) null, 0, false);
    assertEquals(availableVacationdays, BigDecimal.ZERO);
  }

  @Test
  public void testGetAvailableVacationdaysNoDaysUsed()
  {
    List<VacationDO> vacationList = new ArrayList<>();
    when(vacationDao.getActiveVacationForYear(employee, now.get(Calendar.YEAR), false)).thenReturn(vacationList);
    when(employee.getUrlaubstage()).thenReturn(30);
    BigDecimal availableVacationdays = vacationService.getAvailableVacationdaysForYear(employee, now.get(Calendar.YEAR), false);
    assertEquals(availableVacationdays, new BigDecimal(30));
  }

  @Test
  public void testGetAvailableVacationdaysDaysUsed()
  {
    List<VacationDO> vacationList = new ArrayList<>();
    VacationDO vacation = new VacationDO();
    Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 25);
    vacation.setStartDate(startDate.getTime());
    Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.APRIL);
    endDate.set(Calendar.DAY_OF_MONTH, 5);
    vacation.setEndDate(endDate.getTime());
    vacationList.add(vacation);
    BigDecimal numberOfDays = vacationService.getVacationDays(vacation);
    when(vacationDao.getActiveVacationForYear(employee, now.get(Calendar.YEAR), false)).thenReturn(vacationList);
    when(employee.getUrlaubstage()).thenReturn(30);
    BigDecimal availableVacationdays = vacationService.getAvailableVacationdaysForYear(employee, now.get(Calendar.YEAR), false);
    assertEquals(availableVacationdays, new BigDecimal(30).subtract(numberOfDays));
  }

  @Test
  public void testGetAvailableVacationDaysForYearAtDate() throws ParseException
  {
    final List<VacationDO> vacationList = Arrays.asList(
        createVacation("2017-02-01", "2017-02-02", false, VacationStatus.IN_PROGRESS), // 2, will not be considered
        createVacation("2017-02-03", "2017-02-06", false, VacationStatus.REJECTED), // 2, will not be considered
        createVacation("2017-02-01", "2017-02-10", false, VacationStatus.APPROVED), // 8
        createVacation("2017-02-13", "2017-02-13", true, VacationStatus.APPROVED), // 0.5
        createVacation("2017-02-24", "2017-03-03", false, VacationStatus.APPROVED), // 3 in feb + 3 in mar
        createVacation("2017-03-30", "2017-04-04", false, VacationStatus.APPROVED) // 2 in mar + 2 in apr
    );

    final int yearlyVacation = 30;
    final BigDecimal prevYear = new BigDecimal(20);
    final BigDecimal prevYearUsed = new BigDecimal(16.5);
    when(employee.getUrlaubstage()).thenReturn(yearlyVacation);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(prevYear);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(prevYearUsed);

    final ArgumentCaptor<Date> startDateCaptor = ArgumentCaptor.forClass(Date.class);
    when(vacationDao.getVacationForPeriod(same(employee), startDateCaptor.capture(), any(Date.class), eq(false))).thenReturn(vacationList);
    final Date expectedStartDate = sdf.parse("2017-01-01");

    // Test 1
    final BigDecimal availableVacationdays = vacationService.getAvailableVacationDaysForYearAtDate(employee, sdf.parse("2017-02-28"));
    assertEquals(availableVacationdays, new BigDecimal(yearlyVacation).add(prevYear).subtract(new BigDecimal(11.5)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 1);

    // Test 2
    final BigDecimal availableVacationdays2 = vacationService.getAvailableVacationDaysForYearAtDate(employee, sdf.parse("2017-03-31"));
    assertEquals(availableVacationdays2, new BigDecimal(yearlyVacation).add(prevYear).subtract(new BigDecimal(16.5)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 2);

    // Test 3
    final BigDecimal availableVacationdays3 = vacationService.getAvailableVacationDaysForYearAtDate(employee, sdf.parse("2017-04-30"));
    assertEquals(availableVacationdays3, new BigDecimal(yearlyVacation).add(prevYearUsed).subtract(new BigDecimal(18.5)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 3);
  }

  @Test
  public void testGetAvailableVacationDaysForYearAtDatePrevYear() throws ParseException
  {
    final List<VacationDO> vacationList = Arrays.asList(
        createVacation("2016-02-24", "2016-03-03", false, VacationStatus.APPROVED), // 4 in feb + 3 in mar
        createVacation("2016-03-30", "2016-04-04", false, VacationStatus.APPROVED) // 2 in mar + 2 in apr
    );

    final int yearlyVacation = 30;
    final BigDecimal prevYear = new BigDecimal(20);
    final BigDecimal prevYearUsed = new BigDecimal(16.5);
    when(employee.getUrlaubstage()).thenReturn(yearlyVacation);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(prevYear);
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(prevYearUsed);

    final ArgumentCaptor<Date> startDateCaptor = ArgumentCaptor.forClass(Date.class);
    when(vacationDao.getVacationForPeriod(same(employee), startDateCaptor.capture(), any(Date.class), eq(false))).thenReturn(vacationList);
    final Date expectedStartDate = sdf.parse("2016-01-01");

    // here we verify that the previous year leave att values are ignored because they belong to 2017 (see this.endLastYear) and we query values from 2016
    // Test 1
    final BigDecimal availableVacationdays = vacationService.getAvailableVacationDaysForYearAtDate(employee, sdf.parse("2016-02-29"));
    assertEquals(availableVacationdays, new BigDecimal(yearlyVacation).subtract(new BigDecimal(4)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 1);

    // Test 2
    final BigDecimal availableVacationdays2 = vacationService.getAvailableVacationDaysForYearAtDate(employee, sdf.parse("2016-04-30"));
    assertEquals(availableVacationdays2, new BigDecimal(yearlyVacation).subtract(new BigDecimal(11)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 2);
  }

  private VacationDO createVacation(final String from, final String to, final Boolean halfDay, final VacationStatus status) throws ParseException
  {
    final VacationDO vacation = new VacationDO();
    vacation.setStartDate(sdf.parse(from));
    vacation.setEndDate(sdf.parse(to));
    vacation.setHalfDay(halfDay);
    vacation.setStatus(status);
    return vacation;
  }

}
