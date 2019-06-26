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

package org.projectforge.business.vacation.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by blumenstein on 27.10.16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ThreadLocalUserContext.class, ConfigXml.class })
//Needed for: java.lang.ClassCastException: com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl cannot be cast to javax.xml.parsers.SAXParserFactory
@PowerMockIgnore({ "javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*" })
public class VacationServiceTest
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

  @Before
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
    vacationData.setSpecial(false);
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
    vacationData.setSpecial(false);
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
    vacationData.setSpecial(false);
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
    endLastYear.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
    endLastYear.set(Calendar.MONTH, Calendar.MARCH);
    endLastYear.set(Calendar.DAY_OF_MONTH, 31);
    when(configService.getEndDateVacationFromLastYear()).thenReturn(endLastYear);

    VacationDO vacationData = new VacationDO();
    vacationData.setSpecial(false);
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
    endLastYear.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
    endLastYear.set(Calendar.MONTH, Calendar.MARCH);
    endLastYear.set(Calendar.DAY_OF_MONTH, 31);
    when(configService.getEndDateVacationFromLastYear()).thenReturn(endLastYear);

    VacationDO vacationData = new VacationDO();
    vacationData.setSpecial(false);
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
    BigDecimal numberOfDays = vacationService.getVacationDays(vacationData.getStartDate(), vacationData.getEndDate(), vacationData.getHalfDay());
    BigDecimal newValue = vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ONE.add(numberOfDays));
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearOverEndDate()
  {
    VacationDO vacationData = new VacationDO();
    vacationData.setSpecial(false);
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
    vacationData.setSpecial(true);
    BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(newValue, BigDecimal.ZERO);
  }

  @Test
  public void testUpdateUsedVacationDaysFromLastYearFirstNormalAfterIsSpecial()
  {
    endLastYear.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
    endLastYear.set(Calendar.MONTH, Calendar.MARCH);
    endLastYear.set(Calendar.DAY_OF_MONTH, 31);
    when(configService.getEndDateVacationFromLastYear()).thenReturn(endLastYear);

    final VacationDO vacationData = new VacationDO();
    final VacationDO vacationDataOld = new VacationDO();
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(20));
    when(employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(3));
    vacationData.setEmployee(employee);
    vacationDataOld.setEmployee(employee);
    vacationData.setSpecial(false);
    vacationDataOld.setSpecial(false);
    vacationData.setId(4711);
    vacationDataOld.setId(4711);
    final Calendar startDate = Calendar.getInstance();
    startDate.set(Calendar.MONTH, Calendar.MARCH);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    vacationData.setStartDate(startDate.getTime());
    vacationDataOld.setStartDate(startDate.getTime());
    final Calendar endDate = Calendar.getInstance();
    endDate.set(Calendar.MONTH, Calendar.MARCH);
    endDate.set(Calendar.DAY_OF_MONTH, 10);
    vacationData.setEndDate(endDate.getTime());
    vacationDataOld.setEndDate(endDate.getTime());
    final BigDecimal newValue = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);

    when(vacationDao.getById(4711)).thenReturn(vacationDataOld);
    final BigDecimal numberOfDays = this.vacationService.getVacationDays(vacationData.getStartDate(), vacationData.getEndDate(), vacationData.getHalfDay());
    assertEquals(newValue, new BigDecimal(3).add(numberOfDays));
    vacationData.setSpecial(true);
    final BigDecimal value = this.vacationService.updateUsedVacationDaysFromLastYear(vacationData);
    assertEquals(value, BigDecimal.ZERO);
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
    BigDecimal numberOfDays = vacationService.getVacationDays(vacation.getStartDate(), vacation.getEndDate(), vacation.getHalfDay());
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

  @Test
  public void testUpdateUsedNewVacationDaysFromLastYearUsedNone() throws ParseException
  {
    when(employee.getUrlaubstage()).thenReturn(30);

    vacationService.updateVacationDaysFromLastYearForNewYear(employee, 2016);

    // get values from mock
    ArgumentCaptor<BigDecimal> prevYearLeave = ArgumentCaptor.forClass(BigDecimal.class);
    ArgumentCaptor<BigDecimal> prevYearLeaveUsed = ArgumentCaptor.forClass(BigDecimal.class);
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName()), prevYearLeave.capture());
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName()), prevYearLeaveUsed.capture());

    Assertions.assertEquals(prevYearLeave.getValue(), BigDecimal.valueOf(30));
    Assertions.assertEquals(prevYearLeaveUsed.getValue(), BigDecimal.valueOf(0));
  }

  @Test
  public void testUpdateUsedNewVacationDaysFromLastYearUsedPartial1() throws ParseException
  {
    final List<VacationDO> vacationList = Arrays.asList(
        createVacation("2017-02-01", "2017-02-02", false, VacationStatus.IN_PROGRESS), // 2, will not be considered
        createVacation("2017-02-03", "2017-02-06", false, VacationStatus.REJECTED), // 2, will not be considered
        createVacation("2017-02-01", "2017-02-10", false, VacationStatus.APPROVED), // 8
        createVacation("2017-02-13", "2017-02-13", true, VacationStatus.APPROVED), // 0.5
        createVacation("2017-02-24", "2017-03-03", false, VacationStatus.APPROVED), // 3 in feb + 3 in mar
        createVacation("2017-03-30", "2017-04-04", false, VacationStatus.APPROVED) // 2 in mar + 2 in apr
    );

    when(employee.getUrlaubstage()).thenReturn(30);
    when(vacationDao.getVacationForPeriod(same(employee), any(Date.class), any(Date.class), eq(false))).thenReturn(vacationList);

    vacationService.updateVacationDaysFromLastYearForNewYear(employee, 2016);

    // get values from mock
    ArgumentCaptor<BigDecimal> prevYearLeave = ArgumentCaptor.forClass(BigDecimal.class);
    ArgumentCaptor<BigDecimal> prevYearLeaveUsed = ArgumentCaptor.forClass(BigDecimal.class);
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName()), prevYearLeave.capture());
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName()), prevYearLeaveUsed.capture());

    Assertions.assertEquals(prevYearLeave.getValue(), BigDecimal.valueOf(30));
    Assertions.assertEquals(prevYearLeaveUsed.getValue(), BigDecimal.valueOf(16.5));
  }

  @Test
  public void testUpdateUsedNewVacationDaysFromLastYearUsedPartial2() throws ParseException
  {
    final List<VacationDO> vacationList = Arrays.asList(
        createVacation("2017-02-01", "2017-02-02", false, VacationStatus.IN_PROGRESS), // 2, will not be considered
        createVacation("2017-02-03", "2017-02-06", false, VacationStatus.REJECTED), // 2, will not be considered
        createVacation("2017-02-01", "2017-02-10", false, VacationStatus.APPROVED), // 8
        createVacation("2017-02-13", "2017-02-13", true, VacationStatus.APPROVED), // 0.5
        createVacation("2017-02-24", "2017-03-03", false, VacationStatus.APPROVED), // 3 in feb + 3 in mar
        createVacation("2017-03-30", "2017-04-04", false, VacationStatus.APPROVED) // 2 in mar + 2 in apr
    );

    when(employee.getUrlaubstage()).thenReturn(20);
    when(vacationDao.getVacationForPeriod(same(employee), any(Date.class), any(Date.class), eq(false))).thenReturn(vacationList);

    vacationService.updateVacationDaysFromLastYearForNewYear(employee, 2016);

    // get values from mock
    ArgumentCaptor<BigDecimal> prevYearLeave = ArgumentCaptor.forClass(BigDecimal.class);
    ArgumentCaptor<BigDecimal> prevYearLeaveUsed = ArgumentCaptor.forClass(BigDecimal.class);
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName()), prevYearLeave.capture());
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName()), prevYearLeaveUsed.capture());

    Assertions.assertEquals(prevYearLeave.getValue(), BigDecimal.valueOf(20));
    Assertions.assertEquals(prevYearLeaveUsed.getValue(), BigDecimal.valueOf(16.5));
  }

  @Test
  public void testUpdateUsedNewVacationDaysFromLastYearUsedPartial3() throws ParseException
  {
    final List<VacationDO> vacationList = Arrays.asList(
        createVacation("2017-02-01", "2017-02-02", false, VacationStatus.IN_PROGRESS), // 2, will not be considered
        createVacation("2017-02-03", "2017-02-06", false, VacationStatus.REJECTED), // 2, will not be considered
        createVacation("2017-02-01", "2017-02-10", false, VacationStatus.APPROVED), // 8
        createVacation("2017-02-13", "2017-02-13", true, VacationStatus.APPROVED), // 0.5
        createVacation("2017-02-24", "2017-03-03", false, VacationStatus.APPROVED), // 3 in feb + 3 in mar
        createVacation("2017-03-30", "2017-04-04", false, VacationStatus.APPROVED) // 2 in mar + 2 in apr
    );

    when(employee.getUrlaubstage()).thenReturn(15);
    when(vacationDao.getVacationForPeriod(same(employee), any(Date.class), any(Date.class), eq(false))).thenReturn(vacationList);

    vacationService.updateVacationDaysFromLastYearForNewYear(employee, 2016);

    // get values from mock
    ArgumentCaptor<BigDecimal> prevYearLeave = ArgumentCaptor.forClass(BigDecimal.class);
    ArgumentCaptor<BigDecimal> prevYearLeaveUsed = ArgumentCaptor.forClass(BigDecimal.class);
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName()), prevYearLeave.capture());
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName()), prevYearLeaveUsed.capture());

    Assertions.assertEquals(prevYearLeave.getValue(), BigDecimal.valueOf(15));
    Assertions.assertEquals(prevYearLeaveUsed.getValue(), BigDecimal.valueOf(15));
  }

  @Test
  public void testUpdateUsedNewVacationDaysFromLastYearUsedMore() throws ParseException
  {
    final List<VacationDO> vacationList = Arrays.asList(
        createVacation("2017-02-01", "2017-02-02", false, VacationStatus.IN_PROGRESS), // 2, will not be considered
        createVacation("2017-02-03", "2017-02-06", false, VacationStatus.REJECTED), // 2, will not be considered
        createVacation("2017-02-01", "2017-02-25", false, VacationStatus.APPROVED), // 18
        createVacation("2017-03-01", "2017-04-30", false, VacationStatus.APPROVED) //
    );

    when(employee.getUrlaubstage()).thenReturn(30);
    when(vacationDao.getVacationForPeriod(same(employee), any(Date.class), any(Date.class), eq(false))).thenReturn(vacationList);

    vacationService.updateVacationDaysFromLastYearForNewYear(employee, 2016);

    // get values from mock
    ArgumentCaptor<BigDecimal> prevYearLeave = ArgumentCaptor.forClass(BigDecimal.class);
    ArgumentCaptor<BigDecimal> prevYearLeaveUsed = ArgumentCaptor.forClass(BigDecimal.class);
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName()), prevYearLeave.capture());
    Mockito.verify(employee).putAttribute(same(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName()), prevYearLeaveUsed.capture());

    Assertions.assertEquals(prevYearLeave.getValue(), BigDecimal.valueOf(30));
    Assertions.assertEquals(prevYearLeaveUsed.getValue(), BigDecimal.valueOf(30));
  }

}
