package org.projectforge.business.vacation.service;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DayHolder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by blumenstein on 27.10.16.
 */
@PrepareForTest({ ThreadLocalUserContext.class, ConfigXml.class })
public class VacationServiceTest extends PowerMockTestCase
{
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

  private Calendar now = Calendar.getInstance();

  Calendar endLastYear = Calendar.getInstance();

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    mockStatic(ConfigXml.class);
    Locale locale = Locale.getDefault();
    TimeZone timeZone = TimeZone.getDefault();
    ConfigXml configXml = new ConfigXml("./target/Projectforge");
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
    PowerMockito.when(ConfigXml.getInstance()).thenReturn(configXml);
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
    BigDecimal numberOfDays = DayHolder.getNumberOfWorkingDays(vacationData.getStartDate(), vacationData.getEndDate());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ONE.add(numberOfDays));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearOverEndDate()
  {
    VacationDO vacationData = new VacationDO();
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
    BigDecimal numberOfDays = DayHolder.getNumberOfWorkingDays(vacationData.getStartDate(), endLastYear.getTime());
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ONE.add(numberOfDays));
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
    BigDecimal numberOfDays = DayHolder.getNumberOfWorkingDays(vacation.getStartDate(), vacation.getEndDate());
    when(vacationDao.getActiveVacationForYear(employee, now.get(Calendar.YEAR), false)).thenReturn(vacationList);
    when(employee.getUrlaubstage()).thenReturn(30);
    BigDecimal availableVacationdays = vacationService.getAvailableVacationdaysForYear(employee, now.get(Calendar.YEAR), false);
    assertEquals(availableVacationdays, new BigDecimal(30).subtract(numberOfDays));
  }

}
