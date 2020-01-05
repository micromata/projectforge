/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
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
@PowerMockIgnore({"javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*"})
public class VacationServiceTest {
  private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final LocalDate now = LocalDate.now();

  private LocalDate endLastYear = now;

  @InjectMocks
  private VacationService vacationService = new VacationService();

  @Mock
  private ConfigurationService configService;

  @Mock
  private EmployeeDao employeeDao;

  @Mock
  private VacationDao vacationDao;

  @Mock
  private EmployeeDO employee;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    Locale locale = Locale.getDefault();
    TimeZone timeZone = TimeZone.getDefault();
    ConfigXml.createForJunitTests();
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
    endLastYear = LocalDate.of(2017, Month.MARCH, 31);
    when(configService.getEndDateVacationFromLastYear()).thenReturn(endLastYear);
  }

  @Test
  public void testGetVacationDays() {
    final LocalDate workingDate1 = LocalDate.of(2017, Month.FEBRUARY, 1);
    final LocalDate workingDate2 = LocalDate.of(2017, Month.FEBRUARY, 2);

    assertEquals(vacationService.getVacationDays(workingDate1, workingDate1, false), new BigDecimal(1));
    assertEquals(vacationService.getVacationDays(workingDate1, workingDate1, true), new BigDecimal(0.5));
    assertEquals(vacationService.getVacationDays(workingDate1, workingDate2, false), new BigDecimal(2));
    assertEquals(vacationService.getVacationDays(workingDate1, workingDate2, true), new BigDecimal(0.5));
  }

  @Test
  public void testGetAvailableVacationDaysForYearAtDate() throws ParseException {
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

    final ArgumentCaptor<LocalDate> startDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    when(vacationDao.getVacationForPeriod(same(employee), startDateCaptor.capture(), any(LocalDate.class), eq(false))).thenReturn(vacationList);
    final LocalDate expectedStartDate = LocalDate.of(2017, Month.JANUARY, 1);

    // Test 1
    final BigDecimal availableVacationdays = vacationService.getAvailableVacationDaysForYearAtDate(employee, LocalDate.of(2017, Month.FEBRUARY, 28));
    assertEquals(availableVacationdays, new BigDecimal(yearlyVacation).add(prevYear).subtract(new BigDecimal(11.5)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 1);

    // Test 2
    final BigDecimal availableVacationdays2 = vacationService.getAvailableVacationDaysForYearAtDate(employee, LocalDate.of(2017, Month.MARCH, 31));
    assertEquals(availableVacationdays2, new BigDecimal(yearlyVacation).add(prevYear).subtract(new BigDecimal(16.5)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 2);

    // Test 3
    final BigDecimal availableVacationdays3 = vacationService.getAvailableVacationDaysForYearAtDate(employee, LocalDate.of(2017, Month.APRIL, 30));
    assertEquals(availableVacationdays3, new BigDecimal(yearlyVacation).add(prevYearUsed).subtract(new BigDecimal(18.5)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 3);
  }

  @Test
  public void testGetAvailableVacationDaysForYearAtDatePrevYear() throws ParseException {
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

    final ArgumentCaptor<LocalDate> startDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    when(vacationDao.getVacationForPeriod(same(employee), startDateCaptor.capture(), any(LocalDate.class), eq(false))).thenReturn(vacationList);
    final LocalDate expectedStartDate = LocalDate.of(2016, Month.JANUARY, 1);

    // here we verify that the previous year leave att values are ignored because they belong to 2017 (see this.endLastYear) and we query values from 2016
    // Test 1
    final BigDecimal availableVacationdays = vacationService.getAvailableVacationDaysForYearAtDate(employee, LocalDate.of(2016, Month.FEBRUARY, 29));
    assertEquals(availableVacationdays, new BigDecimal(yearlyVacation).subtract(new BigDecimal(4)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 1);

    // Test 2
    final BigDecimal availableVacationdays2 = vacationService.getAvailableVacationDaysForYearAtDate(employee, LocalDate.of(2016, Month.APRIL, 30));
    assertEquals(availableVacationdays2, new BigDecimal(yearlyVacation).subtract(new BigDecimal(11)));
    assertEquals(startDateCaptor.getValue(), expectedStartDate);
    assertEquals(startDateCaptor.getAllValues().size(), 2);
  }

  private VacationDO createVacation(final String from, final String to, final Boolean halfDay, final VacationStatus status) throws ParseException {
    final VacationDO vacation = new VacationDO();
    vacation.setStartDate(LocalDate.parse(from, dtf));
    vacation.setEndDate(LocalDate.parse(to, dtf));
    vacation.setHalfDay(halfDay);
    vacation.setStatus(status);
    return vacation;
  }
}
